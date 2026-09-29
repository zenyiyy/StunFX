package com.maseffectsplus.render;

import com.maseffectsplus.config.EffectConfig;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class ActiveEffect {
    public final EffectConfig config;
    public final Entity target;
    public final Vec3d spawnPos;
    public int age = 0;
    public final int maxAge;

    public ActiveEffect(EffectConfig config, Entity target, Vec3d spawnPos) {
        this.config = config;
        this.target = target;
        this.spawnPos = spawnPos;
        this.maxAge = Math.max(1, config.durationTicks);
    }

    public boolean isExpired() {
        return age >= maxAge;
    }

    public void tick() {
        age++;
    }

    public Vec3d getPosition(float tickDelta) {
        boolean follow = config.followTarget || config.style == com.maseffectsplus.config.EffectStyle.BLACK_FLASH;
        if (follow && target != null && target.isAlive()) {
            try {
                return target.getLerpedPos(tickDelta).add(0, config.heightOffset, 0);
            } catch (Throwable t) {
                return new Vec3d(target.getX(), target.getY() + config.heightOffset, target.getZ());
            }
        }
        return spawnPos;
    }

    public float getProgress(float tickDelta) {
        return Math.min(1.0f, (age + tickDelta) / (float) maxAge);
    }

    public float getAlpha(float tickDelta) {
        float progress = getProgress(tickDelta);
        // Black Flash must hit instantly, no fade-in
        int fadeInTicks = config.style == com.maseffectsplus.config.EffectStyle.BLACK_FLASH ? 0 : config.fadeInTicks;
        float fadeIn = (float) fadeInTicks / maxAge;
        float fadeOut = (float) config.fadeOutTicks / maxAge;

        float alpha = config.alpha;
        if (progress < fadeIn && fadeIn > 0) {
            alpha *= (progress / fadeIn);
        } else if (progress > (1.0f - fadeOut) && fadeOut > 0) {
            alpha *= ((1.0f - progress) / fadeOut);
        }
        return Math.max(0.0f, Math.min(1.0f, alpha));
    }
}
