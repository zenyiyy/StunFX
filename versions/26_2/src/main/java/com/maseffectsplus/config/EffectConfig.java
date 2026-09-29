package com.maseffectsplus.config;

public class EffectConfig {
    public boolean enabled = true;

    // --- Shape Tab ---
    public EffectStyle style = EffectStyle.SHOCKWAVE;
    public float startRadius = 0.40f;
    public float endRadius = 3.40f;
    public float thickness = 0.18f;
    public float height = 2.60f;
    public float heightOffset = 0.05f;
    public int corners = 48;
    public int rings = 3;
    public float spin = 40.0f; // degrees/sec
    public boolean followTarget = true;
    public float scale = 1.0f; // overall size multiplier (Black Flash: 1.0 = default size)

    // --- Colour Tab ---
    public float red = 1.0f;
    public float green = 0.85f;
    public float blue = 0.2f;
    public float alpha = 0.9f;

    // Secondary color (used for outer glow / lightning tips)
    public float secondaryRed = 1.0f;
    public float secondaryGreen = 0.2f;
    public float secondaryBlue = 0.2f;
    public boolean rainbow = false;

    // --- Timing Tab ---
    public int durationTicks = 20; // 1 second
    public int fadeInTicks = 2;
    public int fadeOutTicks = 8;

    // --- Sound Tab ---
    public boolean soundEnabled = true;
    public String soundType = "DEFAULT";
    public float volume = 1.0f;
    public float pitch = 1.0f;

    public static EffectConfig createDefaultStunslam() {
        EffectConfig c = new EffectConfig();
        c.enabled = true;
        c.style = EffectStyle.BLACK_FLASH;
        c.startRadius = 0.5f;
        c.endRadius = 4.2f;
        c.thickness = 0.35f;
        c.height = 3.0f;
        c.heightOffset = 0.5f;
        c.corners = 48;
        c.rings = 5;
        c.spin = 60.0f;
        c.followTarget = true;

        // JJK Black Flash Colors: Pitch Black Core & Crimson Red Energy
        c.red = 0.05f;
        c.green = 0.0f;
        c.blue = 0.02f;
        c.alpha = 0.95f;

        c.secondaryRed = 0.95f;
        c.secondaryGreen = 0.05f;
        c.secondaryBlue = 0.15f;

        c.durationTicks = 25;
        c.soundEnabled = true;
        c.soundType = "BLACK_FLASH";
        c.volume = 1.2f;
        c.pitch = 0.85f;
        return c;
    }

    public static EffectConfig createDefaultTotemPop() {
        EffectConfig c = new EffectConfig();
        c.enabled = true;
        c.style = EffectStyle.SHOCKWAVE;
        c.startRadius = 0.40f;
        c.endRadius = 3.40f;
        c.thickness = 0.18f;
        c.height = 2.60f;
        c.heightOffset = 0.05f;
        c.corners = 48;
        c.rings = 3;
        c.spin = 40.0f;
        c.followTarget = true;

        // Golden Totem Colors
        c.red = 1.0f;
        c.green = 0.85f;
        c.blue = 0.1f;
        c.alpha = 0.85f;
        c.secondaryRed = 0.2f;
        c.secondaryGreen = 1.0f;
        c.secondaryBlue = 0.4f;

        c.durationTicks = 20;
        c.soundEnabled = true;
        c.soundType = "TOTEM";
        c.volume = 1.0f;
        c.pitch = 1.0f;
        return c;
    }

    public static EffectConfig createDefaultBigDamage() {
        EffectConfig c = new EffectConfig();
        c.enabled = true;
        c.style = EffectStyle.RING;
        c.startRadius = 0.40f;
        c.endRadius = 4.00f;
        c.thickness = 0.38f;
        c.height = 2.60f;
        c.heightOffset = 0.05f;
        c.corners = 48;
        c.rings = 3;
        c.spin = 40.0f;
        c.followTarget = true;

        c.red = 1.0f;
        c.green = 0.35f;
        c.blue = 0.1f;
        c.alpha = 0.85f;

        c.durationTicks = 18;
        c.soundEnabled = true;
        c.soundType = "SMASH";
        c.volume = 1.0f;
        c.pitch = 1.1f;
        return c;
    }

    public static EffectConfig createDefaultKill() {
        EffectConfig c = new EffectConfig();
        c.enabled = true;
        c.style = EffectStyle.DOME;
        c.startRadius = 0.40f;
        c.endRadius = 2.20f;
        c.thickness = 0.18f;
        c.height = 2.60f;
        c.heightOffset = 0.05f;
        c.corners = 48;
        c.rings = 8;
        c.spin = 40.0f;
        c.followTarget = true;

        c.red = 0.8f;
        c.green = 0.1f;
        c.blue = 0.9f;
        c.alpha = 0.85f;

        c.durationTicks = 22;
        c.soundEnabled = true;
        c.soundType = "DEATH";
        c.volume = 1.0f;
        c.pitch = 0.9f;
        return c;
    }

    public static EffectConfig createDefaultDamageTaken() {
        EffectConfig c = new EffectConfig();
        c.enabled = true;
        c.style = EffectStyle.PILLAR;
        c.startRadius = 0.40f;
        c.endRadius = 1.40f;
        c.thickness = 0.18f;
        c.height = 2.00f;
        c.heightOffset = 0.05f;
        c.corners = 48;
        c.rings = 3;
        c.spin = 40.0f;
        c.followTarget = true;

        c.red = 1.0f;
        c.green = 0.15f;
        c.blue = 0.15f;
        c.alpha = 0.75f;

        c.durationTicks = 15;
        c.soundEnabled = false;
        c.volume = 0.8f;
        c.pitch = 1.0f;
        return c;
    }
}
