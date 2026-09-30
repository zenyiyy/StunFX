package com.maseffectsplus.config;

/**
 * One-click looks. Colours: "core" is the dark inside of the Black Flash tendrils, "edge" is the glowing outline
 * (for the other effect styles they are simply the primary and secondary colour).
 */
public final class EffectPresets {
    private EffectPresets() {}

    public record Preset(String name, float[] core, float[] edge, float density) {}

    public static final Preset[] ALL = {
            new Preset("Manga",       new float[]{0.03f, 0.00f, 0.04f}, new float[]{1.00f, 0.05f, 0.10f}, 1.0f),
            new Preset("Cursed Blue", new float[]{0.02f, 0.03f, 0.08f}, new float[]{0.15f, 0.65f, 1.00f}, 1.0f),
            new Preset("Gold",        new float[]{0.06f, 0.04f, 0.00f}, new float[]{1.00f, 0.75f, 0.15f}, 1.0f),
            new Preset("Violet",      new float[]{0.04f, 0.00f, 0.06f}, new float[]{0.70f, 0.20f, 1.00f}, 1.0f),
            new Preset("Minimal",     new float[]{0.03f, 0.00f, 0.04f}, new float[]{1.00f, 0.05f, 0.10f}, 0.4f),
    };

    /** Returns the index of the preset that matches the effect's current colours, or -1 for a custom look. */
    public static int indexOf(EffectConfig c) {
        for (int i = 0; i < ALL.length; i++) {
            Preset p = ALL[i];
            if (near(c.red, p.core[0]) && near(c.green, p.core[1]) && near(c.blue, p.core[2])
                    && near(c.secondaryRed, p.edge[0]) && near(c.secondaryGreen, p.edge[1]) && near(c.secondaryBlue, p.edge[2])
                    && near(c.density, p.density)) {
                return i;
            }
        }
        return -1;
    }

    public static void apply(EffectConfig c, Preset p) {
        c.red = p.core[0];
        c.green = p.core[1];
        c.blue = p.core[2];
        c.secondaryRed = p.edge[0];
        c.secondaryGreen = p.edge[1];
        c.secondaryBlue = p.edge[2];
        c.density = p.density;
    }

    /** Applies the next preset after the current one (starting with the first if the look is custom). */
    public static Preset applyNext(EffectConfig c) {
        Preset p = ALL[(indexOf(c) + 1) % ALL.length];
        apply(c, p);
        return p;
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 0.01f;
    }
}
