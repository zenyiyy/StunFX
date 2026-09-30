package com.maseffectsplus.gui;

import com.maseffectsplus.config.ModConfig;
import com.maseffectsplus.hud.ComboHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Size, position and display time of the "BLACK FLASH x3" combo. A sample is shown behind this screen. */
public class ComboScreen extends Screen {
    private static final int W = 170;
    private static final int H = 18;
    private static final int ROW = 22;
    private static final int START_Y = 30;

    private final Screen parent;

    public ComboScreen(Screen parent) {
        super(Component.literal("Combo display"));
        this.parent = parent;
    }

    private static Component onOff(String name, boolean value) {
        return Component.literal(name + ": " + (value ? "§aON" : "§cOFF"));
    }

    private static <T extends AbstractWidget> T tip(T widget, String text) {
        widget.setTooltip(Tooltip.create(Component.literal(text)));
        return widget;
    }

    @Override
    protected void init() {
        this.clearWidgets(); // also called directly to rebuild the page: remove the old widgets first
        ComboHud.previewing = true; // show a sample combo behind the menu
        ModConfig config = ModConfig.get();
        int x = this.width / 2 - W / 2;
        int row = 0;

        this.addRenderableWidget(tip(Button.builder(onOff("Stunslam combo", config.comboCounterEnabled), btn -> {
                    config.comboCounterEnabled = !config.comboCounterEnabled;
                    btn.setMessage(onOff("Stunslam combo", config.comboCounterEnabled));
                    ModConfig.save();
                })
                .bounds(x, START_Y + ROW * row++, W, H).build(),
                "Shows how many stunslams in a row you landed.\nIt only ends when you fail a stunslam or die."));

        this.addRenderableWidget(tip(new CustomSliderWidget(x, START_Y + ROW * row++, W, H, 0.5, 2.5, config.comboScale,
                v -> { config.comboScale = v.floatValue(); ModConfig.save(); },
                v -> Component.literal(String.format(Locale.ROOT, "Size: %.1fx", v))), "How big the combo number is."));

        this.addRenderableWidget(tip(Button.builder(Component.literal("Move on screen..."),
                        btn -> this.minecraft.setScreenAndShow(new HudDragScreen(this, HudDragScreen.Target.COMBO)))
                .bounds(x, START_Y + ROW * row++, W, H).build(),
                "Drag the combo to the place you want with the mouse."));

        this.addRenderableWidget(tip(new CustomSliderWidget(x, START_Y + ROW * row++, W, H, 2, 15, config.comboWindowSeconds,
                v -> { config.comboWindowSeconds = (int) Math.round(v); ModConfig.save(); },
                v -> Component.literal("Show for: " + Math.round(v) + "s")),
                "How long the number stays on screen after a stunslam.\nThe combo itself keeps counting until you fail one."));

        this.addRenderableWidget(tip(Button.builder(Component.literal("Reset settings"), btn -> {
                    config.comboCounterEnabled = true;
                    config.comboScale = 1.0f;
                    config.comboX = 50;
                    config.comboY = 62;
                    config.comboWindowSeconds = 6;
                    ModConfig.save();
                    this.init(); // rebuild so all controls show the defaults
                })
                .bounds(x, START_Y + ROW * row++, W, H).build(), "Back to the default size and position."));

        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> this.onClose())
                .bounds(x, START_Y + ROW * row + 6, W, H).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
    }

    @Override
    public void removed() {
        ComboHud.previewing = false; // whatever way this screen closes
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
