package com.maseffectsplus.gui;

import com.maseffectsplus.config.EffectConfig;
import com.maseffectsplus.config.EffectPresets;
import com.maseffectsplus.config.EffectStyle;
import com.maseffectsplus.config.ModConfig;
import com.maseffectsplus.render.EffectManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One compact page for a single effect. A short description explains what the effect is, colours are picked
 * from a palette (with an optional RGB fine-tune), and every setting has a tooltip.
 */
public class EffectEditScreen extends Screen {
    private static final int W = 150;
    private static final int H = 18;
    private static final int ROW = 22;
    private static final int START_Y = 44;

    /** Palette: red, orange, yellow, green, cyan, blue, purple, pink, white, black. */
    private static final int[] PALETTE = {
            0xFF2020, 0xFF7A1A, 0xFFD21A, 0x30E060, 0x20D8E8, 0x2F7BFF, 0x9B3BFF, 0xFF4FB0, 0xFFFFFF, 0x101010
    };

    private record ColourLabel(String text, int x, int y, Supplier<int[]> rgb) {}

    private final Screen parent;
    private final EffectConfig cfg;
    private final Supplier<EffectConfig> defaults;
    private final String description;

    private final boolean stunslam;
    private final boolean blackFlash;
    private final boolean ring;
    private final boolean hasSound;

    private boolean showRgb = false;
    private final List<ColourLabel> colourLabels = new ArrayList<>();

    /** @param stunslam true for the stunslam page: adds the trigger setting (mace only / any weapon) */
    public EffectEditScreen(Screen parent, String title, String description, EffectConfig cfg,
                            Supplier<EffectConfig> defaults, boolean stunslam) {
        super(Text.literal(title));
        this.parent = parent;
        this.description = description;
        this.cfg = cfg;
        this.defaults = defaults;
        this.stunslam = stunslam;
        this.blackFlash = cfg.style == EffectStyle.BLACK_FLASH;
        this.ring = cfg.style == EffectStyle.RING;
        // only the stunslam has its own sound; the other effects are purely visual
        this.hasSound = blackFlash;
    }

    private static Text onOff(String name, boolean v) {
        return Text.literal(name + ": " + (v ? "§aON" : "§cOFF"));
    }

    private static Text triggerText(ModConfig c) {
        return Text.literal("Trigger: " + (c.stunslamAnyWeapon ? "Any weapon" : "Mace only"));
    }

    private Text lookText() {
        int idx = EffectPresets.indexOf(cfg);
        return Text.literal("Look: " + (idx >= 0 ? EffectPresets.ALL[idx].name() : "Custom"));
    }

    private static <T extends ClickableWidget> T tip(T widget, String text) {
        widget.setTooltip(Tooltip.of(Text.literal(text)));
        return widget;
    }

    private int leftX() {
        return this.width / 2 - W - 8;
    }

    private int rightX() {
        return this.width / 2 + 8;
    }

    private static boolean sameColour(float[] a, int rgb) {
        return Math.abs(a[0] - ((rgb >> 16) & 0xFF) / 255.0f) < 0.02f
                && Math.abs(a[1] - ((rgb >> 8) & 0xFF) / 255.0f) < 0.02f
                && Math.abs(a[2] - (rgb & 0xFF) / 255.0f) < 0.02f;
    }

    private float[] primary() {
        return new float[]{cfg.red, cfg.green, cfg.blue};
    }

    private float[] secondary() {
        return new float[]{cfg.secondaryRed, cfg.secondaryGreen, cfg.secondaryBlue};
    }

    private void setPrimary(int rgb) {
        cfg.red = ((rgb >> 16) & 0xFF) / 255.0f;
        cfg.green = ((rgb >> 8) & 0xFF) / 255.0f;
        cfg.blue = (rgb & 0xFF) / 255.0f;
        ModConfig.save();
    }

    private void setSecondary(int rgb) {
        cfg.secondaryRed = ((rgb >> 16) & 0xFF) / 255.0f;
        cfg.secondaryGreen = ((rgb >> 8) & 0xFF) / 255.0f;
        cfg.secondaryBlue = (rgb & 0xFF) / 255.0f;
        ModConfig.save();
    }

    private CustomSliderWidget rgbSlider(int x, int y, int w, String letter, float value, Consumer<Float> set) {
        return new CustomSliderWidget(x, y, w, H, 0, 255, value * 255.0,
                v -> { set.accept((float) (v / 255.0)); ModConfig.save(); },
                v -> Text.literal(letter + " " + Math.round(v)));
    }

