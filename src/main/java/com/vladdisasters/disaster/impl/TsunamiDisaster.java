package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.registry.ModBlocks;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Цунами: стена воды проходит через местность, сносит слабые блоки и уносит существ.
 * Используется особая «вода цунами», которая не растекается и сама исчезает — после волны воды не остаётся.
 */
public class TsunamiDisaster extends Disaster {
    private static final int HALF_WIDTH = 55;
    private static final int THICKNESS = 5;
    private static final int TRAVEL = 190;
    private static final int WAVE_HEIGHT = 15;

    private final Direction dir;
    private int front;         // пройденное волной расстояние
    private BlockPos start;    // линия старта (середина)
    private int baseLevel;     // базовая высота волны
    private final LongOpenHashSet placed = new LongOpenHashSet();

    public TsunamiDisaster(DisasterType type, ServerLevel level, ServerPlayer target) {
        super(type, level, target, 50 * 20);
        this.dir = Direction.Plane.HORIZONTAL.getRandomDirection(level.random);
    }

    @Override
    public void start() {
        // Волна приходит со стороны, противоположной направлению движения
        start = origin.relative(dir.getOpposite(), TRAVEL / 2);
        baseLevel = Math.max(level.getSeaLevel(), origin.getY());
        String from = switch (dir.getOpposite()) {
            case NORTH -> "севера";
            case SOUTH -> "юга";
            case EAST -> "востока";
            default -> "запада";
        };
        DisasterUtil.broadcast(level, Component.literal("🌊 Волна идёт с " + from + "! Высота около " + WAVE_HEIGHT + " блоков.").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));
    }

    @Override
    public void tick() {
        if (front < TRAVEL + THICKNESS && age % 2 == 0) {
            advance();
            front++;
        }
        if (front >= TRAVEL + THICKNESS && age > duration - 40) done = true;
        if (age % 10 == 0) {
            BlockPos mid = start.relative(dir, front);
            level.playSound(null, mid.getX(), mid.getY() + 8, mid.getZ(), SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.WEATHER, 8.0F, 0.5F);
            level.playSound(null, mid.getX(), mid.getY() + 8, mid.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.WEATHER, 6.0F, 0.6F);
        }
        pushEntities();
    }

    private BlockPos column(int along, int across) {
        Direction side = dir.getClockWise();
        return start.relative(dir, along).relative(side, across);
    }

    private void advance() {
        int newRow = front;
        int oldRow = front - THICKNESS;
        for (int a = -HALF_WIDTH; a <= HALF_WIDTH; a++) {
            // Убираем хвост волны
            if (oldRow >= 0) {
                BlockPos c = column(oldRow, a);
                if (level.isLoaded(c)) {
                    int top = baseLevel + WAVE_HEIGHT + 30;
                    for (int y = top; y >= baseLevel - 30; y--) {
                        BlockPos p = new BlockPos(c.getX(), y, c.getZ());
                        if (placed.remove(p.asLong()) && level.getBlockState(p).is(ModBlocks.TSUNAMI_WATER.get())) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
            // Новая передняя кромка
            if (newRow <= TRAVEL) {
                BlockPos c = column(newRow, a);
                if (!level.isLoaded(c)) continue;
                int ground = DisasterUtil.topYNoLeaves(level, c.getX(), c.getZ());
                // Края волны ниже середины — волна изогнута
                double edge = 1.0 - Math.pow(Math.abs(a) / (double) HALF_WIDTH, 3);
                int crest = (int) Math.max(ground + 3, baseLevel + WAVE_HEIGHT * edge);
                for (int y = ground + 1; y <= crest; y++) {
                    BlockPos p = new BlockPos(c.getX(), y, c.getZ());
                    BlockState st = level.getBlockState(p);
                    if (st.is(ModBlocks.TSUNAMI_WATER.get())) continue;
                    if (st.isAir() || DisasterUtil.weak(level, p, st)) {
                        if (!st.isAir() && random.nextInt(4) == 0) level.destroyBlock(p, false);
                        level.setBlock(p, ModBlocks.TSUNAMI_WATER.get().defaultBlockState(), 2);
                        placed.add(p.asLong());
                    }
                }
                // Брызги на гребне
                if (random.nextInt(6) == 0) {
                    level.sendParticles(ParticleTypes.SPLASH, c.getX() + 0.5, crest + 1, c.getZ() + 0.5, 10, 0.5, 0.3, 0.5, 0.2);
                    level.sendParticles(ParticleTypes.CLOUD, c.getX() + 0.5, crest + 1, c.getZ() + 0.5, 2, 0.5, 0.3, 0.5, 0.05);
                }
            }
        }
    }

    private void pushEntities() {
        BlockPos a = column(Math.max(0, front - THICKNESS - 2), -HALF_WIDTH);
        BlockPos b = column(Math.min(TRAVEL, front + 1), HALF_WIDTH);
        AABB box = new AABB(Vec3.atLowerCornerOf(a), Vec3.atLowerCornerOf(b)).inflate(1, 0, 1)
                .setMinY(baseLevel - 40).setMaxY(baseLevel + WAVE_HEIGHT + 30);
        Vec3 push = new Vec3(dir.getStepX(), 0, dir.getStepZ()).scale(0.55);
        for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            BlockState at = level.getBlockState(e.blockPosition());
            BlockState atHead = level.getBlockState(BlockPos.containing(e.getX(), e.getEyeY(), e.getZ()));
            boolean inWave = at.is(ModBlocks.TSUNAMI_WATER.get()) || atHead.is(ModBlocks.TSUNAMI_WATER.get());
            if (!inWave) continue;
            e.clearFire();
            DisasterUtil.push(e, push.add(0, 0.06, 0).subtract(e.getDeltaMovement().multiply(0.3, 0, 0.3)));
            if (e instanceof LivingEntity le && atHead.is(ModBlocks.TSUNAMI_WATER.get())) {
                le.setAirSupply(Math.max(-19, le.getAirSupply() - 6));
                if (le.getAirSupply() <= -19 && age % 20 == 0) le.hurt(level.damageSources().drown(), 2.0F);
            }
        }
    }

    @Override
    public void end() {
        // Гарантированная уборка: никакой воды после цунами
        for (long l : placed) {
            BlockPos p = BlockPos.of(l);
            if (level.isLoaded(p) && level.getBlockState(p).is(ModBlocks.TSUNAMI_WATER.get())) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        placed.clear();
    }

    @Override
    public float shake(ServerPlayer player) {
        BlockPos mid = start == null ? origin : start.relative(dir, front);
        double d = Math.sqrt(player.blockPosition().distSqr(mid));
        return d < 60 ? (float) (0.4 * (1 - d / 60)) : 0;
    }
}
