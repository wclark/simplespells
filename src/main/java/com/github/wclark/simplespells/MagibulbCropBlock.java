package com.github.wclark.simplespells;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MagibulbCropBlock extends CropBlock {
    public static final MapCodec<MagibulbCropBlock> CODEC = simpleCodec(MagibulbCropBlock::new);

    public MagibulbCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.MYCELIUM) || state.is(BlockTags.DIRT) || state.getBlock() instanceof FarmBlock;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isGrowingSoil(level.getBlockState(pos.below()))) {
            super.randomTick(state, level, pos, random);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return !this.isMaxAge(state) && isGrowingSoil(level.getBlockState(pos.below()));
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return isGrowingSoil(level.getBlockState(pos.below()));
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return SimpleSpells.MAGIBULB_SEEDS.get();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(SimpleSpells.MAGIBULB_SEEDS.get());
    }

    private static boolean isGrowingSoil(BlockState state) {
        return state.is(Blocks.MYCELIUM);
    }
}
