package com.vladdisasters.entity;

import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.disaster.ImpactEffects;
import com.vladdisasters.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Метеор / лавовая бомба / гига-астероид. Позиция сущности — центр глыбы. */
public class MeteorEntity extends Entity {
    public static final int KIND_METEOR = 0;
    public static final int KIND_LAVA_BOMB = 1;
    public static final int KIND_GIGA = 2;

    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.INT);

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public MeteorEntity(Level level, Vec3 pos, Vec3 velocity, float size, int kind) {
        this(ModEntities.METEOR.get(), level);
        this.setPos(pos.x, pos.y, pos.z);
        this.setDeltaMovement(velocity);
        this.entityData.set(SIZE, size);
        this.entityData.set(KIND, kind);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 1.0F);
        builder.define(KIND, KIND_METEOR);
    }

    public float getSize() {
        return this.entityData.get(SIZE);
    }

    public int getKind() {
        return this.entityData.get(KIND);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        int kind = getKind();
        if (kind == KIND_LAVA_BOMB) v = v.add(0, -0.06, 0);
        else if (kind == KIND_METEOR) v = v.add(0, -0.015, 0);
        setDeltaMovement(v);

        if (level().isClientSide) {
            clientParticles(kind);
            return;
        }

        float size = getSize();
        Vec3 pos = position();
        Vec3 next = pos.add(v);
        double low = kind == KIND_GIGA ? size * 0.5 : size * 0.35;
        BlockHitResult hit = level().clip(new ClipContext(pos.add(0, -low, 0), next.add(0, -low, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        if (hit.getType() != HitResult.Type.MISS) {
            impact(hit.getLocation());
            return;
        }
        // Попадание в существ
        if (kind != KIND_GIGA) {
            AABB box = new AABB(pos, next).inflate(size * 0.6);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box)) {
                impact(e.position());
                return;
            }
        }
        setPos(next.x, next.y, next.z);
        if (getY() < level().getMinBuildHeight() - 20 || tickCount > 1500) discard();
    }

    private void clientParticles(int kind) {
        float s = getSize();
        Level l = level();
        if (kind == KIND_GIGA) {
            for (int i = 0; i < 30; i++) {
                double ox = (random.nextDouble() - 0.5) * s * 1.4, oy = (random.nextDouble() - 0.5) * s * 1.4, oz = (random.nextDouble() - 0.5) * s * 1.4;
                l.addAlwaysVisibleParticle(i % 3 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, true, getX() + ox, getY() + oy, getZ() + oz, 0, 0, 0);
                l.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, getX() + ox, getY() + oy + s * 0.3, getZ() + oz, 0, 0.05, 0);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                double ox = (random.nextDouble() - 0.5) * s, oy = (random.nextDouble() - 0.5) * s, oz = (random.nextDouble() - 0.5) * s;
                l.addParticle(ParticleTypes.FLAME, getX() + ox, getY() + oy, getZ() + oz, 0, 0, 0);
                l.addParticle(ParticleTypes.LARGE_SMOKE, getX() + ox, getY() + oy, getZ() + oz, 0, 0.02, 0);
            }
            if (kind == KIND_LAVA_BOMB) l.addParticle(ParticleTypes.LAVA, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    private void impact(Vec3 at) {
        if (!(level() instanceof ServerLevel sl)) return;
        float size = getSize();
        int kind = getKind();
        BlockPos bp = BlockPos.containing(at);
        switch (kind) {
            case KIND_GIGA -> {
                ImpactEffects.bigImpact(sl, bp, (int) (size * 2.1F), (int) (size * 1.3F), (int) (size * 6F), 130F, true);
                // Огненные обломки во все стороны
                for (int i = 0; i < 28; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    double sp = 0.6 + random.nextDouble() * 1.2;
                    Vec3 vel = new Vec3(Math.cos(a) * sp, 1.0 + random.nextDouble() * 1.4, Math.sin(a) * sp);
                    sl.addFreshEntity(new MeteorEntity(sl, at.add(0, size, 0), vel, 0.8F + random.nextFloat(), KIND_LAVA_BOMB));
                }
            }
            case KIND_LAVA_BOMB -> {
                sl.explode(this, at.x, at.y, at.z, 1.2F + size * 1.2F, true, Level.ExplosionInteraction.TNT);
                scatter(sl, bp, 3, true);
                if (random.nextInt(9) == 0) {
                    BlockPos up = bp.above();
                    if (sl.getBlockState(up).isAir()) sl.setBlock(up, Blocks.LAVA.defaultBlockState(), 3);
                }
            }
            default -> {
                sl.explode(this, at.x, at.y, at.z, 2.5F + size * 1.6F, true, Level.ExplosionInteraction.TNT);
                scatter(sl, bp, 5, false);
            }
        }
        discard();
    }

    /** Раскалённые камни вокруг места падения. */
    private void scatter(ServerLevel sl, BlockPos c, int n, boolean volcanic) {
        for (int i = 0; i < n; i++) {
            BlockPos p = c.offset(random.nextInt(5) - 2, random.nextInt(3) - 2, random.nextInt(5) - 2);
            BlockState st = sl.getBlockState(p);
            if (!st.isAir() && st.isSolid() && DisasterUtil.breakable(sl, p, st)) {
                sl.setBlock(p, random.nextInt(2) == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState()
                        : volcanic ? Blocks.BASALT.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState(), 3);
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(getSize() * 1.5F);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double max = getKind() == KIND_GIGA ? 600 : 200;
        return distance < max * max;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(SIZE, tag.getFloat("Size"));
        this.entityData.set(KIND, tag.getInt("Kind"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Size", getSize());
        tag.putInt("Kind", getKind());
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
