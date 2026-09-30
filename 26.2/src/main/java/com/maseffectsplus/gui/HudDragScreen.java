package com.maseffectsplus.gui;

import com.maseffectsplus.config.ModConfig;
import com.maseffectsplus.hud.ComboHud;
import com.maseffectsplus.hud.PopCounterHud;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Full-screen editor: the element (combo number or pop counter) is shown where it is and can be dragged
 * with the mouse. The position is saved as soon as you let go.
 */
public class HudDragScreen extends Screen {
    public enum Target {
        COMBO("Drag the combo where you want it"),
        POP_COUNTER("Drag the pop counter where you want it");

        final String hint;
        Target(String hint) {
            this.hint = hint;
        }
    }

    private final Screen parent;
    private final Target target;
    private boolean dragging = false;
    private double grabDx;
    private double grabDy;

    public HudDragScreen(Screen parent, Target target) {
        super(Component.literal("Move on screen"));
        this.parent = parent;
        this.target = target;
    }

    private float[] rect() {
        return target == Target.COMBO ? ComboHud.lastRect : PopCounterHud.lastRect;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        ComboHud.previewing = target == Target.COMBO;
        PopCounterHud.previewing = target == Target.POP_COUNTER;

        int cx = this.width / 2;
        int y = this.height - 26;
        this.addRenderableWidget(Button.builder(Component.literal("Reset position"), btn -> {
                    ModConfig config = ModConfig.get();
                    if (target == Target.COMBO) {
                        config.comboX = 50;
                        config.comboY = 62;
                    } else {
                        config.popPosX = 100;
                        config.popPosY = 0;
                    }
                    ModConfig.save();
                })
                .bounds(cx - 115, y, 110, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> this.onClose())
                .bounds(cx + 5, y, 110, 20).build());
    }

    /** No blur or dimming: the element has to be seen exactly as it looks in the game. */
    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, target.hint, this.width / 2, 12, 0xFFFFFFFF);
        context.centeredText(this.font, "Hold the left mouse button on it and move. Done or Esc when finished.",
                this.width / 2, 24, 0xFFAAAAAA);

        float[] r = rect();
        if (r != null) {
            boolean hover = mouseX >= r[0] && mouseX <= r[0] + r[2] && mouseY >= r[1] && mouseY <= r[1] + r[3];
            int color = dragging ? 0xFF55FF55 : (hover ? 0xFFFFFF55 : 0xFFFFAA00);
            int x1 = Math.round(r[0]) - 3;
            int y1 = Math.round(r[1]) - 3;
            int x2 = Math.round(r[0] + r[2]) + 3;
            int y2 = Math.round(r[1] + r[3]) + 3;
            context.fill(x1, y1, x2, y1 + 1, color);
            context.fill(x1, y2 - 1, x2, y2, color);
            context.fill(x1, y1, x1 + 1, y2, color);
            context.fill(x2 - 1, y1, x2, y2, color);
        }
        // guide when the combo sits exactly in the horizontal middle
        if (target == Target.COMBO && ModConfig.get().comboX == 50) {
            context.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x5555FF55);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true; // one of the buttons
        }
        float[] r = rect();
        if (click.button() == 0 && r != null
                && click.x() >= r[0] - 3 && click.x() <= r[0] + r[2] + 3
                && click.y() >= r[1] - 3 && click.y() <= r[1] + r[3] + 3) {
            dragging = true;
            if (target == Target.COMBO) {
                grabDx = (r[0] + r[2] / 2.0) - click.x(); // the combo is positioned by its horizontal centre and top
            } else {
                grabDx = r[0] - click.x();                // the counter by its top left corner
            }
            grabDy = r[1] - click.y();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (!dragging) {
            return super.mouseDragged(click, offsetX, offsetY);
        }
        ModConfig config = ModConfig.get();
        double x = click.x() + grabDx;
        double y = click.y() + grabDy;

        if (target == Target.COMBO) {
            int px = (int) Math.round(clamp(x / this.width * 100.0));
            int py = (int) Math.round(clamp(y / this.height * 100.0));
            config.comboX = Math.abs(px - 50) <= 1 ? 50 : px; // snap to the middle
            config.comboY = py;
        } else {
            float[] r = rect();
            double w = r != null ? r[2] : 100;
            double h = r != null ? r[3] : 40;
            int edge = PopCounterHud.EDGE;
            double freeX = Math.max(1.0, this.width - w - 2 * edge);
            double freeY = Math.max(1.0, this.height - h - 2 * edge);
            int px = (int) Math.round(clamp((x - edge) / freeX * 100.0));
            int py = (int) Math.round(clamp((y - edge) / freeY * 100.0));
            config.popPosX = px <= 2 ? 0 : (px >= 98 ? 100 : px); // snap to the screen edges
            config.popPosY = py <= 2 ? 0 : (py >= 98 ? 100 : py);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragging) {
            dragging = false;
            ModConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(100.0, v));
    }

    @Override
    public void removed() {
        ComboHud.previewing = false;
        PopCounterHud.previewing = false;
        super.removed();
    }

    @Override
    public void onClose() {
        ModConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }
}
