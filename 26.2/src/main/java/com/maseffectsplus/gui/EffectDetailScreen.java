package com.maseffectsplus.gui;

import com.maseffectsplus.config.EffectConfig;
import com.maseffectsplus.config.ModConfig;
import com.maseffectsplus.render.EffectManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class EffectDetailScreen extends Screen {
    private enum Tab {
        SHAPE("Shape"),
        COLOUR("Colour"),
        TIMING("Timing"),
        SOUND("Sound");

        final String title;
        Tab(String title) {
            this.title = title;
        }
    }

    private final Screen parent;
    private final String effectName;
    private final EffectConfig config;
    private Tab currentTab = Tab.SHAPE;

    public EffectDetailScreen(Screen parent, String effectName, EffectConfig config) {
        super(Component.literal(effectName));
        this.parent = parent;
        this.effectName = effectName;
        this.config = config;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int tabWidth = 65;
        int tabHeight = 18;
        int tabStartX = centerX - (tabWidth * 4 + 15) / 2;
        int tabY = 28;

        // --- Tab Selection Bar ---
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            int x = tabStartX + i * (tabWidth + 5);
            boolean isSelected = (tab == currentTab);
            this.addRenderableWidget(Button.builder(
                    Component.literal((isSelected ? "§6§l" : "") + tab.title),
                    btn -> {
                        currentTab = tab;
                        this.init();
                    })
                    .bounds(x, tabY, tabWidth, tabHeight)
                    .build());
        }

        int colWidth = 145;
        int colHeight = 20;
        int spacing = 22;
        int leftColX = centerX - colWidth - 5;
        int rightColX = centerX + 5;
        int contentStartY = 54;

        switch (currentTab) {
            case SHAPE -> initShapeTab(leftColX, rightColX, contentStartY, colWidth, colHeight, spacing);
            case COLOUR -> initColourTab(leftColX, rightColX, contentStartY, colWidth, colHeight, spacing);
            case TIMING -> initTimingTab(leftColX, rightColX, contentStartY, colWidth, colHeight, spacing);
            case SOUND -> initSoundTab(leftColX, rightColX, contentStartY, colWidth, colHeight, spacing);
        }

        // --- Bottom Controls (matching screenshots) ---
        int bottomY = this.height - 48;

        // Preview Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Preview"),
                btn -> EffectManager.spawnPreviewEffect(config))
                .bounds(centerX - 105, bottomY, 100, 20)
                .build());

        // Reset Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Reset"),
                btn -> {
                    resetToDefault();
                    this.init();
                })
                .bounds(centerX + 5, bottomY, 100, 20)
                .build());

        // Done Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                btn -> this.onClose())
                .bounds(centerX - 75, bottomY + 24, 150, 20)
                .build());
    }

    private void initShapeTab(int leftX, int rightX, int startY, int w, int h, int sp) {
        // Left Column:
        // Style
        this.addRenderableWidget(Button.builder(
                Component.literal("Style: " + config.style.getDisplayName()),
                btn -> {
                    config.style = config.style.next();
                    btn.setMessage(Component.literal("Style: " + config.style.getDisplayName()));
                    ModConfig.save();
                })
                .bounds(leftX, startY, w, h)
                .build());

        // Start radius
        this.addRenderableWidget(new CustomSliderWidget(
                leftX, startY + sp, w, h,
                0.1, 5.0, config.startRadius,
                val -> { config.startRadius = val.floatValue(); ModConfig.save(); },
                val -> Component.literal(String.format(Locale.ROOT, "Start radius: %.2f", val))
        ));

        // End radius
        this.addRenderableWidget(new CustomSliderWidget(
                leftX, startY + sp * 2, w, h,
                0.5, 10.0, config.endRadius,
                val -> { config.endRadius = val.floatValue(); ModConfig.save(); },
                val -> Component.literal(String.format(Locale.ROOT, "End radius: %.2f", val))
        ));

        // Thickness
        this.addRenderableWidget(new CustomSliderWidget(
                leftX, startY + sp * 3, w, h,
                0.05, 1.5, config.thickness,
                val -> { config.thickness = val.floatValue(); ModConfig.save(); },
                val -> Component.literal(String.format(Locale.ROOT, "Thickness: %.2f", val))
        ));

        // Height
        this.addRenderableWidget(new CustomSliderWidget(
                leftX, startY + sp * 4, w, h,
                0.2, 8.0, config.height,
                val -> { config.height = val.floatValue(); ModConfig.save(); },
                val -> Component.literal(String.format(Locale.ROOT, "Height: %.2f", val))
        ));

        // Height offset
        this.addRenderableWidget(new CustomSliderWidget(
                leftX, startY + sp * 5, w, h,
                -2.0, 3.0, config.heightOffset,
                val -> { config.heightOffset = val.floatValue(); ModConfig.save(); },
                val -> Component.literal(String.format(Locale.ROOT, "Height offset: %.2f", val))
        ));

        // Right Column:
        // Corners
        this.addRenderableWidget(new CustomSliderWidget(
                rightX, startY, w, h,
                12, 64, config.corners,
                val -> { config.corners = val.intValue(); ModConfig.save(); },
                val -> Component.literal("Corners: " + val.intValue())
        ));

        // Rings
        this.addRenderableWidget(new CustomSliderWidget(
                rightX, startY + sp, w, h,
                1, 16, config.rings,
                val -> { config.rings = val.intValue(); ModConfig.save(); },
                val -> Component.literal("Rings: " + val.intValue())
        ));

        // Spin
        this.addRenderableWidget(new CustomSliderWidget(
                rightX, startY + sp * 2, w, h,
                0, 360, config.spin,
                val -> { config.spin = val.floatValue(); ModConfig.save(); },
                val -> Component.literal("Spin: " + Math.round(val) + "°/s")
        ));

        // Follow target
        this.addRenderableWidget(Button.builder(
                Component.literal("Follow target: " + (config.followTarget ? "ON" : "OFF")),
                btn -> {
                    config.followTarget = !config.followTarget;
                    btn.setMessage(Component.literal("Follow target: " + (config.followTarget ? "ON" : "OFF")));
                    ModConfig.save();
                })
                .bounds(rightX, startY + sp * 3, w, h)
                .build());

        if (effectName.contains("Stunslam")) {
            // Overall size of the Black Flash (1.00 = default)
            this.addRenderableWidget(new CustomSliderWidget(
                    rightX, startY + sp * 4, w, h,
                    0.2, 3.0, config.scale,
                    val -> { config.scale = val.floatValue(); ModConfig.save(); },
                    val -> Component.literal(String.format(Locale.ROOT, "Size: %.2f", val))
            ));

            // What triggers the Black Flash after a shield break
            ModConfig modConfig = ModConfig.get();
            this.addRenderableWidget(Button.builder(
                    stunslamModeText(modConfig),
                    btn -> {
                        modConfig.stunslamAnyWeapon = !modConfig.stunslamAnyWeapon;
                        btn.setMessage(stunslamModeText(modConfig));
                        ModConfig.save();
                    })
                    .bounds(rightX, startY + sp * 5, w, h)
                    .build());
        }
    }

    private Component stunslamModeText(ModConfig modConfig) {
        return Component.literal("Trigger: " + (modConfig.stunslamAnyWeapon ? "Any weapon" : "Mace only"));
    }

    private void initColourTab(int leftX, int rightX, int startY, int w, int h, int sp) {
        // Primary RGB & Alpha
        this.addRenderableWidget(new CustomSliderWidget(
                leftX, startY, w, h, 0, 255, config.red * 255,
                val -> { config.red = (float) (val / 255.0); ModConfig.save(); },
                val -> Component.literal("Red: " + val.intValue())
        ));

        this.addRenderableWidget(new CustomSliderWidget(
                leftX, startY + sp, w, h, 0, 255, config.green * 255,
                val -> { config.green = (float) (val / 255.0); ModConfig.save(); },
                val -> Component.literal("Green: " + val.intValue())
        ));

        this.addRenderableWidget(new CustomSliderWidget(
                leftX, startY + sp * 2, w, h, 0, 255, config.blue * 255,
                val -> { config.blue = (float) (val / 255.0); ModConfig.save(); },
                val -> Component.literal("Blue: " + val.intValue())
        ));

        this.addRenderableWidget(new CustomSliderWidget(
                leftX, startY + sp * 3, w, h, 0, 100, config.alpha * 100,
                val -> { config.alpha = (float) (val / 100.0); ModConfig.save(); },
                val -> Component.literal("Alpha: " + val.intValue() + "%")
        ));

        // Secondary RGB (Edge/Lightning aura)
        this.addRenderableWidget(new CustomSliderWidget(
                rightX, startY, w, h, 0, 255, config.secondaryRed * 255,
                val -> { config.secondaryRed = (float) (val / 255.0); ModConfig.save(); },
                val -> Component.literal("Sec Red: " + val.intValue())
        ));

        this.addRenderableWidget(new CustomSliderWidget(
                rightX, startY + sp, w, h, 0, 255, config.secondaryGreen * 255,
                val -> { config.secondaryGreen = (float) (val / 255.0); ModConfig.save(); },
                val -> Component.literal("Sec Green: " + val.intValue())
        ));

        this.addRenderableWidget(new CustomSliderWidget(
                rightX, startY + sp * 2, w, h, 0, 255, config.secondaryBlue * 255,
                val -> { config.secondaryBlue = (float) (val / 255.0); ModConfig.save(); },
                val -> Component.literal("Sec Blue: " + val.intValue())
        ));

        // Rainbow toggle
        this.addRenderableWidget(Button.builder(
                Component.literal("Rainbow: " + (config.rainbow ? "ON" : "OFF")),
                btn -> {
                    config.rainbow = !config.rainbow;
                    btn.setMessage(Component.literal("Rainbow: " + (config.rainbow ? "ON" : "OFF")));
                    ModConfig.save();
                })
                .bounds(rightX, startY + sp * 3, w, h)
                .build());
    }

    private void initTimingTab(int leftX, int rightX, int startY, int w, int h, int sp) {
        this.addRenderableWidget(new CustomSliderWidget(
                leftX, startY, w, h, 5, 80, config.durationTicks,
                val -> { config.durationTicks = val.intValue(); ModConfig.save(); },
                val -> Component.literal("Duration: " + val.intValue() + " ticks")
        ));

        this.addRenderableWidget(new CustomSliderWidget(
                leftX, startY + sp, w, h, 0, 20, config.fadeInTicks,
                val -> { config.fadeInTicks = val.intValue(); ModConfig.save(); },
                val -> Component.literal("Fade In: " + val.intValue() + " ticks")
        ));

        this.addRenderableWidget(new CustomSliderWidget(
                rightX, startY, w, h, 0, 30, config.fadeOutTicks,
                val -> { config.fadeOutTicks = val.intValue(); ModConfig.save(); },
                val -> Component.literal("Fade Out: " + val.intValue() + " ticks")
        ));
    }

    private void initSoundTab(int leftX, int rightX, int startY, int w, int h, int sp) {
        this.addRenderableWidget(Button.builder(
                Component.literal("Sound: " + (config.soundEnabled ? "ON" : "OFF")),
                btn -> {
                    config.soundEnabled = !config.soundEnabled;
                    btn.setMessage(Component.literal("Sound: " + (config.soundEnabled ? "ON" : "OFF")));
                    ModConfig.save();
                })
                .bounds(leftX, startY, w, h)
                .build());

        this.addRenderableWidget(new CustomSliderWidget(
                leftX, startY + sp, w, h, 0, 200, config.volume * 100,
                val -> { config.volume = (float) (val / 100.0); ModConfig.save(); },
                val -> Component.literal("Volume: " + val.intValue() + "%")
        ));

        this.addRenderableWidget(new CustomSliderWidget(
                rightX, startY + sp, w, h, 50, 150, config.pitch * 100,
                val -> { config.pitch = (float) (val / 100.0); ModConfig.save(); },
                val -> Component.literal("Pitch: " + String.format(Locale.ROOT, "%.2f", val / 100.0))
        ));
    }

    private void resetToDefault() {
        if (effectName.contains("Stunslam")) {
            copyFrom(EffectConfig.createDefaultStunslam());
        } else if (effectName.contains("Totem")) {
            copyFrom(EffectConfig.createDefaultTotemPop());
        } else if (effectName.contains("Big damage")) {
            copyFrom(EffectConfig.createDefaultBigDamage());
        } else if (effectName.contains("Kill")) {
            copyFrom(EffectConfig.createDefaultKill());
        } else if (effectName.contains("Damage taken")) {
            copyFrom(EffectConfig.createDefaultDamageTaken());
        }
        ModConfig.save();
    }

    private void copyFrom(EffectConfig src) {
        config.style = src.style;
        config.startRadius = src.startRadius;
        config.endRadius = src.endRadius;
        config.thickness = src.thickness;
        config.height = src.height;
        config.heightOffset = src.heightOffset;
        config.corners = src.corners;
        config.rings = src.rings;
        config.spin = src.spin;
        config.followTarget = src.followTarget;
        config.scale = src.scale;
        config.red = src.red;
        config.green = src.green;
        config.blue = src.blue;
        config.alpha = src.alpha;
        config.secondaryRed = src.secondaryRed;
        config.secondaryGreen = src.secondaryGreen;
        config.secondaryBlue = src.secondaryBlue;
        config.rainbow = src.rainbow;
        config.durationTicks = src.durationTicks;
        config.fadeInTicks = src.fadeInTicks;
        config.fadeOutTicks = src.fadeOutTicks;
        config.soundEnabled = src.soundEnabled;
        config.volume = src.volume;
        config.pitch = src.pitch;
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
