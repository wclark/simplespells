package com.github.wclark.simplespells;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ArchmageSpellProjectile extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> DATA_SPELL = SynchedEntityData.defineId(ArchmageSpellProjectile.class, EntityDataSerializers.INT);
    private static final String SPELL_TAG = "ArchmageSpell";
    private static final int MAX_LIFETIME_TICKS = 80;
    private static final float SPELL_AMOUNT = 10.0F;
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
    private static final List<EntityType<? extends AgeableMob>> RELIEF_ANIMALS = List.of(
            EntityType.COW,
            EntityType.SHEEP,
            EntityType.PIG,
            EntityType.CHICKEN,
            EntityType.RABBIT
    );

    public ArchmageSpellProjectile(EntityType<? extends ArchmageSpellProjectile> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SPELL, ArchmageStaffSpell.TORMENT.ordinal());
    }

    public ArchmageStaffSpell getSpell() {
        return ArchmageStaffSpell.byId(this.entityData.get(DATA_SPELL));
    }

    public void setSpell(ArchmageStaffSpell spell) {
        this.entityData.set(DATA_SPELL, spell.ordinal());
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
        if (this.level().isClientSide || !(result.getEntity() instanceof LivingEntity living)) {
            return;
        }

        if (getSpell() == ArchmageStaffSpell.TORMENT) {
            living.hurt(this.damageSources().indirectMagic(this, this.getOwner()), SPELL_AMOUNT);
            addRandomEffects(living, BAD_EFFECTS);
        } else {
            living.heal(SPELL_AMOUNT);
            addRandomEffects(living, GOOD_EFFECTS);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            applyImpact(result.getLocation());
            this.level().broadcastEntityEvent(this, (byte)3);
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 24; i++) {
                this.level().addParticle(
                        particle(),
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        (this.random.nextDouble() - 0.5D) * 0.2D,
                        (this.random.nextDouble() - 0.5D) * 0.2D,
                        (this.random.nextDouble() - 0.5D) * 0.2D);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(SPELL_TAG, getSpell().ordinal());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setSpell(ArchmageStaffSpell.byId(compound.getInt(SPELL_TAG)));
    }

    private void applyImpact(Vec3 location) {
        ServerLevel level = (ServerLevel)this.level();
        if (getSpell() == ArchmageStaffSpell.TORMENT) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
            if (lightning != null) {
                lightning.moveTo(location.x, location.y, location.z);
                level.addFreshEntity(lightning);
            }
        } else {
            spawnReliefAnimal(level, location);
        }
    }

    private void spawnReliefAnimal(ServerLevel level, Vec3 location) {
        EntityType<? extends AgeableMob> type = RELIEF_ANIMALS.get(this.random.nextInt(RELIEF_ANIMALS.size()));
        AgeableMob animal = type.create(level);
        if (animal == null) {
            return;
        }

        animal.setBaby(true);
        animal.moveTo(location.x, location.y, location.z, this.random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(animal);
    }

    private void addRandomEffects(LivingEntity living, List<Holder<MobEffect>> effects) {
        List<Holder<MobEffect>> remainingEffects = new ArrayList<>(effects);
        for (int i = 0; i < 3; i++) {
            int index = this.random.nextInt(remainingEffects.size());
            living.addEffect(new MobEffectInstance(remainingEffects.remove(index), EFFECT_DURATION_TICKS, 1), this.getEffectSource());
        }
    }

    private void spawnTrailParticle() {
        if (this.level().isClientSide) {
            this.level().addParticle(particle(), this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private DustParticleOptions particle() {
        return new DustParticleOptions(getSpell().particleColor(), 1.15F);
    }
}
