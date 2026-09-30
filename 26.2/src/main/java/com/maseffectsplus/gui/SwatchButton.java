package com.maseffectsplus.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/** A small clickable square filled with one colour (used for the colour palette). */
public class SwatchButton extends AbstractButton {
    private final int rgb;
    private final BooleanSupplier selected;
    private final Runnable onClick;

    public SwatchButton(int x, int y, int size, int rgb, BooleanSupplier selected, Runnable onClick) {
        super(x, y, size, size, Component.literal(""));
        this.rgb = rgb;
        this.selected = selected;
        this.onClick = onClick;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        onClick.run();
    }

    /** Called on top of the normal button background: paint the colour with a border (white when selected). */
    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int border = selected.getAsBoolean() ? 0xFFFFFFFF : (isHovered() ? 0xFFBBBBBB : 0xFF000000);
        context.fill(x, y, x + getWidth(), y + getHeight(), border);
        context.fill(x + 1, y + 1, x + getWidth() - 1, y + getHeight() - 1, 0xFF000000 | rgb);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // decorative colour square, nothing to narrate
    }
}
