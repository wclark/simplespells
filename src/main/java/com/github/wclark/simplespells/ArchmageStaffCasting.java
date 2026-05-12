package com.github.wclark.simplespells;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ArchmageStaffCasting {
    private static final int SPELL_COOLDOWN_TICKS = 200;
    private static final int FULL_ARCHMAGE_ARMOR_SPELL_COOLDOWN_TICKS = 100;

    private ArchmageStaffCasting() {
    }

    public static boolean cast(ServerPlayer player, ArchmageStaffSpell spell) {
        if (player.getCooldowns().isOnCooldown(spell.staff())) {
            return false;
        }

        if (!consumeEssence(player, spell)) {
            triggerBacklash(player, spell);
            player.getCooldowns().addCooldown(spell.staff(), getSpellCooldownTicks(player));
            player.swing(InteractionHand.MAIN_HAND, true);
            return true;
        }

        ServerLevel level = player.serverLevel();
        ArchmageSpellProjectile projectile = new ArchmageSpellProjectile(SimpleSpells.ARCHMAGE_SPELL_PROJECTILE.get(), level);
        Vec3 look = player.getLookAngle();
        projectile.setOwner(player);
        projectile.setSpell(spell);
        projectile.setPos(player.getX() + look.x * 0.6D, player.getEyeY() - 0.1D + look.y * 0.2D, player.getZ() + look.z * 0.6D);
        projectile.shoot(look.x, look.y, look.z, 3.1F, 0.0F);
        level.addFreshEntity(projectile);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.75F, 0.8F);
        player.getCooldowns().addCooldown(spell.staff(), getSpellCooldownTicks(player));
        player.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }

    static int getSpellCooldownTicks(Player player) {
        return isWearingFullArchmageArmor(player) ? FULL_ARCHMAGE_ARMOR_SPELL_COOLDOWN_TICKS : SPELL_COOLDOWN_TICKS;
    }

    private static boolean isWearingFullArchmageArmor(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(SimpleSpells.ARCHMAGE_MASK.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(SimpleSpells.ARCHMAGE_GARBS.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(SimpleSpells.ARCHMAGE_PANTS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(SimpleSpells.ARCHMAGE_BOOTS.get());
    }

    private static boolean consumeEssence(ServerPlayer player, ArchmageStaffSpell spell) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.items) {
            if (stack.is(spell.essence())) {
                stack.shrink(1);
                return true;
            }
        }

        for (ItemStack stack : inventory.offhand) {
            if (stack.is(spell.essence())) {
                stack.shrink(1);
                return true;
            }
        }

        return false;
    }

    private static void triggerBacklash(ServerPlayer player, ArchmageStaffSpell spell) {
        ServerLevel level = player.serverLevel();
        player.hurt(player.damageSources().magic(), 6.0F);
        Entity entity = spell == ArchmageStaffSpell.TORMENT ? EntityType.BLAZE.create(level) : EntityType.BREEZE.create(level);
        if (entity == null) {
            return;
        }

        Vec3 spawnOffset = player.getLookAngle().multiply(1.4D, 0.0D, 1.4D);
        if (spawnOffset.lengthSqr() < 0.01D) {
            spawnOffset = new Vec3(1.4D, 0.0D, 0.0D);
        }

        entity.moveTo(player.getX() + spawnOffset.x, player.getY(), player.getZ() + spawnOffset.z, player.getYRot(), 0.0F);
        level.addFreshEntity(entity);
        if (spell == ArchmageStaffSpell.TORMENT) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.8F, 0.75F);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_SHOOT, SoundSource.HOSTILE, 0.8F, 0.95F);
        }
    }
}
