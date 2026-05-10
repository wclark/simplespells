package com.github.wclark.simplespells;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class SpellProjectile extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.INT);
    private static final String MODE_TAG = "SpellMode";
    private static final int MAX_LIFETIME_TICKS = 80;
    private static final float SPELL_AMOUNT = 5.0F;
    private static final int EFFECT_DURATION_TICKS = 200;
    private static final List<Holder<MobEffect>> GOOD_EFFECTS = List.of(
            MobEffects.MOVEMENT_SPEED,
            MobEffects.DIG_SPEED,
            MobEffects.DAMAGE_BOOST,
            MobEffects.REGENERATION,
            MobEffects.DAMAGE_RESISTANCE,
            MobEffects.FIRE_RESISTANCE,
            MobEffects.JUMP,
            MobEffects.NIGHT_VISION,
            MobEffects.LUCK
    );
    private static final List<Holder<MobEffect>> BAD_EFFECTS = List.of(
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.DIG_SLOWDOWN,
            MobEffects.WEAKNESS,
            MobEffects.POISON,
            MobEffects.CONFUSION,
            MobEffects.BLINDNESS,
            MobEffects.UNLUCK,
            MobEffects.DARKNESS
    );

    public SpellProjectile(EntityType<? extends SpellProjectile> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODE, SpellMode.INSTANT_HEALING.ordinal());
    }

    public SpellMode getMode() {
        return SpellMode.byId(this.entityData.get(DATA_MODE));
    }

    public void setMode(SpellMode mode) {
        this.entityData.set(DATA_MODE, mode.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        spawnTrailParticle();
        if (!this.level().isClientSide && this.tickCount > MAX_LIFETIME_TICKS) {
            this.discard();
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        SpellMode mode = getMode();

        if (this.level().isClientSide) {
            return;
        }

        if (target instanceof LivingEntity living) {
            if (mode == SpellMode.INSTANT_HEALING) {
                living.heal(SPELL_AMOUNT);
            } else if (mode == SpellMode.GOOD_EFFECT) {
                living.addEffect(new MobEffectInstance(randomEffect(GOOD_EFFECTS), EFFECT_DURATION_TICKS, 0), this.getEffectSource());
            } else if (mode == SpellMode.BAD_EFFECT) {
                living.addEffect(new MobEffectInstance(randomEffect(BAD_EFFECTS), EFFECT_DURATION_TICKS, 0), this.getEffectSource());
            }
        }

        if (mode == SpellMode.DAMAGE) {
            target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), SPELL_AMOUNT);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte)3);
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 18; i++) {
                this.level().addParticle(
                        particle(),
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        (this.random.nextDouble() - 0.5D) * 0.16D,
                        (this.random.nextDouble() - 0.5D) * 0.16D,
                        (this.random.nextDouble() - 0.5D) * 0.16D);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(MODE_TAG, getMode().ordinal());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setMode(SpellMode.byId(compound.getInt(MODE_TAG)));
    }

    private Holder<MobEffect> randomEffect(List<Holder<MobEffect>> effects) {
        return effects.get(this.random.nextInt(effects.size()));
    }

    private void spawnTrailParticle() {
        if (this.level().isClientSide) {
            this.level().addParticle(particle(), this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private DustParticleOptions particle() {
        return new DustParticleOptions(getMode().getParticleColor(), 1.0F);
    }
}
