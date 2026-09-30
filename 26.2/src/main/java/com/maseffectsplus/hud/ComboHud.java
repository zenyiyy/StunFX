package com.maseffectsplus.hud;

import com.maseffectsplus.combat.ComboTracker;
import com.maseffectsplus.config.EffectConfig;
import com.maseffectsplus.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;

/**
 * "BLACK FLASH x3": big number that pops on every new stunslam and fades out at the end of the display time.
 * Position and size are settings. While the combo settings screen is open a sample is shown so you can place it.
 */
public class ComboHud implements net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement {
    private static final long POP_MS = 250L;
    private static final long FADE_MS = 1000L;

    /** Set by the combo settings screen: show a sample combo so position and size can be adjusted live. */
    public static volatile boolean previewing = false;
    /** Last drawn area {x, y, width, height} in scaled GUI pixels (used by the move screen for dragging). */
    public static volatile float[] lastRect = null;

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, DeltaTracker deltaTracker) {
        render(context, deltaTracker);
    }

    public static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        try {
            ModConfig config = ModConfig.get();
            int count;
            float alpha = 1.0f;
            float pop = 1.0f;

            if (previewing) {
                count = 3;
            } else {
                if (!config.effectsEnabled || !config.comboCounterEnabled) return;
                count = ComboTracker.getCount();
                if (count < 2) return; // a single stunslam is not a combo yet

                // The number is only shown for a while after each stunslam; the combo keeps going in the background
                long age = ComboTracker.getAgeMs();
                long left = ComboTracker.displayMs() - age;
                if (left <= 0) return;
                alpha = left < FADE_MS ? Math.max(0.0f, left / (float) FADE_MS) : 1.0f;
                pop = age < POP_MS ? 1.0f + 0.6f * (1.0f - age / (float) POP_MS) : 1.0f;
            }

            int a = Math.max(5, Math.round(alpha * 255)); // alpha below 4 would be drawn fully opaque

            // Colour = the glow colour of the stunslam effect, so it matches the chosen look
            EffectConfig bf = config.stunslam;
            int rgb = (Math.round(bf.secondaryRed * 255) << 16) | (Math.round(bf.secondaryGreen * 255) << 8) | Math.round(bf.secondaryBlue * 255);
            int color = (a << 24) | rgb;
            int white = (a << 24) | 0xFFFFFF;

            Font tr = Minecraft.getInstance().font;
            float size = config.comboScale;
            int cx = Math.round(context.guiWidth() * config.comboX / 100.0f);
            int cy = Math.round(context.guiHeight() * config.comboY / 100.0f);

            float rectW = Math.max(tr.width("BLACK FLASH") * 1.2f, tr.width("x" + count) * 3.5f * pop) * size;
            float rectH = (12 + tr.lineHeight * 3.5f * pop) * size;
            lastRect = new float[]{cx - rectW / 2.0f, cy, rectW, rectH};

            context.pose().pushMatrix();
            try {
                context.pose().translate(cx, cy);
                context.pose().scale(size, size);

                context.pose().pushMatrix();
                context.pose().scale(1.2f, 1.2f);
                context.centeredText(tr, "BLACK FLASH", 0, 0, white);
                context.pose().popMatrix();

                context.pose().translate(0, 12);
                float s = 3.5f * pop;
                context.pose().scale(s, s);
                context.centeredText(tr, "x" + count, 0, 0, color);
            } finally {
                context.pose().popMatrix();
            }
        } catch (Throwable t) {
            // Never crash the HUD loop
        }
    }
}
