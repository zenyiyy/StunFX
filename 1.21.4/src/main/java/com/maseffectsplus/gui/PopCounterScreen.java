package com.maseffectsplus.gui;

import com.maseffectsplus.combat.PopCounterManager;
import com.maseffectsplus.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Locale;

/** Everything about the totem pop counter in one small screen. The counter itself stays visible behind it. */
public class PopCounterScreen extends Screen {
    private static final int W = 170;
    private static final int H = 18;
    private static final int ROW = 22;
    private static final int START_Y = 36;

    private final Screen parent;

    public PopCounterScreen(Screen parent) {
        super(Text.literal("Totem pop counter"));
        this.parent = parent;
    }

    private static Text onOff(String name, boolean value) {
        return Text.literal(name + ": " + (value ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        this.clearChildren(); // also called directly to rebuild the page: remove the old widgets first
        ModConfig config = ModConfig.get();
        int x = this.width / 2 - W / 2;

        this.addDrawableChild(ButtonWidget.builder(onOff("Pop counter", config.popCounterEnabled), btn -> {
                    config.popCounterEnabled = !config.popCounterEnabled;
                    btn.setMessage(onOff("Pop counter", config.popCounterEnabled));
                    ModConfig.save();
                })
                .dimensions(x, START_Y, W, H).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset counter"), btn -> PopCounterManager.clearAll())
                .dimensions(x, START_Y + ROW, W, H).build());

        ButtonWidget move = ButtonWidget.builder(Text.literal("Move on screen..."),
                        btn -> this.client.setScreen(new HudDragScreen(this, HudDragScreen.Target.POP_COUNTER)))
                .dimensions(x, START_Y + ROW * 2, W, H).build();
        move.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Drag the counter to the place you want with the mouse.")));
        this.addDrawableChild(move);

        this.addDrawableChild(new CustomSliderWidget(x, START_Y + ROW * 3, W, H, 0.5, 2.0, config.popScale,
                val -> { config.popScale = val.floatValue(); ModConfig.save(); },
                val -> Text.literal(String.format(Locale.ROOT, "Size: %.1fx", val))));

        this.addDrawableChild(ButtonWidget.builder(onOff("Pop sound", config.popCounterSound), btn -> {
                    config.popCounterSound = !config.popCounterSound;
                    btn.setMessage(onOff("Pop sound", config.popCounterSound));
                    ModConfig.save();
                })
                .dimensions(x, START_Y + ROW * 4, W, H).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add test pop"), btn -> PopCounterManager.recordPop("TestPlayer"))
                .dimensions(x, START_Y + ROW * 5, W, H).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset settings"), btn -> {
                    config.popCounterEnabled = true;
                    config.popPosX = 100;
                    config.popPosY = 0;
                    config.popScale = 1.0f;
                    config.popCounterSound = false;
                    ModConfig.save();
                    this.init(); // rebuild so all buttons show the defaults
                })
                .dimensions(x, START_Y + ROW * 6, W, H).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> this.close())
                .dimensions(x, START_Y + ROW * 7 + 6, W, H).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        ModConfig.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
