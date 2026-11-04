package com.vladdisasters.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Визуальная часть чёрной дыры. Вся физика — в BlackHoleDisaster. */
public class BlackHoleEntity extends Entity {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.FLOAT);

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 1.0F);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    public void setRadius(float r) {
        this.entityData.set(RADIUS, r);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, 0, 0);
        if (level().isClientSide) {
            float r = getRadius();
            for (int i = 0; i < 6; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double d = r * (2.2 + random.nextDouble() * 2);
                double x = getX() + Math.cos(a) * d, z = getZ() + Math.sin(a) * d;
                double y = getY() + (random.nextDouble() - 0.5) * r * 0.4;
                // частицы летят к центру по спирали
                level().addAlwaysVisibleParticle(ParticleTypes.PORTAL, true, x, y, z, (getX() - x) * 0.3, 0, (getZ() - z) * 0.3);
            }
        } else if (tickCount > 20 * 120) {
            discard(); // страховка, если бедствие оборвалось
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(getRadius() * 4F);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512 * 512;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setRadius(tag.getFloat("Radius"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Radius", getRadius());
    }
}
