package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.entity.MeteorEntity;
import com.vladdisasters.network.ClientDisasterState;
import com.vladdisasters.registry.ModSounds;
import com.vladdisasters.registry.ModStructures;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Извержение вулкана. Вулканы генерируются заранее вместе с миром (структура vladdisasters:volcano).
 * Бедствие находит ближайший вулкан и заставляет его извергаться.
 */
public class VolcanoDisaster extends Disaster {
    private static final int SEARCH_CHUNKS = 48;
    private final BlockPos crater;
    private final int cx, cz;

    private VolcanoDisaster(DisasterType type, ServerLevel level, ServerPlayer target, BlockPos crater) {
        super(type, level, target, 120 * 20);
        this.crater = crater;
        this.cx = crater.getX() >> 4;
        this.cz = crater.getZ() >> 4;
    }

    @Nullable
    public static Disaster create(DisasterType type, ServerLevel level, ServerPlayer target) {
        BlockPos found = level.findNearestMapStructure(ModStructures.VOLCANOES, target.blockPosition(), SEARCH_CHUNKS, false);
        if (found == null) return null;
        // Центр вулкана совпадает с углом стартового чанка структуры
        int x = found.getX(), z = found.getZ();
        level.getChunk(x >> 4, z >> 4); // прогрузить
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        // Ищем лаву в кратере
        BlockPos best = new BlockPos(x, top, z);
        for (int dx = -8; dx <= 8; dx += 2) {
            for (int dz = -8; dz <= 8; dz += 2) {
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + dx, z + dz);
                BlockPos p = new BlockPos(x + dx, h - 1, z + dz);
                if (level.getBlockState(p).is(Blocks.LAVA) && h > best.getY() - 6) {
                    best = p.above();
                }
            }
        }
        return new VolcanoDisaster(type, level, target, best);
    }

    @Override
    public void start() {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) level.setChunkForced(cx + dx, cz + dz, true);
        DisasterUtil.broadcast(level, Component.literal("🌋 Вулкан проснулся! Координаты кратера: " + crater.getX() + " " + crater.getY() + " " + crater.getZ())
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        for (ServerPlayer p : players()) {
            if (p.blockPosition().closerThan(crater, 400)) {
                p.playNotifySound(SoundEvents.GENERIC_EXPLODE.value(), SoundSource.MASTER, 1.5F, 0.3F);
                p.playNotifySound(ModSounds.QUAKE_RUMBLE.get(), SoundSource.MASTER, 1.5F, 0.6F);
            }
        }
    }

    @Override
    public void end() {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) level.setChunkForced(cx + dx, cz + dz, false);
    }

    @Override
    public void tick() {
        double cxw = crater.getX() + 0.5, cy = crater.getY() + 1, czw = crater.getZ() + 0.5;
        float phase = (float) age / duration;
        float power = phase < 0.15F ? phase / 0.15F : phase > 0.85F ? (1 - phase) / 0.15F : 1F;

        // Столб дыма и пепла
        if (age % 2 == 0) {
            for (ServerPlayer p : players()) {
                if (!p.blockPosition().closerThan(crater, 500)) continue;
                level.sendParticles(p, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, cxw, cy + 2, czw, 12, 3, 4, 3, 0.08);
                level.sendParticles(p, ParticleTypes.LARGE_SMOKE, true, cxw, cy + 10, czw, 10, 4, 6, 4, 0.1);
                level.sendParticles(p, ParticleTypes.LAVA, true, cxw, cy, czw, 6, 3, 1, 3, 0.5);
                if (age % 20 == 0) level.sendParticles(p, ParticleTypes.EXPLOSION_EMITTER, true, cxw, cy + 2, czw, 1, 1, 1, 1, 0);
            }
        }
        // Лавовые бомбы
        if (age % Math.max(2, (int) (6 - 4 * power)) == 0) {
            double ang = random.nextDouble() * Math.PI * 2;
            double sp = 0.4 + random.nextDouble() * 1.1;
            Vec3 vel = new Vec3(Math.cos(ang) * sp, 1.3 + random.nextDouble() * 1.2, Math.sin(ang) * sp);
            MeteorEntity bomb = new MeteorEntity(level, new Vec3(cxw, cy + 1, czw), vel, 0.6F + random.nextFloat() * 0.9F, MeteorEntity.KIND_LAVA_BOMB);
            level.addFreshEntity(bomb);
        }
        // Гул
        if (age % 50 == 0) {
            level.playSound(null, cxw, cy, czw, ModSounds.QUAKE_RUMBLE.get(), SoundSource.AMBIENT, 8.0F, 0.5F);
            level.playSound(null, cxw, cy, czw, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.AMBIENT, 8.0F, 0.4F);
        }
        // Переливание лавы через край — потоки текут по склонам
        if (age == 30 * 20 || age == 55 * 20 || age == 80 * 20) {
            int flows = 3 + random.nextInt(3);
            for (int i = 0; i < flows; i++) {
                double ang = random.nextDouble() * Math.PI * 2;
                for (int r = 8; r <= 16; r++) {
                    int x = (int) Math.round(cxw + Math.cos(ang) * r), z = (int) Math.round(czw + Math.sin(ang) * r);
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    BlockPos p = new BlockPos(x, y, z);
                    if (y < crater.getY() - 2 && level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.LAVA.defaultBlockState(), 3);
                        break;
                    }
                }
            }
        }
        // Пепел падает на игроков рядом: замедление
        if (age % 40 == 0) {
            for (ServerPlayer p : players()) {
                if (p.blockPosition().closerThan(crater, 120) && level.canSeeSky(p.blockPosition().above())) {
                    p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false));
                    if (p.blockPosition().closerThan(crater, 40)) p.hurt(level.damageSources().inFire(), 1.0F);
                }
            }
        }
    }

    @Override
    public int effectFlags(ServerPlayer player) {
        return player.blockPosition().closerThan(crater, 350) ? ClientDisasterState.ASH : 0;
    }

    @Override
    public float shake(ServerPlayer player) {
        double d = Math.sqrt(player.blockPosition().distSqr(crater));
        return d < 200 ? (float) (0.6 * (1 - d / 200)) : 0F;
    }
}
