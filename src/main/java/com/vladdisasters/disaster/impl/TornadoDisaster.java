package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Торнадо: вращающаяся воронка, засасывает существ и вырывает блоки. */
public class TornadoDisaster extends Disaster {
    private static final int HEIGHT = 55;
    private double x, z, groundY;
    private double vx, vz;

    public TornadoDisaster(DisasterType type, ServerLevel level, ServerPlayer target) {
        super(type, level, target, 75 * 20);
    }

    @Override
    public void start() {
        double ang = random.nextDouble() * Math.PI * 2;
        x = origin.getX() + Math.cos(ang) * 55;
        z = origin.getZ() + Math.sin(ang) * 55;
        groundY = origin.getY();
        level.setWeatherParameters(0, duration + 100, true, true);
    }

    @Override
    public void end() {
        level.setWeatherParameters(6000, 0, false, false);
    }

    private double radiusAt(double h) {
        return 2.5 + h * 0.22; // узкая внизу, широкая вверху
    }

    @Override
    public void tick() {
        // Движение: медленно идёт к игроку, но петляет
        if (target != null) {
            double dx = target.getX() - x, dz = target.getZ() - z;
            double d = Math.max(1, Math.sqrt(dx * dx + dz * dz));
            vx = vx * 0.96 + dx / d * 0.012 + (random.nextDouble() - 0.5) * 0.04;
            vz = vz * 0.96 + dz / d * 0.012 + (random.nextDouble() - 0.5) * 0.04;
        }
        double sp = Math.sqrt(vx * vx + vz * vz);
        if (sp > 0.35) {
            vx *= 0.35 / sp;
            vz *= 0.35 / sp;
        }
        x += vx;
        z += vz;
        BlockPos col = BlockPos.containing(x, groundY, z);
        if (level.isLoaded(col)) groundY = DisasterUtil.topYNoLeaves(level, col.getX(), col.getZ()) + 1;

        // Видимая воронка из частиц
        double spin = age * 0.45;
        for (ServerPlayer p : players()) {
            if (p.distanceToSqr(x, groundY, z) > 220 * 220) continue;
            for (int i = 0; i < 26; i++) {
                double h = random.nextDouble() * HEIGHT;
                double r = radiusAt(h) * (0.85 + random.nextDouble() * 0.3);
                double a = spin + random.nextDouble() * Math.PI * 2;
                level.sendParticles(p, i % 3 == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.CLOUD, true,
                        x + Math.cos(a) * r, groundY + h, z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
            BlockState dust = level.getBlockState(BlockPos.containing(x, groundY - 1, z));
            if (!dust.isAir()) {
                level.sendParticles(p, new BlockParticleOption(ParticleTypes.BLOCK, dust), true, x, groundY + 0.5, z, 15, 3, 0.5, 3, 0.2);
            }
        }
        if (age % 30 == 0) {
            level.playSound(null, x, groundY + 5, z, ModSounds.TORNADO_WIND.get(), SoundSource.WEATHER, 6.0F, 0.8F + random.nextFloat() * 0.2F);
        }

        // Засасываем сущности
        AABB box = new AABB(x - 28, groundY - 6, z - 28, x + 28, groundY + HEIGHT, z + 28);
        for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof Player pl && (pl.isCreative() || pl.isSpectator())))) {
            double dx = x - e.getX(), dz = z - e.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 28) continue;
            double h = e.getY() - groundY;
            double core = radiusAt(Math.max(0, h));
            Vec3 tangent = new Vec3(-dz, 0, dx).normalize();
            Vec3 inward = new Vec3(dx, 0, dz).normalize();
            double pull = 0.045 * (1.2 - dist / 28);
            Vec3 add = inward.scale(pull).add(tangent.scale(pull * 1.6));
            if (dist < core + 2) {
                add = add.add(0, 0.16 + random.nextDouble() * 0.06, 0).add(tangent.scale(0.15));
                if (h > HEIGHT * 0.75) add = add.add(inward.scale(-0.6)).add(0, -0.1, 0); // выбрасывает наверху
            }
            DisasterUtil.push(e, add);
            if (e instanceof FallingBlockEntity fb) fb.time = 1; // не даём упасть «как обычно» слишком рано
        }

        // Вырываем блоки
        if (age % 2 == 0) {
            for (int i = 0; i < 9; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = random.nextDouble() * 6.5;
                int bx = (int) Math.floor(x + Math.cos(a) * r), bz = (int) Math.floor(z + Math.sin(a) * r);
                BlockPos probe = new BlockPos(bx, (int) groundY, bz);
                if (!level.isLoaded(probe)) continue;
                int top = DisasterUtil.topY(level, bx, bz);
                BlockPos p = new BlockPos(bx, top, bz);
                BlockState st = level.getBlockState(p);
                if (st.isAir() || st.getDestroySpeed(level, p) > 3.0F) continue;
                if (!st.getFluidState().isEmpty()) continue;
                Vec3 tan = new Vec3(-(z - bz), 0, x - bx).normalize();
                DisasterUtil.launch(level, p, tan.scale(0.4).add(0, 0.7 + random.nextDouble() * 0.4, 0));
            }
        }
    }

    @Override
    public float shake(ServerPlayer player) {
        double d = Math.sqrt(player.distanceToSqr(x, groundY, z));
        return d < 70 ? (float) (0.5 * (1 - d / 70)) : 0F;
    }

    @SuppressWarnings("unused")
    private static boolean isAir(BlockState s) {
        return s.isAir() || s.is(Blocks.CAVE_AIR);
    }
}
