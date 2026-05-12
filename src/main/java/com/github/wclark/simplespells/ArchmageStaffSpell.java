package com.github.wclark.simplespells;

import org.joml.Vector3f;

import net.minecraft.world.item.Item;

public enum ArchmageStaffSpell {
    TORMENT(new Vector3f(0.02F, 0.02F, 0.025F), () -> SimpleSpells.DEATH_ESSENCE.get(), () -> SimpleSpells.STAFF_OF_TORMENT.get()),
    RELIEF(new Vector3f(0.96F, 0.96F, 1.0F), () -> SimpleSpells.LIFE_ESSENCE.get(), () -> SimpleSpells.STAFF_OF_RELIEF.get());

    private final Vector3f particleColor;
    private final ItemSupplier essence;
    private final ItemSupplier staff;

    ArchmageStaffSpell(Vector3f particleColor, ItemSupplier essence, ItemSupplier staff) {
        this.particleColor = particleColor;
        this.essence = essence;
        this.staff = staff;
    }

    public Vector3f particleColor() {
        return new Vector3f(this.particleColor);
    }

    public Item essence() {
        return this.essence.get();
    }

    public Item staff() {
        return this.staff.get();
    }

    public static ArchmageStaffSpell byId(int id) {
        ArchmageStaffSpell[] values = values();
        return values[Math.floorMod(id, values.length)];
    }

    private interface ItemSupplier {
        Item get();
    }
}
