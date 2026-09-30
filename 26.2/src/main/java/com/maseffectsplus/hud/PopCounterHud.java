package com.maseffectsplus.hud;

import com.maseffectsplus.combat.PopCounterManager;
import com.maseffectsplus.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;

import java.util.List;

public class PopCounterHud implements net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement {
    private static final int MAX_LINES = 8;
    public static final int EDGE = 6; // distance to the screen edge

    /** Set by the move screen: draw the counter even if it is switched off, so it can be placed. */
    public static volatile boolean previewing = false;
    /** Last drawn box {x, y, width, height} in scaled GUI pixels (used by the move screen for dragging). */
    public static volatile float[] lastRect = null;
    private static final long HIGHLIGHT_MS = 1500L;

    /** Pop count -> colour: yellow (1), orange (2), red (3+). */
    private static int countColor(int count) {
        if (count <= 1) return 0xFFFFDD55;
        if (count == 2) return 0xFFFF9933;
        return 0xFFFF4444;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, DeltaTracker deltaTracker) {
        render(context, deltaTracker);
    }

    public static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        try {
            ModConfig config = ModConfig.get();
            if (!previewing && (!config.effectsEnabled || !config.popCounterEnabled)) return;

            Minecraft client = Minecraft.getInstance();

            List<PopCounterManager.PopEntry> entries = PopCounterManager.getSortedEntries();

            Font tr = client.font;
            int shown = Math.min(entries.size(), MAX_LINES);
            int extra = entries.size() - shown;

            String title = "Totem Pops";
            String[] names = new String[shown];
            String[] counts = new String[shown];
            int nameW = tr.width(title);
            int gap = 8;
            int countW = 0;
            for (int i = 0; i < shown; i++) {
                PopCounterManager.PopEntry e = entries.get(i);
                names[i] = e.playerName;
                counts[i] = e.count + (e.count == 1 ? " pop" : " pops");
                nameW = Math.max(nameW, tr.width(names[i]));
                countW = Math.max(countW, tr.width(counts[i]));
            }
            // Always show the panel while the counter is on, so it's clear it is running
            String empty = shown == 0 ? "No pops yet" : null;
            String more = extra > 0 ? "+" + extra + " more" : null;
            int contentW = Math.max(nameW + gap + countW, tr.width(title));
            if (more != null) contentW = Math.max(contentW, tr.width(more));
            if (empty != null) contentW = Math.max(contentW, tr.width(empty));

            int pad = 4;
            int lineH = 10;
            int boxW = contentW + pad * 2;
            int boxH = pad * 2 + 12 + shown * lineH + (more != null || empty != null ? lineH : 0);
            // Position: chosen corner + offsets, scaled with the size setting
            float s = config.popScale;
            int screenW = context.guiWidth();
            int screenH = context.guiHeight();
            float scaledW = boxW * s;
            float scaledH = boxH * s;
            // popPosX / popPosY are percentages between the screen edges (with a small margin), so the box always
            // stays on screen whatever its size is
            float px = EDGE + Math.max(0.0f, screenW - scaledW - 2 * EDGE) * config.popPosX / 100.0f;
            float py = EDGE + Math.max(0.0f, screenH - scaledH - 2 * EDGE) * config.popPosY / 100.0f;
            lastRect = new float[]{px, py, scaledW, scaledH};
            context.pose().pushMatrix();
            try {
            context.pose().translate(px, py);
            context.pose().scale(s, s);
            int boxX = 0;
            int boxY = 0;

            // Readable background with a thin gold accent on the left
            context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0x88000000);
            context.fill(boxX, boxY, boxX + 1, boxY + boxH, 0xFFFFAA00);

            int textX = boxX + pad + 1;
            int y = boxY + pad;
            context.text(tr, title, textX, y, 0xFFFFAA00, true);
            y += 12;

            long now = System.currentTimeMillis();
            int rightEdge = boxX + boxW - pad;
            for (int i = 0; i < shown; i++) {
                PopCounterManager.PopEntry e = entries.get(i);
                boolean fresh = (now - e.lastPopTime) < HIGHLIGHT_MS;
                int nameColor = fresh ? 0xFFFFFFFF : 0xFFCCCCCC;
                if (fresh) {
                    // brief flash behind a line that just popped
                    context.fill(boxX + 2, y - 1, boxX + boxW, y + lineH - 1, 0x33FFAA00);
                }
                context.text(tr, names[i], textX, y, nameColor, true);
                context.text(tr, counts[i], rightEdge - tr.width(counts[i]), y, countColor(e.count), true);
                y += lineH;
            }
            if (more != null) {
                context.text(tr, more, textX, y, 0xFF888888, true);
            } else if (empty != null) {
                context.text(tr, empty, textX, y, 0xFF888888, true);
            }
            } finally {
                context.pose().popMatrix();
            }
        } catch (Throwable t) {
            // Never crash the HUD loop
        }
    }
}
