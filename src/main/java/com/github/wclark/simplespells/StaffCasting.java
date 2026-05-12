package com.github.wclark.simplespells;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class StaffCasting {
    private StaffCasting() {
    }

    public static boolean canRequestCast(ItemStack stack, Player player) {
        if (stack.is(SimpleSpells.MAGE_STAFF.get())) {
            return !player.getCooldowns().isOnCooldown(SimpleSpells.MAGE_STAFF.get());
        }

        if (stack.getItem() instanceof ArchmageStaffItem staff) {
            return !player.getCooldowns().isOnCooldown(staff);
        }

        return false;
    }

    public static boolean cast(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.is(SimpleSpells.MAGE_STAFF.get())) {
            return MageStaffItem.cast(player);
        }

        if (stack.getItem() instanceof ArchmageStaffItem staff) {
            return ArchmageStaffCasting.cast(player, staff.getSpell());
        }

        return false;
    }
}
