package com.github.wclark.simplespells;

import org.joml.Vector3f;

public enum SpellMode {
    INSTANT_HEALING("instant_healing", "message.simplespells.staff_mode.instant_healing", 1.0F, 0.28F, 0.68F),
    DAMAGE("damage", "message.simplespells.staff_mode.damage", 0.45F, 0.02F, 0.02F),
    GOOD_EFFECT("good_effect", "message.simplespells.staff_mode.good_effect", 0.18F, 0.82F, 0.25F),
    BAD_EFFECT("bad_effect", "message.simplespells.staff_mode.bad_effect", 0.32F, 0.04F, 0.55F);

    private static final SpellMode[] VALUES = values();

    private final String serializedName;
    private final String translationKey;
    private final float red;
    private final float green;
    private final float blue;

    SpellMode(String serializedName, String translationKey, float red, float green, float blue) {
        this.serializedName = serializedName;
        this.translationKey = translationKey;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public SpellMode next() {
        return byId(this.ordinal() + 1);
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public Vector3f getParticleColor() {
        return new Vector3f(this.red, this.green, this.blue);
    }

    public static SpellMode byId(int id) {
        int wrappedId = Math.floorMod(id, VALUES.length);
        return VALUES[wrappedId];
    }
}
