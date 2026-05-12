package com.github.wclark.simplespells;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArchmageStaffSpellTest {
    @Test
    void byIdWrapsForwardAndBackward() {
        assertEquals(ArchmageStaffSpell.TORMENT, ArchmageStaffSpell.byId(0));
        assertEquals(ArchmageStaffSpell.TORMENT, ArchmageStaffSpell.byId(2));
        assertEquals(ArchmageStaffSpell.RELIEF, ArchmageStaffSpell.byId(-1));
    }
}
