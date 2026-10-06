package com.maseffectsplus.combat;

import com.maseffectsplus.config.ModConfig;
import com.maseffectsplus.render.EffectManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CombatTracker {
    // Maps entityId -> timestamp when shield was disabled
    private static final Map<Integer, Long> SHIELD_DISABLED_ENTITIES = new ConcurrentHashMap<>();
    private static final long STUN_WINDOW_MS = 700L; // 0.7s window after the shield break to execute the stun slam

    // After a stunslam the target's shield stays disabled for about 5 seconds (like in the game): no new shield
    // break, and so no new Black Flash, can happen in that time. This stops axe spam from triggering one every 2nd hit.
    private static final Map<Integer, Long> STUNSLAM_LOCK = new ConcurrentHashMap<>();
    private static final long STUNSLAM_LOCK_MS = 5000L;
    // Right after a predicted shield break by your axe, the next axe swing must not count as another break.
    private static final Map<Integer, Long> PREDICT_LOCK = new ConcurrentHashMap<>();
    private static final long PREDICT_LOCK_MS = 1200L;

    private static boolean locked(Map<Integer, Long> map, int entityId) {
        Long until = map.get(entityId);
        return until != null && System.currentTimeMillis() < until;
    }

    // When you last hit each entity: a "shield disabled" message only counts if it follows a hit of yours
    private static final Map<Integer, Long> LAST_ATTACK = new ConcurrentHashMap<>();
    private static final long OWN_HIT_MATCH_MS = 1500L;
    // When you last hit each entity with an axe: only an axe breaks a shield, so only a "shield disabled" message
    // right after one of your own axe hits is your break (not a teammate's, not one from a fight next to you)
    private static final Map<Integer, Long> LAST_AXE_ATTACK = new ConcurrentHashMap<>();
    private static long lastPrune = 0L;

    // A break we only guessed from an axe hit (SHIELD_PREDICTED) has to be confirmed by the server (SHIELD_CONFIRMED),
    // otherwise it didn't happen (axe not charged, hit from the side or behind, shield was down...).
    private static final Map<Integer, Long> SHIELD_PREDICTED = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> SHIELD_CONFIRMED = new ConcurrentHashMap<>();
    private static final long CONFIRM_MARGIN_MS = 250L; // on top of your ping

    private static long latencyMs(Minecraft client) {
        try {
            if (client.getConnection() != null && client.player != null) {
                net.minecraft.client.multiplayer.PlayerInfo entry =
                        client.getConnection().getPlayerInfo(client.player.getUUID());
                if (entry != null) return Math.max(0, entry.getLatency());
            }
        } catch (Throwable ignored) {
        }
        return 100L;
    }

    /** True if we guessed a shield break and the server didn't confirm it in time. */
    private static boolean predictionExpired(Minecraft client, int id, long now) {
        Long pred = SHIELD_PREDICTED.get(id);
        if (pred == null) return false;
        Long conf = SHIELD_CONFIRMED.get(id);
        if (conf != null && conf >= pred) return false;
        return now - pred > latencyMs(client) + CONFIRM_MARGIN_MS;
    }

    private static void cancelPrediction(int id) {
        SHIELD_PREDICTED.remove(id);
        SHIELD_DISABLED_ENTITIES.remove(id);
        ComboTracker.cancelAttempt(); // the shield never broke, so this isn't a failed stunslam
    }

    /** Called every client tick: drops guessed shield breaks the server did not confirm. */
    public static void tick() {
        if (SHIELD_PREDICTED.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        for (Integer id : SHIELD_PREDICTED.keySet()) {
            Long pred = SHIELD_PREDICTED.get(id);
            if (pred == null) continue;
            // Newer versions don't send a "shield disabled" message any more. A broken shield shows up as the target
            // no longer blocking shortly after your axe hit (it can't raise it again for a few seconds).
            Long conf = SHIELD_CONFIRMED.get(id);
            boolean confirmed = conf != null && conf >= pred;
            Entity entity = client.level == null ? null : client.level.getEntity(id);
            if (!confirmed && entity instanceof LivingEntity living && !living.isBlocking() && now - pred >= 30L) {
                SHIELD_CONFIRMED.put(id, now);
                continue;
            }
            if (predictionExpired(client, id, now)) cancelPrediction(id);
        }
    }

    /** Drops old entries now and then so the maps don't grow during a long session. */
    private static void pruneOld(long now) {
        if (now - lastPrune < 30_000L) return;
        lastPrune = now;
        SHIELD_PREDICTED.values().removeIf(t -> now - t > 10_000L);
        SHIELD_CONFIRMED.values().removeIf(t -> now - t > 10_000L);
        SHIELD_DISABLED_ENTITIES.values().removeIf(t -> now - t > 10_000L);
        LAST_ATTACK.values().removeIf(t -> now - t > 10_000L);
        LAST_AXE_ATTACK.values().removeIf(t -> now - t > 10_000L);
        LAST_POP_BY_ENTITY.values().removeIf(t -> now - t > 10_000L);
        STUNSLAM_LOCK.values().removeIf(until -> now > until);
        PREDICT_LOCK.values().removeIf(until -> now > until);
    }

    /** The server told us a shield was disabled (entity event 30). */
    public static void onShieldDisabled(int entityId) {
        long now = System.currentTimeMillis();
        // Only a break that follows an axe hit of yours is your stunslam (a teammate breaking the shield is not)
        Long hit = LAST_AXE_ATTACK.get(entityId);
        if (hit == null || now - hit > Math.min(OWN_HIT_MATCH_MS, latencyMs(Minecraft.getInstance()) + 400L)) return;
        // A "shield disabled" message arriving shortly after a stunslam is the same break that was already used
        // (it comes with your ping). Later ones are real new breaks.
        Long until = STUNSLAM_LOCK.get(entityId);
        if (until != null && now < until - (STUNSLAM_LOCK_MS - 1500L)) return;
        SHIELD_DISABLED_ENTITIES.put(entityId, now);
        SHIELD_CONFIRMED.put(entityId, now);
    }

    public static void onAttackEntity(Minecraft client, Entity target) {
        try {
            if (client.player == null || !(target instanceof LivingEntity livingTarget)) return;

            ModConfig config = ModConfig.get();
            long now = System.currentTimeMillis();
            LAST_ATTACK.put(target.getId(), now);
            if (client.player.getMainHandItem().getItem() instanceof AxeItem) {
                LAST_AXE_ATTACK.put(target.getId(), now);
            }
            pruneOld(now);

            // Was the shield already broken a moment ago? (checked before this hit, so the breaking hit itself
            // never counts as its own follow-up)
            Long disabledTime = SHIELD_DISABLED_ENTITIES.get(target.getId());
            boolean shieldRecentlyBroken = disabledTime != null && (now - disabledTime) <= STUN_WINDOW_MS;

            // A break we only guessed from an axe hit that the server never confirmed didn't happen
            boolean unconfirmed = false;
            if (shieldRecentlyBroken && predictionExpired(client, target.getId(), now)) {
                cancelPrediction(target.getId());
                shieldRecentlyBroken = false;
                unconfirmed = true;
            }

            // An axe hit on a shielded target that isn't broken yet breaks the shield: remember it immediately.
            // If the shield was just broken, an axe is a normal follow-up weapon like any other.
            boolean fromSide = false;
            boolean recordedBreak = false;
            if (!shieldRecentlyBroken && !locked(PREDICT_LOCK, target.getId())
                    && client.player.getMainHandItem().getItem() instanceof AxeItem) {
                // Only a raised shield can be broken by an axe (a shield that is merely held is not disabled), and a
                // raised shield is active again, so this counts as a new break even shortly after a stunslam.
                // Like in the game, it only breaks when you hit the shield from the front (a charged axe is not needed).
                boolean blocking = livingTarget.isBlocking();
                double yaw = Math.toRadians(livingTarget.getYHeadRot());
                double dx = client.player.getX() - target.getX();
                double dz = client.player.getZ() - target.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                fromSide = blocking && len > 1.0e-4 && ((-Math.sin(yaw)) * dx + Math.cos(yaw) * dz) / len <= 0.0;
                boolean hasShield = blocking && !fromSide;
                if (hasShield) {
                    recordedBreak = true;
                    SHIELD_PREDICTED.put(target.getId(), now);
                    SHIELD_CONFIRMED.remove(target.getId());
                    SHIELD_DISABLED_ENTITIES.put(target.getId(), now);
                    PREDICT_LOCK.put(target.getId(), now + PREDICT_LOCK_MS);
                    // From now on the stunslam has to follow in time, otherwise the combo is lost
                    ComboTracker.onShieldBrokenByYou();
                }
            }

            boolean isStunslam = false;
            if (shieldRecentlyBroken) {
                if (config.stunslamAnyWeapon) {
                    // Any weapon mode: the hit right after the shield break counts, whatever you hold (axe included)
                    isStunslam = true;
                    SHIELD_DISABLED_ENTITIES.remove(target.getId());
                } else {
                    // Mace mode: only a real smash counts, holding the mace AND falling (> 1.5 blocks of fall distance)
                    // and not gliding: with an elytra you glide to the target, put the chestplate back on and then hit). A plain hit on the ground never triggers a Black Flash.
                    boolean smash = client.player.fallDistance > 1.5f && !client.player.isFallFlying();
                    if (client.player.getMainHandItem().getItem() == Items.MACE && smash) {
                        isStunslam = true;
                        SHIELD_DISABLED_ENTITIES.remove(target.getId());
                    }
                }
            }

            if (config.debug) {
                String why;
                if (isStunslam) {
                    why = "FLASH";
                } else if (shieldRecentlyBroken) {
                    why = config.stunslamAnyWeapon ? "?" : "follow-up not valid: need mace + smash, not while gliding (fall "
                            + String.format(java.util.Locale.ROOT, "%.1f", client.player.fallDistance) + ", need >1.5)";
                } else if (unconfirmed) {
                    why = "the server did not confirm a shield break, no stunslam";
                } else if (recordedBreak) {
                    why = "shield break recorded, now hit within 0.7s";
                } else if (fromSide) {
                    why = "hit from the side or behind, the shield doesn't block it";
                } else if (locked(STUNSLAM_LOCK, target.getId())) {
                    why = "flash <5s ago and the shield isn't raised again yet";
                } else if (locked(PREDICT_LOCK, target.getId())) {
                    why = "axe swing right after a break, ignored";
                } else if (client.player.getMainHandItem().getItem() instanceof AxeItem) {
                    why = "axe hit but the target's shield isn't raised";
                } else {
                    why = "no shield break recorded (0.7s window over or no axe hit)";
                }
                client.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("StunFX: " + why));
            }

            // The follow-up after your shield break wasn't a valid stunslam (e.g. a mace hit without a smash): combo lost
            if (shieldRecentlyBroken && !isStunslam && ComboTracker.hasAttempt()) {
                ComboTracker.fail();
            }

            if (!config.effectsEnabled) return;

            if (isStunslam && config.stunslam.enabled) {
                // Trigger Jujutsu Kaisen Black Flash Stunslam!
                STUNSLAM_LOCK.put(target.getId(), now + STUNSLAM_LOCK_MS);
                ComboTracker.onStunslam();
                EffectManager.spawnEffect(config.stunslam, target);
                if (config.ringWithFlash && config.bigDamage.enabled) {
                    EffectManager.spawnEffect(config.bigDamage, target);
                }
                if (config.stunslam.soundEnabled) {
                    playCustomSound(Identifier.fromNamespaceAndPath("maseffectsplus", "stunslam.black_flash"), config.stunslam.volume * 2.0f, config.stunslam.pitch);
                }
            } else if (client.player.getMainHandItem().getItem() == Items.MACE && config.bigDamage.enabled
                    && (client.player.fallDistance > 1.5f && !client.player.isFallFlying())) {
                // Only a real mace smash (falling, not gliding) spawns the ring; a plain hit on the ground does not.
                // Visual only, the Black Flash sound is reserved for a real stunslam
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

    /** Minecraft's own master volume (0..1), so this sound follows the game's volume slider like every other sound. */
    private static float gameVolume() {
        try {
            return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
        } catch (Throwable t) {
            return 1.0f;
        }
    }

    private static synchronized boolean playPreloaded(float volume) {
        if (volume <= 0.001f) return true; // game volume is 0: stay silent
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
        // 1. Instant playback from a pre-opened clip (no loading, no thread spawn). The clip can't change pitch,
        //    so a different pitch (e.g. the deeper finisher sound) goes through Minecraft's sound engine below.
        if (Math.abs(pitch - 1.0f) < 0.16f && playPreloaded(volume * gameVolume())) return;

        // 2. Fallback: Minecraft SoundManager (only if the preloaded clip is unavailable)
        try {
            Minecraft client = Minecraft.getInstance();
            SoundEvent sound = SoundEvent.createVariableRangeEvent(id);

            // Find method on SimpleSoundInstance that creates an instance:
            SimpleSoundInstance instance = null;
            for (java.lang.reflect.Method m : SimpleSoundInstance.class.getMethods()) {
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && SimpleSoundInstance.class.isAssignableFrom(m.getReturnType())) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 3 && params[0].isAssignableFrom(SoundEvent.class) && params[1] == float.class && params[2] == float.class) {
                        m.setAccessible(true);
                        instance = (SimpleSoundInstance) m.invoke(null, sound, pitch, volume);
                        break;
                    }
                }
            }
            if (instance == null) {
                for (java.lang.reflect.Method m : SimpleSoundInstance.class.getMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && SimpleSoundInstance.class.isAssignableFrom(m.getReturnType())) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 2 && params[0].isAssignableFrom(SoundEvent.class) && params[1] == float.class) {
                            m.setAccessible(true);
                            instance = (SimpleSoundInstance) m.invoke(null, sound, pitch);
                            break;
                        }
                    }
                }
            }
            if (instance != null) {
                client.getSoundManager().play(instance);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static final Map<Integer, Long> LAST_POP_BY_ENTITY = new ConcurrentHashMap<>();

    // The effects are for your own fights: only entities you hit a moment ago get a totem pop or kill effect.
    // (The totem pop counter still counts everybody in range.)
    private static final long OWN_TARGET_MS = 6000L;

    private static boolean hitByYouRecently(Entity entity) {
        Long t = LAST_ATTACK.get(entity.getId());
        return t != null && System.currentTimeMillis() - t <= OWN_TARGET_MS;
    }
    public static void onTotemPop(Entity entity) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            // Pops of every player within the totem range count (not only your own fights)
            double distSq = client.player.distanceToSqr(entity);
            double totemRange = ModConfig.get().totemRange;
            if (distSq > totemRange * totemRange) return;

            // Safety net: one totem can't pop twice within 0.3s (damage immunity is 0.5s), so ignore duplicates
            long nowMs = System.currentTimeMillis();
            Long lastPop = LAST_POP_BY_ENTITY.put(entity.getId(), nowMs);
            if (lastPop != null && nowMs - lastPop < 300L) return;

            ModConfig config = ModConfig.get();
            if (entity instanceof Player player) {
                int pops = PopCounterManager.recordPop(player.getName().getString());
                // Optional counter "ding": rises in pitch with every pop of the same player
                if (config.popCounterEnabled && config.popCounterSound && pops > 0) {
                    float pitch = 0.8f + 0.12f * Math.min(pops - 1, 6);
                    client.getSoundManager().play(SimpleSoundInstance.forUI(
                            SoundEvents.EXPERIENCE_ORB_PICKUP, pitch, config.popCounterSoundVolume));
                }
            }

            if (config.effectsEnabled && config.totemPop.enabled && hitByYouRecently(entity)) {
                // Visual only: Minecraft already plays the totem sound itself
                EffectManager.spawnEffect(config.totemPop, entity, config.totemRange);
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

            if (entity == client.player) {
                ComboTracker.reset(); // your own death ends the combo
            }
            if (entity instanceof Player player) {
                PopCounterManager.resetPlayer(player.getName().getString());
                // Player died nearby: play the death sound (a chat death message for the same death is deduplicated)
                DeathSoundHandler.play(false);
            }

            // Mob deaths only count when "effects on mobs" is on (PvP focus: players always count)
            boolean countsAsKill = entity instanceof Player || config.onMobs;
            if (countsAsKill && config.effectsEnabled && config.kill.enabled && hitByYouRecently(entity)) {
                // Visual only: no extra sound (the optional death sound is a separate setting)
                EffectManager.spawnEffect(config.kill, entity);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
