package com.github.wclark.simplespells;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SpellModeTest {
    @Test
    void byIdWrapsForwardAndBackward() {
        assertEquals(SpellMode.INSTANT_HEALING, SpellMode.byId(0));
        assertEquals(SpellMode.INSTANT_HEALING, SpellMode.byId(4));
        assertEquals(SpellMode.BAD_EFFECT, SpellMode.byId(-1));
    }

    @Test
    void nextCyclesThroughAllModes() {
        assertEquals(SpellMode.DAMAGE, SpellMode.INSTANT_HEALING.next());
        assertEquals(SpellMode.GOOD_EFFECT, SpellMode.DAMAGE.next());
        assertEquals(SpellMode.BAD_EFFECT, SpellMode.GOOD_EFFECT.next());
        assertEquals(SpellMode.INSTANT_HEALING, SpellMode.BAD_EFFECT.next());
    }

    @Test
    void serializedNamesStayStableForItemData() {
        assertEquals("instant_healing", SpellMode.INSTANT_HEALING.getSerializedName());
        assertEquals("damage", SpellMode.DAMAGE.getSerializedName());
        assertEquals("good_effect", SpellMode.GOOD_EFFECT.getSerializedName());
        assertEquals("bad_effect", SpellMode.BAD_EFFECT.getSerializedName());
    }
}
