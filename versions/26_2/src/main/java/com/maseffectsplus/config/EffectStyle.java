package com.maseffectsplus.config;

public enum EffectStyle {
    SHOCKWAVE("Shockwave"),
    RING("Ring"),
    DOME("Dome"),
    PILLAR("Pillar"),
    BLACK_FLASH("Black Flash");

    private final String displayName;

    EffectStyle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public EffectStyle next() {
        EffectStyle[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
