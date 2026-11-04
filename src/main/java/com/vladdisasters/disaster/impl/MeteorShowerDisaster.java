package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.entity.MeteorEntity;
import com.vladdisasters.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Метеоритный дождь: огненные камни падают с неба под углом. */
public class MeteorShowerDisaster extends Disaster {
    public MeteorShowerDisaster(DisasterType type, ServerLevel level, ServerPlayer target) {
        super(type, level, target, 60 * 20);
    }

    @Override
    public void tick() {
        if (age % 14 != 0 || age > duration - 80) return;
        for (ServerPlayer p : players()) {
            BlockPos impact = DisasterUtil.randomSurface(level, p.blockPosition(), 0, 45);
            if (impact == null) continue;
            spawnMeteor(level, Vec3.atCenterOf(impact), 0.8F + random.nextFloat() * 1.6F, MeteorEntity.KIND_METEOR);
        }
    }

    /** Запустить метеор, который упадёт в точку impact примерно через 2.5–4 секунды. */
    public static MeteorEntity spawnMeteor(ServerLevel level, Vec3 impact, float size, int kind) {
        double ang = level.random.nextDouble() * Math.PI * 2;
        double horiz = 30 + level.random.nextDouble() * 30;
        double height = 70 + level.random.nextDouble() * 30;
        Vec3 from = impact.add(Math.cos(ang) * horiz, height, Math.sin(ang) * horiz);
        int ticks = 50 + level.random.nextInt(30);
        Vec3 vel = impact.subtract(from).scale(1.0 / ticks);
        MeteorEntity m = new MeteorEntity(level, from, vel, size, kind);
        level.addFreshEntity(m);
        level.playSound(null, from.x, from.y - 30, from.z, ModSounds.METEOR_WHOOSH.get(), SoundSource.HOSTILE, 3.0F, 0.8F + level.random.nextFloat() * 0.4F);
        return m;
    }

    @Override
    public float shake(ServerPlayer player) {
        return 0.1F;
    }
}
