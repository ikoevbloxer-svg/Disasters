package com.vladdisasters.world;

import com.vladdisasters.VladDisasters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/**
 * Бункер ~150 блоков от точки спавна. Стены из обсидиана — бедствия его не ломают.
 * Вход — бетонная будка с лестницей вниз.
 */
public final class BunkerBuilder {
    public static final ResourceKey<LootTable> LOOT_BUNKER = ResourceKey.create(Registries.LOOT_TABLE, VladDisasters.id("chests/bunker"));
    private static final int DEPTH = 18;

    /** Подобрать место и построить. Возвращает позицию входа. */
    @Nullable
    public static BlockPos buildNearSpawn(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        double start = level.random.nextDouble() * Math.PI * 2;
        for (int attempt = 0; attempt < 10; attempt++) {
            double a = start + attempt * (Math.PI * 2 / 10);
            int dist = 140 + level.random.nextInt(21);
            int x = spawn.getX() + (int) Math.round(Math.cos(a) * dist);
            int z = spawn.getZ() + (int) Math.round(Math.sin(a) * dist);
            // прогружаем чанки под бункером
            for (int cx = (x - 10) >> 4; cx <= (x + 10) >> 4; cx++)
                for (int cz = (z - 4) >> 4; cz <= (z + 16) >> 4; cz++)
                    level.getChunk(cx, cz);
            int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
            BlockState top = level.getBlockState(new BlockPos(x, surface, z));
            BlockState below = level.getBlockState(new BlockPos(x, surface - 1, z));
            if (!top.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) continue; // вода
            if (surface - DEPTH < level.getMinBuildHeight() + 8) continue;
            BlockPos entrance = new BlockPos(x, surface, z);
            build(level, entrance);
            return entrance;
        }
        return null;
    }

    public static void build(ServerLevel level, BlockPos entrance) {
        Blueprint b = new Blueprint(level, entrance, null);
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState hull = Blocks.OBSIDIAN.defaultBlockState();
        BlockState inner = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState hutWall = Blocks.GRAY_CONCRETE.defaultBlockState();
        int f = -DEPTH; // уровень пола (относительно входа)

        // ---------- Будка на поверхности ----------
        b.fill(-3, 0, -3, 3, 6, 4, air);
        b.foundation(-2, -1, 2, 2, -1, Blocks.STONE_BRICKS.defaultBlockState(), 8);
        b.fill(-2, -1, -1, 2, -1, 2, Blocks.STONE_BRICKS.defaultBlockState());
        b.hollow(-2, -1, -1, 2, 3, 2, hutWall);
        b.fill(-2, 3, -1, 2, 3, 2, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        b.door(0, 0, 2, Blocks.IRON_DOOR, Direction.NORTH);
        b.set(1, 1, 3, Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACING, Direction.SOUTH));
        b.set(1, 1, 1, Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACING, Direction.NORTH));
        b.wallSign(-1, 1, 3, Direction.SOUTH, "БУНКЕР №1", "Укрытие", "от бедствий", "Кнопка справа");
        b.lantern(-1, 2, 1, true);
        b.set(0, 2, 0, Blocks.AIR.defaultBlockState());

        // ---------- Главный зал ----------
        b.fill(-8, f - 1, -1, 8, f + 7, 14, hull);
        b.fill(-7, f, 0, 7, f + 6, 13, inner);
        b.fill(-6, f + 1, 1, 6, f + 5, 12, air);
        b.fill(-6, f, 1, 6, f, 12, Blocks.POLISHED_ANDESITE.defaultBlockState());
        b.fill(-1, f + 1, 0, 1, f + 5, 0, air);   // проход от шахты

        // ---------- Шахта с лестницей (после зала, чтобы не замуровать) ----------
        for (int y = f + 6; y <= -1; y++) {
            b.fill(-1, y, -1, 1, y, 1, hull);
            b.set(0, y, 0, air);
        }
        // лестнице нужна опора на северной стене внутри будки
        b.fill(0, 0, -1, 0, 1, -1, hutWall);
        for (int y = f + 1; y <= 1; y++) b.ladder(0, y, 0, Direction.SOUTH);

        // свет в потолке
        for (int x = -5; x <= 5; x += 5)
            for (int z = 2; z <= 11; z += 3)
                b.set(x, f + 6, z, Blocks.SEA_LANTERN.defaultBlockState());

        // Спальня (восток)
        for (int z = 2; z <= 11; z += 3) b.bed(6, f + 1, z + 1, Blocks.WHITE_BED, Direction.NORTH);
        for (int z = 2; z <= 11; z += 3) b.bed(4, f + 1, z + 1, Blocks.GRAY_BED, Direction.NORTH);

        // Склад (запад)
        for (int z = 2; z <= 10; z += 2) {
            BlockPos p = b.at(-6, f + 1, z);
            level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST), 2);
            RandomizableContainer.setBlockEntityLootTable(level, level.random, p, LOOT_BUNKER);
            b.set(-6, f + 2, z, Blocks.BARREL.defaultBlockState());
        }
        // Рабочая зона
        b.set(-2, f + 1, 12, Blocks.CRAFTING_TABLE.defaultBlockState());
        b.set(-1, f + 1, 12, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH));
        b.set(0, f + 1, 12, Blocks.SMOKER.defaultBlockState().setValue(SmokerBlock.FACING, Direction.NORTH));
        b.set(1, f + 1, 12, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        b.set(2, f + 1, 12, Blocks.BREWING_STAND.defaultBlockState());
        b.set(-3, f + 1, 12, Blocks.ANVIL.defaultBlockState());
        // Стол
        b.fill(-1, f + 1, 6, 1, f + 1, 7, Blocks.SPRUCE_PLANKS.defaultBlockState());
        b.set(0, f + 2, 6, Blocks.LANTERN.defaultBlockState());
        // Генераторная: «лампы» и рычаги для атмосферы
        b.fill(-7, f + 2, 12, -7, f + 3, 12, Blocks.REDSTONE_LAMP.defaultBlockState());
        b.wallSign(-1, f + 3, 12, Direction.NORTH, "Бункер выдержит", "атомный взрыв", "и Маму Влада", "(наверное)");
        b.wallSign(3, f + 3, 1, Direction.SOUTH, "Правила:", "1. Не шуметь", "2. Колбасу", "делить поровну");
    }

    private BunkerBuilder() {}
}
