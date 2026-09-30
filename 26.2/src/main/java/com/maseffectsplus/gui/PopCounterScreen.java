package com.maseffectsplus.gui;

import com.maseffectsplus.combat.PopCounterManager;
import com.maseffectsplus.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Everything about the totem pop counter in one small screen. The counter itself stays visible behind it. */
public class PopCounterScreen extends Screen {
    private static final int W = 170;
    private static final int H = 18;
    private static final int ROW = 22;
    private static final int START_Y = 36;

    private final Screen parent;

    public PopCounterScreen(Screen parent) {
        super(Component.literal("Totem pop counter"));
        this.parent = parent;
    }

    private static Component onOff(String name, boolean value) {
        return Component.literal(name + ": " + (value ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        this.clearWidgets(); // also called directly to rebuild the page: remove the old widgets first
        ModConfig config = ModConfig.get();
        int x = this.width / 2 - W / 2;

        this.addRenderableWidget(Button.builder(onOff("Pop counter", config.popCounterEnabled), btn -> {
                    config.popCounterEnabled = !config.popCounterEnabled;
                    btn.setMessage(onOff("Pop counter", config.popCounterEnabled));
                    ModConfig.save();
                })
                .bounds(x, START_Y, W, H).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset counter"), btn -> PopCounterManager.clearAll())
                .bounds(x, START_Y + ROW, W, H).build());

        Button move = Button.builder(Component.literal("Move on screen..."),
                        btn -> this.minecraft.setScreenAndShow(new HudDragScreen(this, HudDragScreen.Target.POP_COUNTER)))
                .bounds(x, START_Y + ROW * 2, W, H).build();
        move.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Drag the counter to the place you want with the mouse.")));
        this.addRenderableWidget(move);

        this.addRenderableWidget(new CustomSliderWidget(x, START_Y + ROW * 3, W, H, 0.5, 2.0, config.popScale,
                val -> { config.popScale = val.floatValue(); ModConfig.save(); },
                val -> Component.literal(String.format(Locale.ROOT, "Size: %.1fx", val))));

        this.addRenderableWidget(Button.builder(onOff("Pop sound", config.popCounterSound), btn -> {
                    config.popCounterSound = !config.popCounterSound;
                    btn.setMessage(onOff("Pop sound", config.popCounterSound));
                    ModConfig.save();
                })
                .bounds(x, START_Y + ROW * 4, W, H).build());

        this.addRenderableWidget(Button.builder(Component.literal("Add test pop"), btn -> PopCounterManager.recordPop("TestPlayer"))
                .bounds(x, START_Y + ROW * 5, W, H).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset settings"), btn -> {
                    config.popCounterEnabled = true;
                    config.popPosX = 100;
                    config.popPosY = 0;
                    config.popScale = 1.0f;
                    config.popCounterSound = false;
                    ModConfig.save();
                    this.init(); // rebuild so all buttons show the defaults
                })
                .bounds(x, START_Y + ROW * 6, W, H).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> this.onClose())
                .bounds(x, START_Y + ROW * 7 + 6, W, H).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        ModConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }
}
