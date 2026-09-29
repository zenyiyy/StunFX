package com.maseffectsplus.combat;

import com.maseffectsplus.config.ModConfig;
import com.maseffectsplus.render.EffectManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CombatTracker {
    // Maps entityId -> timestamp when shield was disabled
    private static final Map<Integer, Long> SHIELD_DISABLED_ENTITIES = new ConcurrentHashMap<>();
    private static final long STUN_WINDOW_MS = 700L; // 0.7s window after the shield break to execute the stun slam

    public static void onShieldDisabled(int entityId) {
        SHIELD_DISABLED_ENTITIES.put(entityId, System.currentTimeMillis());
    }

    public static void onAttackEntity(Minecraft client, Entity target) {
        try {
            if (client.player == null || !(target instanceof LivingEntity livingTarget)) return;

            var held = client.player.getMainHandItem().getItem();

            // If player hits with an Axe while target is blocking or holding a shield, record shield disabled immediately
            boolean breakingHit = false;
            if (held instanceof AxeItem) {
                boolean hasShield = livingTarget.isBlocking()
                        || livingTarget.getOffhandItem().getItem() == Items.SHIELD
                        || livingTarget.getMainHandItem().getItem() == Items.SHIELD;
                if (hasShield) {
                    onShieldDisabled(target.getId());
                    breakingHit = true;
                }
            }

            ModConfig config = ModConfig.get();
            long now = System.currentTimeMillis();
            Long disabledTime = SHIELD_DISABLED_ENTITIES.get(target.getId());

            boolean isStunslam = false;
            if (disabledTime != null && (now - disabledTime) <= STUN_WINDOW_MS) {
                if (config.stunslamAnyWeapon) {
                    // Any weapon mode: the hit right after the shield break counts (not the breaking hit itself)
                    if (!breakingHit) {
                        isStunslam = true;
                        SHIELD_DISABLED_ENTITIES.remove(target.getId());
                    }
                } else {
                    // Mace mode: only a real smash counts: holding the mace AND falling (> 1.5 blocks of fall distance)
                    // or flying with an elytra (elytra mace). A plain hit on the ground never triggers a Black Flash.
                    boolean smash = client.player.fallDistance > 1.5 || client.player.isFallFlying();
                    if (held == Items.MACE && smash) {
                        isStunslam = true;
                        SHIELD_DISABLED_ENTITIES.remove(target.getId());
                    }
                }
            }

            if (!config.effectsEnabled) return;

            if (isStunslam && config.stunslam.enabled) {
                // Trigger Jujutsu Kaisen Black Flash Stunslam!
                EffectManager.spawnEffect(config.stunslam, target);
                if (config.stunslam.soundEnabled) {
                    playCustomSound(Identifier.fromNamespaceAndPath("maseffectsplus", "stunslam.black_flash"),
                            config.stunslam.volume * 2.0f, config.stunslam.pitch);
                }
            } else if (held == Items.MACE && config.bigDamage.enabled) {
                // Plain mace hit: visual only, the Black Flash sound is reserved for a real stunslam
                EffectManager.spawnEffect(config.bigDamage, target);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static final int CLIP_POOL_SIZE = 3;
    private static final javax.sound.sampled.Clip[] CLIPS = new javax.sound.sampled.Clip[CLIP_POOL_SIZE];
    private static int nextClip = 0;
    private static boolean preloaded = false;

    /** Opens the clips at game start so the first stunslam plays without any delay. */
    public static synchronized void preloadSound() {
        if (preloaded) return;
        preloaded = true;
        try {
            java.io.InputStream is = CombatTracker.class.getResourceAsStream("/assets/maseffectsplus/sounds/black_flash.wav");
            if (is == null) return;
            byte[] data = is.readAllBytes();
            for (int i = 0; i < CLIP_POOL_SIZE; i++) {
                javax.sound.sampled.AudioInputStream ais = javax.sound.sampled.AudioSystem.getAudioInputStream(
                        new java.io.ByteArrayInputStream(data));
                javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
                clip.open(ais);
                CLIPS[i] = clip;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static synchronized boolean playPreloaded(float volume) {
        preloadSound();
        javax.sound.sampled.Clip clip = CLIPS[nextClip];
        nextClip = (nextClip + 1) % CLIP_POOL_SIZE;
        if (clip == null) return false;
        try {
            clip.stop();
            clip.setFramePosition(0);
            try {
                javax.sound.sampled.FloatControl gain = (javax.sound.sampled.FloatControl) clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log10(Math.max(0.01, volume)) * 20.0);
                gain.setValue(Math.min(gain.getMaximum(), Math.max(gain.getMinimum(), dB)));
            } catch (Throwable ignored) {}
            clip.start();
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static void playCustomSound(Identifier id, float volume, float pitch) {
        // 1. Instant playback from a pre-opened clip (no loading, no thread spawn)
        if (playPreloaded(volume)) return;

        // 2. Fallback: Minecraft SoundManager (only if the preloaded clip is unavailable)
        try {
            Minecraft client = Minecraft.getInstance();
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(id), pitch, volume));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static final Map<Integer, Long> LAST_POP_BY_ENTITY = new ConcurrentHashMap<>();

    public static void onTotemPop(Entity entity) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            // Count pops within 40 block radius as requested
            double distSq = client.player.distanceToSqr(entity);
            if (distSq > 40.0 * 40.0) return;

            // Safety net: one totem can't pop twice within 0.3s (damage immunity is 0.5s), so ignore duplicates
            long nowMs = System.currentTimeMillis();
            Long lastPop = LAST_POP_BY_ENTITY.put(entity.getId(), nowMs);
            if (lastPop != null && nowMs - lastPop < 300L) return;

            ModConfig config = ModConfig.get();
            if (entity instanceof Player player) {
                PopCounterManager.recordPop(player.getName().getString());
            }

            if (config.effectsEnabled && config.totemPop.enabled) {
                EffectManager.spawnEffect(config.totemPop, entity);

                if (config.totemPop.soundEnabled && client.level != null) {
                    client.level.playLocalSound(
                        entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.TOTEM_USE,
                        SoundSource.PLAYERS,
                        config.totemPop.volume,
                        config.totemPop.pitch,
                        false
                    );
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void onEntityDeath(Entity entity) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            double distSq = client.player.distanceToSqr(entity);
            ModConfig config = ModConfig.get();
            if (distSq > config.range * config.range) return;

            if (entity instanceof Player player) {
                PopCounterManager.resetPlayer(player.getName().getString());
            }

            if (config.effectsEnabled && config.kill.enabled) {
                EffectManager.spawnEffect(config.kill, entity);

                // Play configurable kill / death sound (can be toggled in GUI)
                if (config.kill.soundEnabled && client.level != null) {
                    client.level.playLocalSound(
                        entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS,
                        config.kill.volume * 1.5f,
                        config.kill.pitch * 0.7f,
                        false
                    );
                    client.level.playLocalSound(
                        entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.ANVIL_LAND,
                        SoundSource.PLAYERS,
                        config.kill.volume * 0.8f,
                        config.kill.pitch * 1.4f,
                        false
                    );
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void onDamageTaken(Entity entity) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (entity == client.player) {
                ModConfig config = ModConfig.get();
                if (config.effectsEnabled && config.damageTaken.enabled) {
                    EffectManager.spawnEffect(config.damageTaken, entity);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
