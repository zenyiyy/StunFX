package com.maseffectsplus.gui;

import com.maseffectsplus.config.EffectConfig;
import com.maseffectsplus.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The main settings screen: two short columns. Every effect has an Edit button for its own options.
 */
public class MainConfigScreen extends Screen {
    private static final int W = 150;
    private static final int H = 18;
    private static final int GAP = 12;
    private static final int ROW = 21;
    private static final int START_Y = 44;
    private static final int[] ROWS_PER_COLUMN = {6, 4};

    private final Screen parent;

    public MainConfigScreen(Screen parent) {
        super(Text.literal("Stun FX"));
        this.parent = parent;
    }

    private int colX(int col) {
        int total = W * 2 + GAP;
        return this.width / 2 - total / 2 + col * (W + GAP);
    }

    private int rowY(int row) {
        return START_Y + row * ROW;
    }

    private static Text onOff(String name, boolean value) {
        return Text.literal(name + ": " + (value ? "§aON" : "§cOFF"));
    }

    private ButtonWidget toggle(int col, int row, int width, String name, String tooltip, BooleanSupplier get, Consumer<Boolean> set) {
        ButtonWidget button = ButtonWidget.builder(onOff(name, get.getAsBoolean()), btn -> {
                    set.accept(!get.getAsBoolean());
                    btn.setMessage(onOff(name, get.getAsBoolean()));
                    ModConfig.save();
                })
                .dimensions(colX(col), rowY(row), width, H)
                .build();
        button.setTooltip(Tooltip.of(Text.literal(tooltip)));
        return button;
    }

    /** A toggle with a small "Edit" button next to it that opens the effect's own page. */
    private void toggleWithEdit(int col, int row, String name, String description, EffectConfig cfg, String title,
                                Supplier<EffectConfig> defaults, boolean stunslam) {
        String options = stunslam ? "trigger, colours, size, sound and more" : "colours, size and duration";
        this.addDrawableChild(toggle(col, row, W - 40, name, description + "\nClick Edit for " + options + ".",
                () -> cfg.enabled, v -> cfg.enabled = v));
        ButtonWidget edit = ButtonWidget.builder(Text.literal("Edit"),
                        btn -> this.client.setScreen(new EffectEditScreen(this, title, description, cfg, defaults, stunslam)))
                .dimensions(colX(col) + W - 36, rowY(row), 36, H)
                .build();
        edit.setTooltip(Tooltip.of(Text.literal("Change " + options + ".")));
        this.addDrawableChild(edit);
    }

    /** Same look as the effect rows: an ON/OFF toggle with a small "Edit" button that opens a settings screen. */
    private void toggleWithScreen(int col, int row, String name, String description, String editTooltip,
                                  BooleanSupplier get, Consumer<Boolean> set, Supplier<Screen> screen) {
        this.addDrawableChild(toggle(col, row, W - 40, name, description, get, set));
        ButtonWidget edit = ButtonWidget.builder(Text.literal("Edit"), btn -> this.client.setScreen(screen.get()))
                .dimensions(colX(col) + W - 36, rowY(row), 36, H)
                .build();
        edit.setTooltip(Tooltip.of(Text.literal(editTooltip)));
        this.addDrawableChild(edit);
    }

    @Override
    protected void init() {
        ModConfig config = ModConfig.get();

        // --- Effects ---
        this.addDrawableChild(toggle(0, 0, W, "All effects", "Master switch for every visual effect of this mod.",
                () -> config.effectsEnabled, v -> config.effectsEnabled = v));
        toggleWithEdit(0, 1, "Stunslam", "Black Flash: break a shield, then land a mace smash.",
                config.stunslam, "Stunslam", EffectConfig::createDefaultStunslam, true);
        toggleWithEdit(0, 2, "Mace ring", "Shockwave ring on the ground when you hit with a mace.",
                config.bigDamage, "Mace hit ring", EffectConfig::createDefaultBigDamage, false);
        toggleWithEdit(0, 3, "Totem pop", "Ring that spreads when a player pops a totem.",
                config.totemPop, "Totem pop effect", EffectConfig::createDefaultTotemPop, false);
        toggleWithEdit(0, 4, "Kill", "Dome that appears when a player dies.",
                config.kill, "Kill effect", EffectConfig::createDefaultKill, false);
        this.addDrawableChild(toggle(0, 5, W, "Effects on mobs", "Also show effects on mobs, not only on players.",
                () -> config.onMobs, v -> config.onMobs = v));

        // --- Counter and sound ---
        toggleWithScreen(1, 0, "Pop counter", "Shows how many totems nearby players have popped.",
                "Position, size, sound and reset.",
                () -> config.popCounterEnabled, v -> config.popCounterEnabled = v, () -> new PopCounterScreen(this));
        toggleWithScreen(1, 1, "Stunslam combo", "Shows how many stunslams in a row you landed (x2, x3, ...).\nIt only ends when you fail a stunslam or die.",
                "Size, position and display time.",
                () -> config.comboCounterEnabled, v -> config.comboCounterEnabled = v, () -> new ComboScreen(this));
        this.addDrawableChild(toggle(1, 2, W, "Death sound", "Plays a sound whenever a player dies.",
                () -> config.deathSoundEnabled, v -> config.deathSoundEnabled = v));
        // Minecraft caps a sound at 100%, so the slider stops there (the sound file itself is already loud)
        this.addDrawableChild(new CustomSliderWidget(colX(1), rowY(3), W, H, 0, 100, Math.min(1.0f, config.deathSoundVolume) * 100,
                v -> { config.deathSoundVolume = (float) (v / 100.0); ModConfig.save(); },
                v -> Text.literal("Death volume: " + Math.round(v) + "%")));

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> this.close())
                .dimensions(this.width / 2 - 60, rowY(6) + 8, 120, H)
                .build());
    }

    /**
     * Blurred world, dimmed a bit more so name tags and signs behind the menu don't distract,
     * plus a dark box with an orange top line behind each column.
     */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x99000000);
        for (int col = 0; col < ROWS_PER_COLUMN.length; col++) {
            int x = colX(col) - 6;
            int y = START_Y - 17;
            int h = 17 + ROWS_PER_COLUMN[col] * ROW + 3;
            context.fill(x, y, x + W + 12, y + h, 0xAA101018);
            context.fill(x, y, x + W + 12, y + 1, 0xFFFFAA00);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Effects", colX(0), START_Y - 12, 0xFFFFAA00);
        context.drawTextWithShadow(this.textRenderer, "Counter & sound", colX(1), START_Y - 12, 0xFFFFAA00);
    }

    @Override
    public void close() {
        ModConfig.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
