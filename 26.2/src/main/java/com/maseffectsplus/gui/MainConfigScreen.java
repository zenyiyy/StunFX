package com.maseffectsplus.gui;

import com.maseffectsplus.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class MainConfigScreen extends Screen {
    private final Screen parent;

    public MainConfigScreen(Screen parent) {
        super(Component.literal("PopEffects"));
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
        this.addRenderableWidget(Button.builder(
                getToggleText("Effects", config.effectsEnabled),
                btn -> {
                    config.effectsEnabled = !config.effectsEnabled;
                    btn.setMessage(getToggleText("Effects", config.effectsEnabled));
                    ModConfig.save();
                })
                .bounds(leftX, startY, buttonWidth, buttonHeight)
                .build());

        // On yourself: ON / OFF
        this.addRenderableWidget(Button.builder(
                getToggleText("On yourself", config.onYourself),
                btn -> {
                    config.onYourself = !config.onYourself;
                    btn.setMessage(getToggleText("On yourself", config.onYourself));
                    ModConfig.save();
                })
                .bounds(leftX, startY + spacing, buttonWidth, buttonHeight)
                .build());

        // On players: ON / OFF
        this.addRenderableWidget(Button.builder(
                getToggleText("On players", config.onPlayers),
                btn -> {
                    config.onPlayers = !config.onPlayers;
                    btn.setMessage(getToggleText("On players", config.onPlayers));
                    ModConfig.save();
                })
                .bounds(leftX, startY + spacing * 2, buttonWidth, buttonHeight)
                .build());

        // On mobs: ON / OFF
        this.addRenderableWidget(Button.builder(
                getToggleText("On mobs", config.onMobs),
                btn -> {
                    config.onMobs = !config.onMobs;
                    btn.setMessage(getToggleText("On mobs", config.onMobs));
                    ModConfig.save();
                })
                .bounds(leftX, startY + spacing * 3, buttonWidth, buttonHeight)
                .build());

        // Own hits only: OFF / ON
        this.addRenderableWidget(Button.builder(
                getToggleText("Own hits only", config.ownHitsOnly),
                btn -> {
                    config.ownHitsOnly = !config.ownHitsOnly;
                    btn.setMessage(getToggleText("Own hits only", config.ownHitsOnly));
                    ModConfig.save();
                })
                .bounds(leftX, startY + spacing * 4, buttonWidth, buttonHeight)
                .build());

        // Pop counter: ON / OFF
        this.addRenderableWidget(Button.builder(
                getToggleText("Pop counter", config.popCounterEnabled),
                btn -> {
                    config.popCounterEnabled = !config.popCounterEnabled;
                    btn.setMessage(getToggleText("Pop counter", config.popCounterEnabled));
                    ModConfig.save();
                })
                .bounds(leftX, startY + spacing * 5, buttonWidth, buttonHeight)
                .build());

        // Death sound (Unstable SMP style): ON / OFF
        this.addRenderableWidget(Button.builder(
                getToggleText("Death sound", config.deathSoundEnabled),
                btn -> {
                    config.deathSoundEnabled = !config.deathSoundEnabled;
                    btn.setMessage(getToggleText("Death sound", config.deathSoundEnabled));
                    ModConfig.save();
                })
                .bounds(leftX, startY + spacing * 6, buttonWidth, buttonHeight)
                .build());

        // --- Right Column ---

        // Edit Stunslam (Black Flash)
        this.addRenderableWidget(Button.builder(
                Component.literal("Edit Stunslam (Black Flash)"),
                btn -> this.minecraft.setScreenAndShow(new EffectDetailScreen(this, "Stunslam (Black Flash)", config.stunslam)))
                .bounds(rightX, startY, buttonWidth, buttonHeight)
                .build());

        // Edit Totem pop
        this.addRenderableWidget(Button.builder(
                Component.literal("Edit Totem pop"),
                btn -> this.minecraft.setScreenAndShow(new EffectDetailScreen(this, "Totem pop", config.totemPop)))
                .bounds(rightX, startY + spacing, buttonWidth, buttonHeight)
                .build());

        // Edit Big damage
        this.addRenderableWidget(Button.builder(
                Component.literal("Edit Big damage"),
                btn -> this.minecraft.setScreenAndShow(new EffectDetailScreen(this, "Big damage", config.bigDamage)))
                .bounds(rightX, startY + spacing * 2, buttonWidth, buttonHeight)
                .build());

        // Edit Kill
        this.addRenderableWidget(Button.builder(
                Component.literal("Edit Kill"),
                btn -> this.minecraft.setScreenAndShow(new EffectDetailScreen(this, "Kill", config.kill)))
                .bounds(rightX, startY + spacing * 3, buttonWidth, buttonHeight)
                .build());

        // Edit Damage taken
        this.addRenderableWidget(Button.builder(
                Component.literal("Edit Damage taken"),
                btn -> this.minecraft.setScreenAndShow(new EffectDetailScreen(this, "Damage taken", config.damageTaken)))
                .bounds(rightX, startY + spacing * 4, buttonWidth, buttonHeight)
                .build());

        // Range: 48 blocks slider
        this.addRenderableWidget(new CustomSliderWidget(
                rightX, startY + spacing * 5, buttonWidth, buttonHeight,
                8.0, 128.0, config.range,
                val -> {
                    config.range = Math.round(val);
                    ModConfig.save();
                },
                val -> Component.literal("Range: " + Math.round(val) + " blocks")
        ));

        // Max effects: 32 slider
        this.addRenderableWidget(new CustomSliderWidget(
                rightX, startY + spacing * 6, buttonWidth, buttonHeight,
                4.0, 128.0, config.maxEffects,
                val -> {
                    config.maxEffects = (int) Math.round(val);
                    ModConfig.save();
                },
                val -> Component.literal("Max effects: " + (int) Math.round(val))
        ));

        // Death sound volume slider
        this.addRenderableWidget(new CustomSliderWidget(
                leftX, startY + spacing * 7, buttonWidth, buttonHeight,
                0.0, 200.0, config.deathSoundVolume * 100.0,
                val -> {
                    config.deathSoundVolume = (float) (val / 100.0);
                    ModConfig.save();
                },
                val -> Component.literal("Death sound vol: " + Math.round(val) + "%")
        ));

        // Done button
        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                btn -> this.onClose())
                .bounds(centerX - 100, startY + spacing * 8 + 10, 200, buttonHeight)
                .build());
    }

    private Component getToggleText(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        ModConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }
}
