package com.vladdisasters.world;

import com.vladdisasters.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Процедурный конус вулкана. Строится по кусочку в каждом чанке. */
public class VolcanoPiece extends StructurePiece {
    private final int cx, cy, cz, radius, height;
    private final long seed;

    public VolcanoPiece(BlockPos center, int radius, int height, long seed) {
        super(ModStructures.VOLCANO_PIECE.get(), 0, new BoundingBox(
                center.getX() - radius - 6, center.getY() - 64, center.getZ() - radius - 6,
                center.getX() + radius + 6, center.getY() + height + 4, center.getZ() + radius + 6));
        this.cx = center.getX();
        this.cy = center.getY();
        this.cz = center.getZ();
        this.radius = radius;
        this.height = height;
        this.seed = seed;
    }

    public VolcanoPiece(CompoundTag tag) {
        super(ModStructures.VOLCANO_PIECE.get(), tag);
        this.cx = tag.getInt("CX");
        this.cy = tag.getInt("CY");
        this.cz = tag.getInt("CZ");
        this.radius = tag.getInt("R");
        this.height = tag.getInt("H");
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("CX", cx);
        tag.putInt("CY", cy);
        tag.putInt("CZ", cz);
        tag.putInt("R", radius);
        tag.putInt("H", height);
        tag.putLong("Seed", seed);
    }

    /** Детерминированный шум (одинаковый в любом чанке). */
    private double noise(double x, double z) {
        double s = (seed & 0xFFFF) * 0.001;
        return Math.sin(x * 0.11 + s) * Math.cos(z * 0.13 - s) * 0.5
                + Math.sin((x + z) * 0.047 + s * 3) * 0.35
                + Math.sin(x * 0.31 - z * 0.27 + s) * 0.15;
    }

    private static long hash(int x, int y, int z, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h ^= (h >>> 31);
        h *= 0x94D049BB133111EBL;
        return h ^ (h >>> 29);
    }

    private BlockState rock(int x, int y, int z, boolean surface) {
        int r = (int) Math.floorMod(hash(x, y, z, seed), 100L);
        if (surface) {
            if (r < 30) return Blocks.BASALT.defaultBlockState();
            if (r < 52) return Blocks.SMOOTH_BASALT.defaultBlockState();
            if (r < 72) return Blocks.BLACKSTONE.defaultBlockState();
            if (r < 84) return Blocks.TUFF.defaultBlockState();
            if (r < 92) return Blocks.MAGMA_BLOCK.defaultBlockState();
            return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }
        if (r < 55) return Blocks.STONE.defaultBlockState();
        if (r < 80) return Blocks.BLACKSTONE.defaultBlockState();
        if (r < 95) return Blocks.DEEPSLATE.defaultBlockState();
        return Blocks.MAGMA_BLOCK.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int craterR = Math.max(9, radius / 6);
        int peak = cy + height;
        int craterFloor = peak - 16;
        int lavaTop = peak - 7;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                double dx = x - cx, dz = z - cz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                double wobble = noise(x, z) * 7;
                double effR = radius + wobble;
                if (dist > effR) continue;

                int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                double t = 1.0 - dist / effR;              // 0 у подножия, 1 в центре
                double profile = Math.pow(t, 1.45);       // вогнутый склон
                int top = cy + (int) Math.round(height * profile + noise(x * 2.1, z * 1.7) * 2.5 * t);
                boolean inCrater = dist < craterR + noise(z, x) * 1.5;
                if (inCrater) top = craterFloor;
                if (dist < craterR + 3 && !inCrater) top = Math.min(top, peak); // кромка кратера

                // Насыпаем гору
                int from = Math.max(Math.min(ground, cy - 4), cy - 63);
                for (int y = from; y <= top; y++) {
                    p.set(x, y, z);
                    if (!box.isInside(p)) continue;
                    boolean surface = y > top - 3;
                    level.setBlock(p, rock(x, y, z, surface), 2);
                }
                // Срезаем всё, что было над склоном (деревья, трава)
                if (top >= ground - 2) {
                    for (int y = top + 1; y <= Math.max(ground + 14, top + 1); y++) {
                        p.set(x, y, z);
                        if (!box.isInside(p)) continue;
                        if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                // Кратер: лавовое озеро
                if (inCrater) {
                    for (int y = craterFloor + 1; y <= lavaTop; y++) {
                        p.set(x, y, z);
                        if (box.isInside(p)) level.setBlock(p, Blocks.LAVA.defaultBlockState(), 2);
                    }
                }
                // Жерло: лавовая труба до глубины
                if (dist < 3.2) {
                    for (int y = cy - 20; y <= craterFloor; y++) {
                        p.set(x, y, z);
                        if (box.isInside(p)) level.setBlock(p, Blocks.LAVA.defaultBlockState(), 2);
                    }
                }
                // Застывшие лавовые потоки на склонах (магмовые «реки»)
                if (!inCrater && dist > craterR + 2) {
                    double ang = Math.atan2(dz, dx);
                    double stripe = Math.sin(ang * 5 + (seed & 7)) + noise(x * 0.5, z * 0.5) * 0.6;
                    if (stripe > 0.93) {
                        p.set(x, top, z);
                        if (box.isInside(p)) level.setBlock(p, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
                    }
                }
                // У подножия — пепел (гравий и грубая земля)
                if (t < 0.12) {
                    p.set(x, top, z);
                    if (box.isInside(p)) {
                        long r = Math.floorMod(hash(x, 0, z, seed), 3L);
                        level.setBlock(p, r == 0 ? Blocks.GRAVEL.defaultBlockState() : r == 1 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.TUFF.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
}
