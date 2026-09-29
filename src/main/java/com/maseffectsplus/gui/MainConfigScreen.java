package com.maseffectsplus.gui;

import com.maseffectsplus.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MainConfigScreen extends Screen {
    private final Screen parent;

    public MainConfigScreen(Screen parent) {
        super(Text.literal("PopEffects"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig config = ModConfig.get();

        int buttonWidth = 150;
        int buttonHeight = 20;
        int spacing = 24;

        int centerX = this.width / 2;
        int leftX = centerX - buttonWidth - 5;
        int rightX = centerX + 5;
        int startY = 40;

        // --- Left Column ---

        // Effects: ON / OFF
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("Effects", config.effectsEnabled),
                btn -> {
                    config.effectsEnabled = !config.effectsEnabled;
                    btn.setMessage(getToggleText("Effects", config.effectsEnabled));
                    ModConfig.save();
                })
                .dimensions(leftX, startY, buttonWidth, buttonHeight)
                .build());

        // On yourself: ON / OFF
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("On yourself", config.onYourself),
                btn -> {
                    config.onYourself = !config.onYourself;
                    btn.setMessage(getToggleText("On yourself", config.onYourself));
                    ModConfig.save();
                })
                .dimensions(leftX, startY + spacing, buttonWidth, buttonHeight)
                .build());

        // On players: ON / OFF
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("On players", config.onPlayers),
                btn -> {
                    config.onPlayers = !config.onPlayers;
                    btn.setMessage(getToggleText("On players", config.onPlayers));
                    ModConfig.save();
                })
                .dimensions(leftX, startY + spacing * 2, buttonWidth, buttonHeight)
                .build());

        // On mobs: ON / OFF
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("On mobs", config.onMobs),
                btn -> {
                    config.onMobs = !config.onMobs;
                    btn.setMessage(getToggleText("On mobs", config.onMobs));
                    ModConfig.save();
                })
                .dimensions(leftX, startY + spacing * 3, buttonWidth, buttonHeight)
                .build());

        // Own hits only: OFF / ON
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("Own hits only", config.ownHitsOnly),
                btn -> {
                    config.ownHitsOnly = !config.ownHitsOnly;
                    btn.setMessage(getToggleText("Own hits only", config.ownHitsOnly));
                    ModConfig.save();
                })
                .dimensions(leftX, startY + spacing * 4, buttonWidth, buttonHeight)
                .build());

        // Pop counter: ON / OFF
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("Pop counter", config.popCounterEnabled),
                btn -> {
                    config.popCounterEnabled = !config.popCounterEnabled;
                    btn.setMessage(getToggleText("Pop counter", config.popCounterEnabled));
                    ModConfig.save();
                })
                .dimensions(leftX, startY + spacing * 5, buttonWidth, buttonHeight)
                .build());

        // Death sound (Unstable SMP style): ON / OFF
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("Death sound", config.deathSoundEnabled),
                btn -> {
                    config.deathSoundEnabled = !config.deathSoundEnabled;
                    btn.setMessage(getToggleText("Death sound", config.deathSoundEnabled));
                    ModConfig.save();
                })
                .dimensions(leftX, startY + spacing * 6, buttonWidth, buttonHeight)
                .build());

        // --- Right Column ---

        // Edit Stunslam (Black Flash)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Edit Stunslam (Black Flash)"),
                btn -> this.client.setScreen(new EffectDetailScreen(this, "Stunslam (Black Flash)", config.stunslam)))
                .dimensions(rightX, startY, buttonWidth, buttonHeight)
                .build());

        // Edit Totem pop
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Edit Totem pop"),
                btn -> this.client.setScreen(new EffectDetailScreen(this, "Totem pop", config.totemPop)))
                .dimensions(rightX, startY + spacing, buttonWidth, buttonHeight)
                .build());

        // Edit Big damage
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Edit Big damage"),
                btn -> this.client.setScreen(new EffectDetailScreen(this, "Big damage", config.bigDamage)))
                .dimensions(rightX, startY + spacing * 2, buttonWidth, buttonHeight)
                .build());

        // Edit Kill
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Edit Kill"),
                btn -> this.client.setScreen(new EffectDetailScreen(this, "Kill", config.kill)))
                .dimensions(rightX, startY + spacing * 3, buttonWidth, buttonHeight)
                .build());

        // Edit Damage taken
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Edit Damage taken"),
                btn -> this.client.setScreen(new EffectDetailScreen(this, "Damage taken", config.damageTaken)))
                .dimensions(rightX, startY + spacing * 4, buttonWidth, buttonHeight)
                .build());

        // Range: 48 blocks slider
        this.addDrawableChild(new CustomSliderWidget(
                rightX, startY + spacing * 5, buttonWidth, buttonHeight,
                8.0, 128.0, config.range,
                val -> {
                    config.range = Math.round(val);
                    ModConfig.save();
                },
                val -> Text.literal("Range: " + Math.round(val) + " blocks")
        ));

        // Max effects: 32 slider
        this.addDrawableChild(new CustomSliderWidget(
                rightX, startY + spacing * 6, buttonWidth, buttonHeight,
                4.0, 128.0, config.maxEffects,
                val -> {
                    config.maxEffects = (int) Math.round(val);
                    ModConfig.save();
                },
                val -> Text.literal("Max effects: " + (int) Math.round(val))
        ));

        // Death sound volume slider
        this.addDrawableChild(new CustomSliderWidget(
                leftX, startY + spacing * 7, buttonWidth, buttonHeight,
                0.0, 200.0, config.deathSoundVolume * 100.0,
                val -> {
                    config.deathSoundVolume = (float) (val / 100.0);
                    ModConfig.save();
                },
                val -> Text.literal("Death sound vol: " + Math.round(val) + "%")
        ));

        // Done button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                btn -> this.close())
                .dimensions(centerX - 100, startY + spacing * 8 + 10, 200, buttonHeight)
                .build());
    }

    private Text getToggleText(String name, boolean enabled) {
        return Text.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 18, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        ModConfig.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
