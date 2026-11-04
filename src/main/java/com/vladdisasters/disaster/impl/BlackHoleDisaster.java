package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.entity.BlackHoleEntity;
import com.vladdisasters.network.ClientDisasterState;
import com.vladdisasters.registry.ModEntities;
import com.vladdisasters.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Чёрная дыра: появляется в небе, растёт, затягивает существ и блоки.
 * Через минуту схлопывается со взрывом.
 */
public class BlackHoleDisaster extends Disaster {
    private static final int PULL_RANGE = 70;
    private BlackHoleEntity hole;
    private Vec3 center;

    public BlackHoleDisaster(DisasterType type, ServerLevel level, ServerPlayer target) {
        super(type, level, target, 60 * 20);
    }

    @Override
    public void start() {
        BlockPos s = null;
        for (int i = 0; i < 10 && s == null; i++) s = DisasterUtil.randomSurface(level, origin, 35, 50);
        if (s == null) s = origin;
        center = new Vec3(s.getX() + 0.5, s.getY() + 14, s.getZ() + 0.5);
        hole = ModEntities.BLACK_HOLE.get().create(level);
        if (hole == null) {
            done = true;
            return;
        }
        hole.moveTo(center.x, center.y, center.z, 0, 0);
        hole.setRadius(0.5F);
        level.addFreshEntity(hole);
        DisasterUtil.broadcast(level, Component.literal("🕳 Пространство разрывается... появилась чёрная дыра!").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
    }

    private float radius() {
        float t = (float) age / duration;
        return 0.5F + 4.5F * Math.min(1F, t * 1.4F);
    }

    @Override
    public void tick() {
        if (hole == null) return;
        float r = radius();
        hole.setRadius(r);
        if (age % 60 == 0) {
            level.playSound(null, center.x, center.y, center.z, ModSounds.BLACKHOLE_HUM.get(), SoundSource.HOSTILE, 8.0F, 0.6F + r * 0.05F);
        }

        // Притяжение сущностей
        AABB box = new AABB(center, center).inflate(PULL_RANGE);
        for (Entity e : level.getEntities((Entity) null, box, e -> e != hole)) {
            if (e instanceof Player pl && (pl.isCreative() || pl.isSpectator())) continue;
            Vec3 to = center.subtract(e.position().add(0, e.getBbHeight() / 2, 0));
            double d = to.length();
            if (d > PULL_RANGE) continue;
            if (d < r + 0.6) {
                // Поглощение
                if (e instanceof LivingEntity le) {
                    le.hurt(level.damageSources().fellOutOfWorld(), 6.0F);
                    DisasterUtil.push(le, to.normalize().scale(-0.05));
                } else {
                    e.discard();
                }
                continue;
            }
            double strength = Math.min(0.5, (0.9 + r * 0.4) / Math.max(4, d)) * (e instanceof FallingBlockEntity ? 2.2 : 1.0);
            Vec3 tangent = new Vec3(-to.z, 0, to.x).normalize().scale(strength * 0.4);
            DisasterUtil.push(e, to.normalize().scale(strength).add(tangent).add(0, 0.04, 0));
            if (e instanceof FallingBlockEntity fb) fb.time = 1;
            if (e.fallDistance > 0) e.resetFallDistance();
        }

        // Вырывание блоков
        int tries = 18 + (int) (r * 10);
        for (int i = 0; i < tries; i++) {
            double dist = 2 + Math.pow(random.nextDouble(), 1.7) * (10 + r * 5);
            Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            BlockPos p = BlockPos.containing(center.add(dir.scale(dist)));
            if (!level.isLoaded(p)) continue;
            BlockState st = level.getBlockState(p);
            if (st.isAir() || !DisasterUtil.breakable(level, p, st)) continue;
            if (dist < r * 1.8 || !st.getFluidState().isEmpty()) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            } else {
                Vec3 vel = center.subtract(Vec3.atCenterOf(p)).normalize().scale(0.6);
                DisasterUtil.launch(level, p, vel);
            }
        }
        // Частицы засасывания
        for (ServerPlayer p : players()) {
            if (p.distanceToSqr(center) > 300 * 300) continue;
            level.sendParticles(p, ParticleTypes.REVERSE_PORTAL, true, center.x, center.y, center.z, 30, r * 2.5, r * 2.5, r * 2.5, 0.2);
            level.sendParticles(p, ParticleTypes.PORTAL, true, center.x, center.y, center.z, 40, r * 3, r * 3, r * 3, 1.5);
            level.sendParticles(p, ParticleTypes.SQUID_INK, true, center.x, center.y, center.z, 6, r, r, r, 0.02);
        }
    }

    @Override
    public void end() {
        if (hole != null && hole.isAlive()) {
            level.explode(null, center.x, center.y, center.z, 7.0F, false, Level.ExplosionInteraction.TNT);
            for (ServerPlayer p : players()) {
                level.sendParticles(p, ParticleTypes.EXPLOSION_EMITTER, true, center.x, center.y, center.z, 4, 2, 2, 2, 0);
                level.sendParticles(p, ParticleTypes.END_ROD, true, center.x, center.y, center.z, 200, 1, 1, 1, 1.2);
            }
            hole.discard();
            DisasterUtil.broadcast(level, Component.literal("Чёрная дыра схлопнулась.").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    public int effectFlags(ServerPlayer player) {
        return center != null && player.distanceToSqr(center) < 150 * 150 ? ClientDisasterState.BLACK_HOLE : 0;
    }

    @Override
    public float shake(ServerPlayer player) {
        if (center == null) return 0;
        double d = Math.sqrt(player.distanceToSqr(center));
        return d < 90 ? (float) (0.5 * (1 - d / 90)) : 0F;
    }
}
