package com.vladdisasters.registry;

import com.vladdisasters.VladDisasters;
import com.vladdisasters.world.BuildingPiece;
import com.vladdisasters.world.BuildingStructure;
import com.vladdisasters.world.VolcanoPiece;
import com.vladdisasters.world.VolcanoStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, VladDisasters.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, VladDisasters.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<VolcanoStructure>> VOLCANO =
            STRUCTURE_TYPES.register("volcano", () -> (StructureType<VolcanoStructure>) () -> VolcanoStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<BuildingStructure>> BUILDING =
            STRUCTURE_TYPES.register("building", () -> (StructureType<BuildingStructure>) () -> BuildingStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> VOLCANO_PIECE =
            PIECES.register("volcano", () -> (StructurePieceType.ContextlessType) tag -> new VolcanoPiece(tag));
    public static final DeferredHolder<StructurePieceType, StructurePieceType> BUILDING_PIECE =
            PIECES.register("building", () -> (StructurePieceType.ContextlessType) tag -> new BuildingPiece(tag));

    /** Теги структур, используются для поиска вулкана и проверки «игрок в церкви». */
    public static final TagKey<Structure> VOLCANOES = TagKey.create(Registries.STRUCTURE, VladDisasters.id("volcanoes"));
    public static final TagKey<Structure> CHURCHES = TagKey.create(Registries.STRUCTURE, VladDisasters.id("churches"));

    private ModStructures() {}
}
