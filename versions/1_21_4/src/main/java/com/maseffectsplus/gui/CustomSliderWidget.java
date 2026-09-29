package com.maseffectsplus.gui;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Function;

public class CustomSliderWidget extends SliderWidget {
    private final double min;
    private final double max;
    private final Consumer<Double> onApply;
    private final Function<Double, Text> messageProvider;

    public CustomSliderWidget(int x, int y, int width, int height,
                              double min, double max, double initialValue,
                              Consumer<Double> onApply,
                              Function<Double, Text> messageProvider) {
        super(x, y, width, height, Text.empty(), (initialValue - min) / (max - min));
        this.min = min;
        this.max = max;
        this.onApply = onApply;
        this.messageProvider = messageProvider;
        this.updateMessage();
    }

    public double getRealValue() {
        return min + (max - min) * this.value;
    }

    @Override
    protected void updateMessage() {
        if (messageProvider != null) {
            this.setMessage(messageProvider.apply(getRealValue()));
        }
    }

    @Override
    protected void applyValue() {
        if (onApply != null) {
            onApply.accept(getRealValue());
        }
    }
}
