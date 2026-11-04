package com.vladdisasters.world;

import com.mojang.serialization.MapCodec;
import com.vladdisasters.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Огромный вулкан (радиус ~70 блоков, высота ~80 блоков над землёй, кратер с лавой).
 * Центр — в углу стартового чанка, чтобы /locate и бедствие находили кратер точно.
 */
public class VolcanoStructure extends Structure {
    public static final MapCodec<VolcanoStructure> CODEC = simpleCodec(VolcanoStructure::new);

    public VolcanoStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMinBlockX(), z = cp.getMinBlockZ();
        // Средняя высота по кругу — чтобы вулкан не висел и не тонул
        int sum = 0, n = 0, min = Integer.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int h = ctx.chunkGenerator().getFirstOccupiedHeight(x + (int) (Math.cos(a) * 40), z + (int) (Math.sin(a) * 40),
                    Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            sum += h;
            n++;
            min = Math.min(min, h);
        }
        int center = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        int base = Math.min(center, sum / n);
        int sea = ctx.chunkGenerator().getSeaLevel();
        if (base < sea - 4) return Optional.empty(); // не в океане
        int radius = 64 + ctx.random().nextInt(10);
        int height = 72 + ctx.random().nextInt(18);
        BlockPos pos = new BlockPos(x, base, z);
        return Optional.of(new GenerationStub(pos, b -> b.addPiece(new VolcanoPiece(pos, radius, height, ctx.random().nextLong()))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.VOLCANO.get();
    }
}
