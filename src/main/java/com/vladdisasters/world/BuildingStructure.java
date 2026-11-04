package com.vladdisasters.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vladdisasters.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Процедурные постройки: дом Мамы Влада и церковь имени Давида Икоева. */
public class BuildingStructure extends Structure {
    public static final MapCodec<BuildingStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("building").forGetter(s -> s.building)
    ).apply(i, BuildingStructure::new));

    public final String building;

    public BuildingStructure(StructureSettings settings, String building) {
        super(settings);
        this.building = building;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMinBlockX() + 2, z = cp.getMinBlockZ() + 2;
        int[] size = BuildingPiece.size(building);
        int w = size[0], l = size[1];
        int[][] pts = {{x, z}, {x + w - 1, z}, {x, z + l - 1}, {x + w - 1, z + l - 1}, {x + w / 2, z + l / 2}};
        int sum = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int[] pt : pts) {
            int surf = ctx.chunkGenerator().getFirstOccupiedHeight(pt[0], pt[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            int floor = ctx.chunkGenerator().getFirstOccupiedHeight(pt[0], pt[1], Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
            if (surf != floor) return Optional.empty(); // вода — не строим
            sum += surf;
            min = Math.min(min, surf);
            max = Math.max(max, surf);
        }
        if (max - min > 9) return Optional.empty(); // слишком крутой склон
        int y = sum / pts.length;
        BlockPos origin = new BlockPos(x, y, z);
        boolean mamaHome = ctx.random().nextFloat() < 0.15F;
        return Optional.of(new GenerationStub(origin, b -> b.addPiece(new BuildingPiece(building, origin, mamaHome))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.BUILDING.get();
    }
}