    /** Adds one colour group (label + preview + palette + optional RGB sliders) and returns the y below it. */
    private int addColourGroup(int x, int y, String label, boolean isPrimary) {
        colourLabels.add(new ColourLabel(label, x, y,
                () -> {
                    float[] c = isPrimary ? primary() : secondary();
                    return new int[]{Math.round(c[0] * 255), Math.round(c[1] * 255), Math.round(c[2] * 255)};
                }));
        int swY = y + 12;
        int size = 13;
        int gap = 2;
        for (int i = 0; i < PALETTE.length; i++) {
            int rgb = PALETTE[i];
            this.addDrawableChild(tip(new SwatchButton(x + i * (size + gap), swY, size, rgb,
                    () -> sameColour(isPrimary ? primary() : secondary(), rgb),
                    () -> {
                        if (isPrimary) setPrimary(rgb); else setSecondary(rgb);
                    }), "Click to use this colour"));
        }
        int next = swY + size + 6;
        if (showRgb) {
            int gap2 = 4;
            int cw = (W - gap2 * 2) / 3;
            float[] c = isPrimary ? primary() : secondary();
            this.addDrawableChild(rgbSlider(x, next, cw, "R", c[0], v -> { if (isPrimary) cfg.red = v; else cfg.secondaryRed = v; }));
            this.addDrawableChild(rgbSlider(x + cw + gap2, next, cw, "G", c[1], v -> { if (isPrimary) cfg.green = v; else cfg.secondaryGreen = v; }));
            this.addDrawableChild(rgbSlider(x + (cw + gap2) * 2, next, cw, "B", c[2], v -> { if (isPrimary) cfg.blue = v; else cfg.secondaryBlue = v; }));
            next += H + 4;
        }
        return next + 6;
    }

