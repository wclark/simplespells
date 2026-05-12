package com.github.wclark.simplespells;

import net.minecraft.world.item.Item;

public class ArchmageStaffItem extends Item {
    private final ArchmageStaffSpell spell;

    public ArchmageStaffItem(ArchmageStaffSpell spell, Properties properties) {
        super(properties);
        this.spell = spell;
    }

    public ArchmageStaffSpell getSpell() {
        return this.spell;
    }
}
