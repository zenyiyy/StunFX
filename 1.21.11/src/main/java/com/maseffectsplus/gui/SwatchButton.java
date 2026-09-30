package com.maseffectsplus.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;

/** A small clickable square filled with one colour (used for the colour palette). */
public class SwatchButton extends PressableWidget {
    private final int rgb;
    private final BooleanSupplier selected;
    private final Runnable onClick;

    public SwatchButton(int x, int y, int size, int rgb, BooleanSupplier selected, Runnable onClick) {
        super(x, y, size, size, Text.literal(""));
        this.rgb = rgb;
        this.selected = selected;
        this.onClick = onClick;
    }

    @Override
    public void onPress(AbstractInput input) {
        onClick.run();
    }

    /** Called on top of the normal button background: paint the colour with a border (white when selected). */
    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int border = selected.getAsBoolean() ? 0xFFFFFFFF : (isHovered() ? 0xFFBBBBBB : 0xFF000000);
        context.fill(x, y, x + getWidth(), y + getHeight(), border);
        context.fill(x + 1, y + 1, x + getWidth() - 1, y + getHeight() - 1, 0xFF000000 | rgb);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // decorative colour square, nothing to narrate
    }
}
