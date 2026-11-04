package com.vladdisasters.world;

import com.vladdisasters.VladDisasters;
import com.vladdisasters.entity.MamaVladEntity;
import com.vladdisasters.registry.ModEntities;
import com.vladdisasters.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/** Дом Мамы Влада и Церковь имени Давида Икоева. */
public class BuildingPiece extends StructurePiece {
    public static final String MAMA_HOUSE = "mama_house";
    public static final String CHURCH = "church";

    public static final ResourceKey<LootTable> LOOT_HOUSE = ResourceKey.create(Registries.LOOT_TABLE, VladDisasters.id("chests/mama_house"));
    public static final ResourceKey<LootTable> LOOT_CHURCH = ResourceKey.create(Registries.LOOT_TABLE, VladDisasters.id("chests/church"));

    private final String kind;
    private final BlockPos origin;
    private final boolean mamaHome;

    /** {ширина по X, длина по Z, высота} */
    public static int[] size(String kind) {
        return CHURCH.equals(kind) ? new int[]{15, 33, 32} : new int[]{13, 13, 14};
    }

    private static BoundingBox boxFor(String kind, BlockPos o) {
        int[] s = size(kind);
        return new BoundingBox(o.getX() - 1, o.getY() - 12, o.getZ() - 1, o.getX() + s[0], o.getY() + s[2], o.getZ() + s[1]);
    }

    public BuildingPiece(String kind, BlockPos origin, boolean mamaHome) {
        super(ModStructures.BUILDING_PIECE.get(), 0, boxFor(kind, origin));
        this.kind = kind;
        this.origin = origin;
        this.mamaHome = mamaHome;
    }

    public BuildingPiece(CompoundTag tag) {
        super(ModStructures.BUILDING_PIECE.get(), tag);
        this.kind = tag.getString("Kind");
        this.origin = new BlockPos(tag.getInt("OX"), tag.getInt("OY"), tag.getInt("OZ"));
        this.mamaHome = tag.getBoolean("Mama");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putString("Kind", kind);
        tag.putInt("OX", origin.getX());
        tag.putInt("OY", origin.getY());
        tag.putInt("OZ", origin.getZ());
        tag.putBoolean("Mama", mamaHome);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        Blueprint b = new Blueprint(level, origin, box);
        if (CHURCH.equals(kind)) buildChurch(b, level, box, random);
        else buildHouse(b, level, box, random);
    }

