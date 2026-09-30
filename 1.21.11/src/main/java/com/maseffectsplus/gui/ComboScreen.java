package com.maseffectsplus.gui;

import com.maseffectsplus.config.ModConfig;
import com.maseffectsplus.hud.ComboHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.Locale;

/** Size, position and display time of the "BLACK FLASH x3" combo. A sample is shown behind this screen. */
public class ComboScreen extends Screen {
    private static final int W = 170;
    private static final int H = 18;
    private static final int ROW = 22;
    private static final int START_Y = 30;

    private final Screen parent;

    public ComboScreen(Screen parent) {
        super(Text.literal("Combo display"));
        this.parent = parent;
    }

    private static Text onOff(String name, boolean value) {
        return Text.literal(name + ": " + (value ? "§aON" : "§cOFF"));
    }

    private static <T extends ClickableWidget> T tip(T widget, String text) {
        widget.setTooltip(Tooltip.of(Text.literal(text)));
        return widget;
    }

    @Override
    protected void init() {
        this.clearChildren(); // also called directly to rebuild the page: remove the old widgets first
        ComboHud.previewing = true; // show a sample combo behind the menu
        ModConfig config = ModConfig.get();
        int x = this.width / 2 - W / 2;
        int row = 0;

        this.addDrawableChild(tip(ButtonWidget.builder(onOff("Stunslam combo", config.comboCounterEnabled), btn -> {
                    config.comboCounterEnabled = !config.comboCounterEnabled;
                    btn.setMessage(onOff("Stunslam combo", config.comboCounterEnabled));
                    ModConfig.save();
                })
                .dimensions(x, START_Y + ROW * row++, W, H).build(),
                "Shows how many stunslams in a row you landed.\nIt only ends when you fail a stunslam or die."));

        this.addDrawableChild(tip(new CustomSliderWidget(x, START_Y + ROW * row++, W, H, 0.5, 2.5, config.comboScale,
                v -> { config.comboScale = v.floatValue(); ModConfig.save(); },
                v -> Text.literal(String.format(Locale.ROOT, "Size: %.1fx", v))), "How big the combo number is."));

        this.addDrawableChild(tip(ButtonWidget.builder(Text.literal("Move on screen..."),
                        btn -> this.client.setScreen(new HudDragScreen(this, HudDragScreen.Target.COMBO)))
                .dimensions(x, START_Y + ROW * row++, W, H).build(),
                "Drag the combo to the place you want with the mouse."));

        this.addDrawableChild(tip(new CustomSliderWidget(x, START_Y + ROW * row++, W, H, 2, 15, config.comboWindowSeconds,
                v -> { config.comboWindowSeconds = (int) Math.round(v); ModConfig.save(); },
                v -> Text.literal("Show for: " + Math.round(v) + "s")),
                "How long the number stays on screen after a stunslam.\nThe combo itself keeps counting until you fail one."));

        this.addDrawableChild(tip(ButtonWidget.builder(Text.literal("Reset settings"), btn -> {
                    config.comboCounterEnabled = true;
                    config.comboScale = 1.0f;
                    config.comboX = 50;
                    config.comboY = 62;
                    config.comboWindowSeconds = 6;
                    ModConfig.save();
                    this.init(); // rebuild so all controls show the defaults
                })
                .dimensions(x, START_Y + ROW * row++, W, H).build(), "Back to the default size and position."));

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> this.close())
                .dimensions(x, START_Y + ROW * row + 6, W, H).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFFFF);
    }

    @Override
    public void removed() {
        ComboHud.previewing = false; // whatever way this screen closes
        super.removed();
    }

    @Override
    public void close() {
        ModConfig.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
