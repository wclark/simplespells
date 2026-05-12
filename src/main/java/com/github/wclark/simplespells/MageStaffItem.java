package com.github.wclark.simplespells;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MageStaffItem extends SwordItem {
    public static final int SPELL_COOLDOWN_TICKS = 40;
    public static final int FULL_MAGE_ARMOR_SPELL_COOLDOWN_TICKS = 20;
    private static final String MODE_TAG = "SpellMode";

    public MageStaffItem(Properties properties) {
        super(Tiers.IRON, properties.attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            SpellMode mode = getMode(stack).next();
            setMode(stack, mode);
            player.displayClientMessage(Component.translatable("message.simplespells.staff_mode", Component.translatable(mode.getTranslationKey())), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.25F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static SpellMode getMode(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return SpellMode.byId(tag.getInt(MODE_TAG));
    }

    private static void setMode(ItemStack stack, SpellMode mode) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MODE_TAG, mode.ordinal()));
    }

    public static boolean cast(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(SimpleSpells.MAGE_STAFF.get()) || player.getCooldowns().isOnCooldown(SimpleSpells.MAGE_STAFF.get())) {
            return false;
        }

        if (!consumeMagicDust(player)) {
            triggerBacklash(player);
            player.getCooldowns().addCooldown(SimpleSpells.MAGE_STAFF.get(), getSpellCooldownTicks(player));
            player.swing(InteractionHand.MAIN_HAND, true);
            return true;
        }

        ServerLevel level = player.serverLevel();
        SpellMode mode = getMode(stack);
        SpellProjectile projectile = new SpellProjectile(SimpleSpells.SPELL_PROJECTILE.get(), level);
        Vec3 look = player.getLookAngle();
        projectile.setOwner(player);
        projectile.setMode(mode);
        projectile.setPos(player.getX() + look.x * 0.6D, player.getEyeY() - 0.1D + look.y * 0.2D, player.getZ() + look.z * 0.6D);
        projectile.shoot(look.x, look.y, look.z, 2.6F, 0.0F);
        level.addFreshEntity(projectile);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.7F, 1.45F);
        player.getCooldowns().addCooldown(SimpleSpells.MAGE_STAFF.get(), getSpellCooldownTicks(player));
        player.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }

    static int getSpellCooldownTicks(Player player) {
        return isWearingFullMageArmor(player) ? FULL_MAGE_ARMOR_SPELL_COOLDOWN_TICKS : SPELL_COOLDOWN_TICKS;
    }

    private static boolean isWearingFullMageArmor(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(SimpleSpells.MAGE_HAT.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(SimpleSpells.MAGE_ROBES.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(SimpleSpells.MAGE_PANTS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(SimpleSpells.MAGE_BOOTS.get());
    }

    private static boolean consumeMagicDust(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.items) {
            if (stack.is(SimpleSpells.MAGIC_DUST.get())) {
                stack.shrink(1);
                return true;
            }
        }

        for (ItemStack stack : inventory.offhand) {
            if (stack.is(SimpleSpells.MAGIC_DUST.get())) {
                stack.shrink(1);
                return true;
            }
        }

        return false;
    }

    private static void triggerBacklash(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        player.hurt(player.damageSources().magic(), 1.0F);
        Entity entity = player.getRandom().nextBoolean() ? EntityType.SLIME.create(level) : EntityType.MAGMA_CUBE.create(level);
        if (entity == null) {
            return;
        }

        if (entity instanceof Slime slime) {
            slime.setSize(1, true);
        } else if (entity instanceof MagmaCube magmaCube) {
            magmaCube.setSize(1, true);
        }

        Vec3 spawnOffset = player.getLookAngle().multiply(1.25D, 0.0D, 1.25D);
        if (spawnOffset.lengthSqr() < 0.01D) {
            spawnOffset = new Vec3(1.25D, 0.0D, 0.0D);
        }

        entity.moveTo(player.getX() + spawnOffset.x, player.getY(), player.getZ() + spawnOffset.z, player.getYRot(), 0.0F);
        level.addFreshEntity(entity);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 0.7F, 1.3F);
    }
}