    // =====================================================================
    //  ДОМ МАМЫ ВЛАДА (13 x 11, высокий — мама большая)
    // =====================================================================
    private void buildHouse(Blueprint b, WorldGenLevel level, BoundingBox box, RandomSource random) {
        BlockState logs = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState wall = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState roof = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();

        // Расчистка и фундамент
        b.fill(-1, 1, -1, 13, 14, 12, Blocks.AIR.defaultBlockState());
        b.foundation(0, 0, 12, 10, -1, Blocks.COBBLESTONE.defaultBlockState(), 12);
        b.fill(0, 0, 0, 12, 0, 10, Blocks.SPRUCE_PLANKS.defaultBlockState());

        // Стены высотой 6
        b.hollow(0, 0, 0, 12, 7, 10, wall);
        b.fill(0, 0, 0, 12, 0, 10, Blocks.SPRUCE_PLANKS.defaultBlockState());
        for (int[] c : new int[][]{{0, 0}, {12, 0}, {0, 10}, {12, 10}}) b.fill(c[0], 1, c[1], c[0], 7, c[1], logs);
        // Окна
        for (int z : new int[]{3, 7}) {
            b.fill(0, 3, z, 0, 4, z, glass);
            b.fill(12, 3, z, 12, 4, z, glass);
        }
        b.fill(3, 3, 0, 4, 4, 0, glass);
        b.fill(8, 3, 0, 9, 4, 0, glass);
        b.fill(2, 3, 10, 3, 4, 10, glass);
        b.fill(9, 3, 10, 10, 4, 10, glass);
        // Дверь (двойная — мама широкая)
        b.door(5, 1, 10, Blocks.SPRUCE_DOOR, Direction.NORTH);
        b.door(6, 1, 10, Blocks.SPRUCE_DOOR, Direction.NORTH);
        b.fill(7, 1, 10, 7, 2, 10, Blocks.AIR.defaultBlockState());
        b.wallSign(4, 3, 11, Direction.SOUTH, "Дом", "Мамы Влада", "Без колбасы", "не входить!");

        // Двускатная крыша с напуском
        for (int k = 0; k <= 7; k++) {
            int y = 8 + k;
            b.fill(-1 + k, y, -1, 13 - k, y, 11, roof);
            if (k <= 6) {
                // фронтоны
                b.fill(k, y, 0, 12 - k, y, 0, wall);
                b.fill(k, y, 10, 12 - k, y, 10, wall);
            }
            if (k <= 5) b.fill(k + 1, y, 1, 11 - k, y, 9, Blocks.AIR.defaultBlockState());
        }

        // Интерьер
        b.fill(1, 0, 1, 11, 0, 9, Blocks.SPRUCE_PLANKS.defaultBlockState());
        b.fill(4, 1, 3, 8, 1, 7, Blocks.RED_CARPET.defaultBlockState());
        // Огромная кровать мамы
        b.bed(1, 1, 3, Blocks.RED_BED, Direction.NORTH);
        b.bed(2, 1, 3, Blocks.RED_BED, Direction.NORTH);
        b.bed(1, 1, 5, Blocks.ORANGE_BED, Direction.NORTH);
        b.bed(2, 1, 5, Blocks.ORANGE_BED, Direction.NORTH);
        // Кухня: печи, коптильня, кастрюля борща, холодильник
        b.set(11, 1, 1, Blocks.SMOKER.defaultBlockState().setValue(SmokerBlock.FACING, Direction.WEST));
        b.set(11, 1, 2, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.WEST));
        b.set(11, 1, 3, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        b.set(11, 1, 4, Blocks.CRAFTING_TABLE.defaultBlockState());
        b.fill(11, 1, 6, 11, 3, 6, Blocks.IRON_BLOCK.defaultBlockState()); // «холодильник»
        b.set(10, 1, 6, Blocks.IRON_TRAPDOOR.defaultBlockState());
        // Стол с едой
        b.set(6, 1, 5, Blocks.BARREL.defaultBlockState());
        b.set(6, 2, 5, Blocks.CAKE.defaultBlockState());
        b.set(5, 1, 5, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        b.set(7, 1, 5, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        // Телевизор
        b.fill(5, 2, 1, 7, 3, 1, Blocks.BLACK_CONCRETE.defaultBlockState());
        b.set(6, 1, 1, Blocks.NOTE_BLOCK.defaultBlockState());
        // Сундуки с колбасой
        BlockPos chest1 = b.at(1, 1, 8), chest2 = b.at(2, 1, 9);
        createChest(level, box, random, chest1, LOOT_HOUSE, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
        createChest(level, box, random, chest2, LOOT_HOUSE, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        // Свет
        b.lantern(3, 6, 5, true);
        b.lantern(9, 6, 5, true);
        b.lantern(6, 6, 8, true);
        b.set(6, 7, 5, Blocks.OAK_PLANKS.defaultBlockState());
        b.lantern(6, 6, 5, true);
        // Огородик с картошкой
        b.foundation(0, 11, 12, 12, -1, Blocks.DIRT.defaultBlockState(), 12);
        b.fill(0, 0, 11, 12, 0, 11, Blocks.GRASS_BLOCK.defaultBlockState());
        b.fill(0, 0, 12, 12, 0, 12, Blocks.FARMLAND.defaultBlockState());
        b.fill(0, 1, 12, 12, 1, 12, Blocks.POTATOES.defaultBlockState());

        // 15% — Мама дома (слабее, чем во время бедствия)
        if (mamaHome) {
            BlockPos spawn = b.at(6, 1, 4);
            if (box.isInside(spawn)) {
                MamaVladEntity mama = ModEntities.MAMA_VLAD.get().create(level.getLevel());
                if (mama != null) {
                    mama.setupVariant(MamaVladEntity.VARIANT_HOME);
                    mama.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, random.nextFloat() * 360F, 0);
                    mama.setPersistenceRequired();
                    level.addFreshEntityWithPassengers(mama);
                }
            }
        }
    }

    // =====================================================================
    //  ЦЕРКОВЬ ИМЕНИ ДАВИДА ИКОЕВА (неф 15 x 27 + колокольня)
    // =====================================================================
    private void buildChurch(Blueprint b, WorldGenLevel level, BoundingBox box, RandomSource random) {
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState pillar = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState roof = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();

        // Расчистка, фундамент
        b.fill(-1, 1, -1, 15, 31, 32, Blocks.AIR.defaultBlockState());
        b.foundation(0, 0, 14, 26, -1, Blocks.STONE_BRICKS.defaultBlockState(), 12);
        b.foundation(5, 27, 9, 31, -1, Blocks.STONE_BRICKS.defaultBlockState(), 12);

        // Неф
        b.hollow(0, 0, 0, 14, 10, 26, wall);
        b.fill(1, 0, 1, 13, 0, 25, floor);
        for (int x = 0; x <= 14; x++)
            for (int y = 1; y <= 9; y++)
                for (int z : new int[]{0, 26})
                    if (random.nextInt(6) == 0) b.set(x, y, z, mossy);
        for (int z = 0; z <= 26; z += 4) {
            b.fill(0, 1, z, 0, 10, z, pillar);
            b.fill(14, 1, z, 14, 10, z, pillar);
        }
        // Витражи
        for (int z = 2; z <= 24; z += 4) {
            BlockState sg = (z / 4) % 2 == 0 ? Blocks.YELLOW_STAINED_GLASS.defaultBlockState() : Blocks.ORANGE_STAINED_GLASS.defaultBlockState();
            b.fill(0, 3, z, 0, 7, z + 1, sg);
            b.fill(14, 3, z, 14, 7, z + 1, sg);
        }
        // Круглое окно над алтарём
        b.fill(6, 6, 0, 8, 8, 0, Blocks.PURPLE_STAINED_GLASS.defaultBlockState());
        b.set(7, 9, 0, Blocks.PURPLE_STAINED_GLASS.defaultBlockState());
        b.set(7, 5, 0, Blocks.PURPLE_STAINED_GLASS.defaultBlockState());

        // Крыша
        for (int k = 0; k <= 8; k++) {
            int y = 11 + k;
            if (-1 + k > 15 - k) break;
            b.fill(-1 + k, y, -1, 15 - k, y, 27, roof);
            if (k + 1 <= 13 - k) {
                b.fill(k + 1, y, 0, 13 - k, y, 0, wall);
                b.fill(k + 1, y, 26, 13 - k, y, 26, wall);
                b.fill(k + 1, y, 1, 13 - k, y, 25, Blocks.AIR.defaultBlockState());
            }
        }

        // Ковровая дорожка и скамьи
        b.fill(6, 1, 4, 8, 1, 25, Blocks.RED_CARPET.defaultBlockState());
        for (int z = 8; z <= 23; z += 3) {
            for (int x = 2; x <= 5; x++) b.stairs(x, 1, z, Blocks.DARK_OAK_STAIRS, Direction.SOUTH);
            for (int x = 9; x <= 12; x++) b.stairs(x, 1, z, Blocks.DARK_OAK_STAIRS, Direction.SOUTH);
        }

        // Алтарь
        b.fill(3, 0, 1, 11, 0, 4, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        b.fill(5, 1, 3, 9, 1, 3, Blocks.QUARTZ_BLOCK.defaultBlockState());
        b.candles(5, 2, 3, 3);
        b.candles(7, 2, 3, 4);
        b.candles(9, 2, 3, 3);
        b.set(7, 1, 6, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.SOUTH));
        b.wallSign(6, 1, 4, Direction.SOUTH, "Церковь", "имени", "Давида", "Икоева");
        b.wallSign(8, 1, 4, Direction.SOUTH, "Слава", "Давиду", "Икоеву!", "");

        // Статуя Давида Икоева
        b.fill(6, 1, 1, 8, 1, 1, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        b.set(6, 2, 1, Blocks.QUARTZ_PILLAR.defaultBlockState());
        b.set(8, 2, 1, Blocks.QUARTZ_PILLAR.defaultBlockState());
        b.set(6, 3, 1, Blocks.QUARTZ_PILLAR.defaultBlockState());
        b.set(8, 3, 1, Blocks.QUARTZ_PILLAR.defaultBlockState());
        b.fill(6, 4, 1, 8, 5, 1, Blocks.QUARTZ_BLOCK.defaultBlockState());
        b.set(5, 5, 1, Blocks.QUARTZ_BLOCK.defaultBlockState());
        b.set(9, 5, 1, Blocks.QUARTZ_BLOCK.defaultBlockState());
        b.set(4, 6, 1, Blocks.QUARTZ_BLOCK.defaultBlockState()); // руки воздеты к небу
        b.set(10, 6, 1, Blocks.QUARTZ_BLOCK.defaultBlockState());
        b.set(7, 6, 1, gold);                                    // золотая голова
        b.set(7, 7, 1, Blocks.GLOWSTONE.defaultBlockState());    // сияние
        b.set(6, 7, 1, Blocks.GOLD_BLOCK.defaultBlockState());
        b.set(8, 7, 1, Blocks.GOLD_BLOCK.defaultBlockState());

        // Сундук с реликвиями
        createChest(level, box, random, b.at(2, 1, 2), LOOT_CHURCH, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));

        // Люстры
        for (int z = 6; z <= 22; z += 8) {
            b.set(7, 10, z, Blocks.CHAIN.defaultBlockState());
            b.lantern(7, 9, z, true);
        }
        for (int z = 3; z <= 24; z += 6) {
            b.lantern(1, 1, z, false);
            b.lantern(13, 1, z, false);
        }

        // Колокольня у входа
        b.hollow(5, 0, 26, 9, 22, 31, wall);
        b.fill(6, 0, 27, 8, 0, 30, floor);
        b.fill(6, 1, 26, 8, 4, 26, Blocks.AIR.defaultBlockState()); // проход в неф
        b.fill(6, 1, 27, 8, 21, 30, Blocks.AIR.defaultBlockState());
        b.door(7, 1, 31, Blocks.DARK_OAK_DOOR, Direction.NORTH);
        b.wallSign(6, 3, 32, Direction.SOUTH, "Церковь", "имени Давида", "Икоева", "Вход свободный");
        b.wallSign(8, 3, 32, Direction.SOUTH, "Давид Икоев", "видит", "всё", "");
        // окна колокольни
        for (int y : new int[]{8, 14, 19}) {
            b.fill(5, y, 28, 5, y + 1, 29, Blocks.AIR.defaultBlockState());
            b.fill(9, y, 28, 9, y + 1, 29, Blocks.AIR.defaultBlockState());
            b.fill(7, y, 31, 7, y + 1, 31, Blocks.AIR.defaultBlockState());
        }
        // колокол
        b.fill(6, 21, 27, 8, 21, 30, wall);
        b.set(7, 20, 28, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, Direction.SOUTH));
        // шпиль
        for (int k = 0; k <= 2; k++) {
            b.fill(5 + k, 23 + k, 26 + k, 9 - k, 23 + k, 31 - k, roof);
        }
        b.fill(7, 26, 28, 7, 30, 28, gold);    // крест
        b.set(6, 29, 28, gold);
        b.set(8, 29, 28, gold);
    }
}
