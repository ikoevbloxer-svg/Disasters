package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Землетрясение: тряска, трещины в земле, летящие блоки. */
public class EarthquakeDisaster extends Disaster {
    public EarthquakeDisaster(DisasterType type, ServerLevel level, ServerPlayer target) {
        super(type, level, target, 50 * 20);
    }

    private float intensity() {
        float t = (float) age / duration;
        // нарастает, пик в середине, затухает
        return (float) Math.sin(Math.PI * Math.min(1F, t)) * 0.85F + 0.15F;
    }

    @Override
    public void tick() {
        float in = intensity();
        if (age % 40 == 0) {
            for (ServerPlayer p : players()) {
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.QUAKE_RUMBLE.get(), SoundSource.AMBIENT, 2.0F, 0.8F + random.nextFloat() * 0.3F);
            }
        }
        for (ServerPlayer p : players()) {
            // Игроков подбрасывает
            if (p.onGround() && random.nextFloat() < 0.15F * in) {
                DisasterUtil.push(p, new Vec3((random.nextDouble() - 0.5) * 0.35 * in, 0.22 * in, (random.nextDouble() - 0.5) * 0.35 * in));
            }
            // Блоки срываются со своих мест
            if (age % 4 == 0) {
                for (int i = 0; i < 3; i++) {
                    BlockPos s = DisasterUtil.randomSurface(level, p.blockPosition(), 4, 30);
                    if (s == null) continue;
                    DisasterUtil.launch(level, s, new Vec3((random.nextDouble() - 0.5) * 0.3, 0.35 + random.nextDouble() * 0.4 * in, (random.nextDouble() - 0.5) * 0.3));
                }
            }
            // Трещины
            if (age % 35 == 0 && age > 60) fissure(p.blockPosition(), in);
        }
    }

    private void fissure(BlockPos around, float in) {
        BlockPos start = DisasterUtil.randomSurface(level, around, 6, 26);
        if (start == null) return;
        double ang = random.nextDouble() * Math.PI * 2;
        double dx = Math.cos(ang), dz = Math.sin(ang);
        int length = 10 + random.nextInt(14);
        int depth = 5 + random.nextInt((int) (10 * in) + 1);
        for (int i = 0; i < length; i++) {
            int x = start.getX() + (int) Math.round(dx * i + (random.nextDouble() - 0.5));
            int z = start.getZ() + (int) Math.round(dz * i + (random.nextDouble() - 0.5));
            BlockPos probe = new BlockPos(x, start.getY(), z);
            if (!level.isLoaded(probe)) continue;
            int top = DisasterUtil.topYNoLeaves(level, x, z);
            int width = (i > 2 && i < length - 3) ? 2 : 1;
            for (int w = 0; w < width; w++) {
                for (int y = top; y > top - depth && y > level.getMinBuildHeight() + 2; y--) {
                    BlockPos p = new BlockPos(x + (w == 1 ? (int) Math.round(-dz) : 0), y, z + (w == 1 ? (int) Math.round(dx) : 0));
                    BlockState st = level.getBlockState(p);
                    if (DisasterUtil.breakable(level, p, st) && st.getFluidState().isEmpty()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                // Иногда на дне — лава
                BlockPos bottom = new BlockPos(x, top - depth, z);
                if (random.nextInt(8) == 0 && DisasterUtil.breakable(level, bottom, level.getBlockState(bottom))) {
                    level.setBlock(bottom, Blocks.LAVA.defaultBlockState(), 3);
                }
            }
        }
        level.playSound(null, start, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.5F, 0.5F);
    }

    @Override
    public float shake(ServerPlayer player) {
        return intensity();
    }
}