    @Override
    protected void init() {
        // init() is also called directly to rebuild the page: remove the old widgets first, otherwise they pile up
        this.clearChildren();
        colourLabels.clear();
        int lx = leftX();
        int rx = rightX();
        int row = 0;

        // ---- left column: trigger, look, size, duration, sound ----
        if (stunslam) {
            ModConfig modConfig = ModConfig.get();
            this.addDrawableChild(tip(ButtonWidget.builder(triggerText(modConfig), btn -> {
                        modConfig.stunslamAnyWeapon = !modConfig.stunslamAnyWeapon;
                        btn.setMessage(triggerText(modConfig));
                        ModConfig.save();
                    })
                    .dimensions(lx, START_Y + ROW * row++, W, H).build(),
                    "Mace only: needs a real smash (falling or elytra).\nAny weapon: the next hit after the shield break."));
        }
        if (blackFlash || ring) {
            this.addDrawableChild(tip(ButtonWidget.builder(lookText(), btn -> {
                        EffectPresets.applyNext(cfg);
                        ModConfig.save();
                        this.init();
                    })
                    .dimensions(lx, START_Y + ROW * row++, W, H).build(),
                    "One click changes the colours. Click again for the next look."));
        }
        if (blackFlash) {
            this.addDrawableChild(tip(new CustomSliderWidget(lx, START_Y + ROW * row++, W, H, 0.4, 2.0, cfg.scale,
                    v -> { cfg.scale = v.floatValue(); ModConfig.save(); },
                    v -> Text.literal(String.format(Locale.ROOT, "Size: %.1fx", v))), "How big the effect is."));
            this.addDrawableChild(tip(new CustomSliderWidget(lx, START_Y + ROW * row++, W, H, 0.2, 1.0, cfg.density,
                    v -> { cfg.density = v.floatValue(); ModConfig.save(); },
                    v -> Text.literal("Detail: " + Math.round(v * 100) + "%")),
                    "How many tendrils are drawn. Lower = more FPS."));
        } else if (cfg.style != EffectStyle.PILLAR) {
            this.addDrawableChild(tip(new CustomSliderWidget(lx, START_Y + ROW * row++, W, H, 1.0, 8.0, cfg.endRadius,
                    v -> { cfg.endRadius = v.floatValue(); ModConfig.save(); },
                    v -> Text.literal(String.format(Locale.ROOT, "Size: %.1f blocks", v))),
                    "How far the effect spreads."));
        }
        this.addDrawableChild(tip(new CustomSliderWidget(lx, START_Y + ROW * row++, W, H, 8, 60, cfg.durationTicks,
                v -> { cfg.durationTicks = (int) Math.round(v); ModConfig.save(); },
                v -> Text.literal(String.format(Locale.ROOT, "Duration: %.1fs", v / 20.0))),
                "How long the effect stays on screen."));
        if (hasSound) {
            this.addDrawableChild(tip(ButtonWidget.builder(onOff("Sound", cfg.soundEnabled), btn -> {
                        cfg.soundEnabled = !cfg.soundEnabled;
                        btn.setMessage(onOff("Sound", cfg.soundEnabled));
                        ModConfig.save();
                    })
                    .dimensions(lx, START_Y + ROW * row++, W, H).build(), "Play a sound together with the effect."));
            this.addDrawableChild(tip(new CustomSliderWidget(lx, START_Y + ROW * row++, W, H, 0, 200, cfg.volume * 100,
                    v -> { cfg.volume = (float) (v / 100.0); ModConfig.save(); },
                    v -> Text.literal("Volume: " + Math.round(v) + "%")), "How loud the sound is."));
        }
        int leftRows = row;

        // ---- right column: colours ----
        String first = blackFlash ? "Inside colour" : ring ? "Ring colour" : "Colour";
        int y = addColourGroup(rx, START_Y, first, true);
        if (blackFlash || ring) {
            y = addColourGroup(rx, y, blackFlash ? "Glow colour" : "Edge colour", false);
        }
        if (cfg.style == EffectStyle.SHOCKWAVE || cfg.style == EffectStyle.DOME || cfg.style == EffectStyle.PILLAR) {
            this.addDrawableChild(tip(new CustomSliderWidget(rx, y, W, H, 10, 100, cfg.alpha * 100,
                    v -> { cfg.alpha = (float) (v / 100.0); ModConfig.save(); },
                    v -> Text.literal("Opacity: " + Math.round(v) + "%")), "0% = invisible, 100% = solid."));
            y += ROW;
        }
        this.addDrawableChild(tip(ButtonWidget.builder(Text.literal(showRgb ? "Custom colour ▲" : "Custom colour ▼"), btn -> {
                    showRgb = !showRgb;
                    this.init();
                })
                .dimensions(rx, y, W, H).build(), "Fine-tune the colour with red / green / blue sliders."));
        y += ROW;

        // ---- bottom row ----
        int bottomY = Math.max(START_Y + leftRows * ROW, y) + 8;
        int cx = this.width / 2;
        this.addDrawableChild(tip(ButtonWidget.builder(Text.literal("Preview"), btn -> EffectManager.spawnPreviewEffect(cfg))
                .dimensions(cx - 155, bottomY, 100, H).build(), "Shows the effect in front of you."));
        this.addDrawableChild(tip(ButtonWidget.builder(Text.literal("Reset"), btn -> {
                    copyFrom(defaults.get());
                    ModConfig.save();
                    this.init();
                })
                .dimensions(cx - 50, bottomY, 100, H).build(), "Back to the default settings of this effect."));
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> this.close())
                .dimensions(cx + 55, bottomY, 100, H).build());
    }

    /** Copies every setting except the style from src (the style decides which options exist). */
    private void copyFrom(EffectConfig src) {
        cfg.enabled = src.enabled;
        cfg.startRadius = src.startRadius;
        cfg.endRadius = src.endRadius;
        cfg.thickness = src.thickness;
        cfg.height = src.height;
        cfg.heightOffset = src.heightOffset;
        cfg.corners = src.corners;
        cfg.rings = src.rings;
        cfg.spin = src.spin;
        cfg.followTarget = src.followTarget;
        cfg.scale = src.scale;
        cfg.density = src.density;
        cfg.red = src.red;
        cfg.green = src.green;
        cfg.blue = src.blue;
        cfg.alpha = src.alpha;
        cfg.secondaryRed = src.secondaryRed;
        cfg.secondaryGreen = src.secondaryGreen;
        cfg.secondaryBlue = src.secondaryBlue;
        cfg.rainbow = src.rainbow;
        cfg.durationTicks = src.durationTicks;
        cfg.fadeInTicks = src.fadeInTicks;
        cfg.fadeOutTicks = src.fadeOutTicks;
        cfg.soundEnabled = src.soundEnabled;
        cfg.volume = src.volume;
        cfg.pitch = src.pitch;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x99000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, this.description, this.width / 2, 24, 0xFFAAAAAA);
        for (ColourLabel label : colourLabels) {
            context.drawTextWithShadow(this.textRenderer, label.text(), label.x(), label.y() + 1, 0xFFFFAA00);
            int[] c = label.rgb().get();
            int px = label.x() + W - 26;
            context.fill(px - 1, label.y(), px + 27, label.y() + 10, 0xFF000000);
            context.fill(px, label.y() + 1, px + 26, label.y() + 9, 0xFF000000 | (c[0] << 16) | (c[1] << 8) | c[2]);
        }
    }

    @Override
    public void close() {
        ModConfig.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
