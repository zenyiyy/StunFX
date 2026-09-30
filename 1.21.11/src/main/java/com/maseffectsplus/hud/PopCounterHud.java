package com.maseffectsplus.hud;

import com.maseffectsplus.combat.PopCounterManager;
import com.maseffectsplus.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.List;

public class PopCounterHud {
    private static final int MAX_LINES = 8;
    private static final int EDGE = 6; // distance to the screen edge
    private static final long HIGHLIGHT_MS = 1500L;

    /** Pop count -> colour: yellow (1), orange (2), red (3+). */
    private static int countColor(int count) {
        if (count <= 1) return 0xFFFFDD55;
        if (count == 2) return 0xFFFF9933;
        return 0xFFFF4444;
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        try {
            ModConfig config = ModConfig.get();
            if (!config.effectsEnabled || !config.popCounterEnabled) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.options.hudHidden) return;

            List<PopCounterManager.PopEntry> entries = PopCounterManager.getSortedEntries();

            TextRenderer tr = client.textRenderer;
            int shown = Math.min(entries.size(), MAX_LINES);
            int extra = entries.size() - shown;

            String title = "Totem Pops";
            String[] names = new String[shown];
            String[] counts = new String[shown];
            int nameW = tr.getWidth(title);
            int gap = 8;
            int countW = 0;
            for (int i = 0; i < shown; i++) {
                PopCounterManager.PopEntry e = entries.get(i);
                names[i] = e.playerName;
                counts[i] = e.count + (e.count == 1 ? " pop" : " pops");
                nameW = Math.max(nameW, tr.getWidth(names[i]));
                countW = Math.max(countW, tr.getWidth(counts[i]));
            }
            // Always show the panel while the counter is on, so it's clear it is running
            String empty = shown == 0 ? "No pops yet" : null;
            String more = extra > 0 ? "+" + extra + " more" : null;
            int contentW = Math.max(nameW + gap + countW, tr.getWidth(title));
            if (more != null) contentW = Math.max(contentW, tr.getWidth(more));
            if (empty != null) contentW = Math.max(contentW, tr.getWidth(empty));

            int pad = 4;
            int lineH = 10;
            int boxW = contentW + pad * 2;
            int boxH = pad * 2 + 12 + shown * lineH + (more != null || empty != null ? lineH : 0);
            // Position: chosen corner + offsets, scaled with the size setting
            float s = config.popScale;
            int screenW = context.getScaledWindowWidth();
            int screenH = context.getScaledWindowHeight();
            float scaledW = boxW * s;
            float scaledH = boxH * s;
            boolean right = config.popCorner == 0 || config.popCorner == 2;
            boolean bottom = config.popCorner >= 2;
            float px = right ? screenW - scaledW - EDGE : EDGE;
            float py = bottom ? screenH - scaledH - EDGE : EDGE;
            context.getMatrices().pushMatrix();
            try {
            context.getMatrices().translate(px, py);
            context.getMatrices().scale(s, s);
            int boxX = 0;
            int boxY = 0;

            // Readable background with a thin gold accent on the left
            context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0x88000000);
            context.fill(boxX, boxY, boxX + 1, boxY + boxH, 0xFFFFAA00);

            int textX = boxX + pad + 1;
            int y = boxY + pad;
            context.drawTextWithShadow(tr, title, textX, y, 0xFFFFAA00);
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
                context.drawTextWithShadow(tr, names[i], textX, y, nameColor);
                context.drawTextWithShadow(tr, counts[i], rightEdge - tr.getWidth(counts[i]), y, countColor(e.count));
                y += lineH;
            }
            if (more != null) {
                context.drawTextWithShadow(tr, more, textX, y, 0xFF888888);
            } else if (empty != null) {
                context.drawTextWithShadow(tr, empty, textX, y, 0xFF888888);
            }
            } finally {
                context.getMatrices().popMatrix();
            }
        } catch (Throwable t) {
            // Never crash the HUD loop
        }
    }
}
