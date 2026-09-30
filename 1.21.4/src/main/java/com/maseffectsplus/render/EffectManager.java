

package com.maseffectsplus.render;

import com.maseffectsplus.config.EffectConfig;
import com.maseffectsplus.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EffectManager {
    private static final List<ActiveEffect> ACTIVE_EFFECTS = new ArrayList<>();

    public static Vec3d getEntityPos(Entity entity) {
        if (entity == null) return Vec3d.ZERO;
        return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
    }

    public static Vec3d getEntityEyePos(Entity entity) {
        if (entity == null) return Vec3d.ZERO;
        return new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
    }

    public static synchronized void spawnEffect(EffectConfig config, Entity target) {
        try {
            if (config == null || !config.enabled) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            ModConfig modConfig = ModConfig.get();
            if (target != null) {
                if (target == client.player && !modConfig.onYourself) return;
                if (target instanceof PlayerEntity && target != client.player && !modConfig.onPlayers) return;
                if (target instanceof MobEntity && !modConfig.onMobs) return;

                double distSq = client.player.squaredDistanceTo(target);
                if (distSq > modConfig.range * modConfig.range) return;
            }

            Vec3d pos = target != null ? getEntityPos(target).add(0, config.heightOffset, 0) : getEntityPos(client.player);
            spawnEffectAt(config, target, pos);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static synchronized void spawnEffectAt(EffectConfig config, Entity target, Vec3d pos) {
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
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            // Position 3 blocks directly in front of the player's eyes
            Vec3d look = client.player.getRotationVec(1.0f);
            Vec3d pos = getEntityEyePos(client.player).add(look.multiply(3.0)).subtract(0, 0.5, 0);

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

    public static synchronized void clear() {
        ACTIVE_EFFECTS.clear();
    }
}

