package com.maseffectsplus.render;

import com.maseffectsplus.config.EffectConfig;
import com.maseffectsplus.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EffectManager {
    private static final List<ActiveEffect> ACTIVE_EFFECTS = new ArrayList<>();

    public static Vec3 getEntityPos(Entity entity) {
        if (entity == null) return Vec3.ZERO;
        return new Vec3(entity.getX(), entity.getY(), entity.getZ());
    }

    public static Vec3 getEntityEyePos(Entity entity) {
        if (entity == null) return Vec3.ZERO;
        return new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
    }

    public static synchronized void spawnEffect(EffectConfig config, Entity target) {
        try {
            if (config == null || !config.enabled) return;

            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            ModConfig modConfig = ModConfig.get();
            if (target != null) {
                if (target == client.player && !modConfig.onYourself) return;
                if (target instanceof Player && target != client.player && !modConfig.onPlayers) return;
                if (target instanceof Mob && !modConfig.onMobs) return;

                double distSq = client.player.distanceToSqr(target);
                if (distSq > modConfig.range * modConfig.range) return;
            }

            Vec3 pos = target != null ? getEntityPos(target).add(0, config.heightOffset, 0) : getEntityPos(client.player);
            spawnEffectAt(config, target, pos);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static synchronized void spawnEffectAt(EffectConfig config, Entity target, Vec3 pos) {
        try {
            ModConfig modConfig = ModConfig.get();
            if (ACTIVE_EFFECTS.size() >= modConfig.maxEffects) {
                ACTIVE_EFFECTS.remove(0); // Evict oldest
            }
            ACTIVE_EFFECTS.add(new ActiveEffect(config, target, pos));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static synchronized void spawnPreviewEffect(EffectConfig config) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            // Position 3 blocks directly in front of the player's eyes
            Vec3 look = client.player.getViewVector(1.0f);
            Vec3 pos = getEntityEyePos(client.player).add(look.scale(3.0)).subtract(0, 0.5, 0);

            spawnEffectAt(config, null, pos);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static synchronized void tick() {
        try {
            Iterator<ActiveEffect> it = ACTIVE_EFFECTS.iterator();
            while (it.hasNext()) {
                ActiveEffect effect = it.next();
                effect.tick();
                if (effect.isExpired()) {
                    it.remove();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static synchronized List<ActiveEffect> getActiveEffects() {
        return new ArrayList<>(ACTIVE_EFFECTS);
    }

    public static synchronized int activeCount() {
        return ACTIVE_EFFECTS.size();
    }

    public static synchronized void clear() {
        ACTIVE_EFFECTS.clear();
    }
}
