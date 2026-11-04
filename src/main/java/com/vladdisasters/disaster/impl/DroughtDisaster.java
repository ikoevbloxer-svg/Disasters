package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.network.ClientDisasterState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Засуха: уровень воды опускается на 2 блока, биомы вокруг превращаются в пустыню,
 * трава высыхает в песок, растения гибнут. Игроки быстрее голодают на солнце.
 */
public class DroughtDisaster extends Disaster {
    private static final int MAX_RING = 6; // радиус в чанках (~96 блоков)
    private final Set<Long> converted = new HashSet<>();
    private Holder<Biome> desert;

    public DroughtDisaster(DisasterType type, ServerLevel level, ServerPlayer target) {
        super(type, level, target, 150 * 20);
    }

    @Override
    public void start() {
        level.setWeatherParameters(duration + 6000, 0, false, false);
        desert = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.DESERT);
    }

    @Override
    public void tick() {
        // Распространение пустыни кольцами вокруг игроков
        if (age % 25 == 0) {
            int ring = Math.min(MAX_RING, age / 200);
            for (ServerPlayer p : players()) {
                ChunkPos c = p.chunkPosition();
                List<LevelChunk> batch = new ArrayList<>();
                for (int dx = -ring; dx <= ring; dx++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        long key = ChunkPos.asLong(c.x + dx, c.z + dz);
                        if (converted.contains(key)) continue;
                        LevelChunk ch = level.getChunkSource().getChunkNow(c.x + dx, c.z + dz);
                        if (ch == null) continue;
                        converted.add(key);
                        dryChunk(ch);
                        batch.add(ch);
                        if (batch.size() >= 6) break;
                    }
                    if (batch.size() >= 6) break;
                }
                if (!batch.isEmpty()) {
                    level.getChunkSource().chunkMap.resendBiomesForChunks(new ArrayList<ChunkAccess>(batch));
                }
            }
        }
        // Жара: голод и иногда возгорания сухой травы
        for (ServerPlayer p : players()) {
            if (level.canSeeSky(p.blockPosition().above()) && level.isDay()) {
                p.causeFoodExhaustion(0.012F);
                if (age % 100 == 0) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, false));
            }
            if (age % 60 == 0 && random.nextInt(3) == 0) {
                BlockPos s = com.vladdisasters.disaster.DisasterUtil.randomSurface(level, p.blockPosition(), 10, 50);
                if (s != null && level.getBlockState(s.above()).isAir() && (level.getBlockState(s).is(BlockTags.LEAVES) || level.getBlockState(s).is(Blocks.DEAD_BUSH))) {
                    level.setBlock(s.above(), Blocks.FIRE.defaultBlockState(), 3);
                }
            }
        }
    }

    /** Превращает чанк в пустыню и опускает воду на 2 блока. */
    private void dryChunk(LevelChunk chunk) {
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> desert, level.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);

        int baseX = chunk.getPos().getMinBlockX(), baseZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = baseX + lx, z = baseZ + lz;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                p.set(x, top, z);
                BlockState st = level.getBlockState(p);

                // Вода: снимаем два верхних слоя
                if (!st.getFluidState().isEmpty() && st.is(Blocks.WATER)) {
                    for (int k = 0; k < 2; k++) {
                        p.set(x, top - k, z);
                        BlockState w = level.getBlockState(p);
                        if (w.is(Blocks.WATER) || w.is(Blocks.SEAGRASS) || w.is(Blocks.TALL_SEAGRASS) || w.is(Blocks.KELP) || w.is(Blocks.KELP_PLANT)) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                        }
                    }
                    continue;
                }
                // Растения гибнут
                if (st.is(BlockTags.LEAVES)) {
                    if (random.nextInt(3) != 0) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                    continue;
                }
                if (st.getBlock() instanceof CropBlock || st.is(BlockTags.FLOWERS) || st.is(Blocks.SHORT_GRASS)
                        || st.is(Blocks.TALL_GRASS) || st.is(Blocks.FERN) || st.is(Blocks.LARGE_FERN) || st.is(BlockTags.SAPLINGS)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                    p.move(0, -1, 0);
                    st = level.getBlockState(p);
                }
                // Земля → песок
                if (st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.DIRT) || st.is(Blocks.PODZOL) || st.is(Blocks.MYCELIUM)
                        || st.is(Blocks.FARMLAND) || st.is(Blocks.COARSE_DIRT) || st.is(Blocks.ROOTED_DIRT) || st.is(Blocks.MUD)) {
                    BlockPos here = p.immutable();
                    level.setBlock(here, Blocks.SAND.defaultBlockState(), 2 | 16);
                    BlockPos below = here.below();
                    if (level.getBlockState(below).is(Blocks.DIRT)) level.setBlock(below, Blocks.SANDSTONE.defaultBlockState(), 2 | 16);
                    if (random.nextInt(40) == 0 && level.getBlockState(here.above()).isAir()) {
                        level.setBlock(here.above(), random.nextInt(3) == 0 ? Blocks.CACTUS.defaultBlockState() : Blocks.DEAD_BUSH.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    @Override
    public int effectFlags(ServerPlayer player) {
        return ClientDisasterState.DROUGHT;
    }
}
