package com.vladdisasters.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * Помощник для строительства по локальным координатам от точки origin.
 * Если задан box — ставит только блоки внутри него (для генерации по чанкам).
 */
public class Blueprint {
    public final LevelAccessor level;
    public final BlockPos origin;
    @Nullable
    public final BoundingBox box;

    public Blueprint(LevelAccessor level, BlockPos origin, @Nullable BoundingBox box) {
        this.level = level;
        this.origin = origin;
        this.box = box;
    }

    public BlockPos at(int x, int y, int z) {
        return origin.offset(x, y, z);
    }

    public boolean inside(BlockPos p) {
        return box == null || box.isInside(p);
    }

    public void set(int x, int y, int z, BlockState s) {
        BlockPos p = at(x, y, z);
        if (inside(p)) level.setBlock(p, s, 2);
    }

    public BlockState get(int x, int y, int z) {
        return level.getBlockState(at(x, y, z));
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, s);
    }

    /** Пустая коробка: стены, пол и потолок из s, внутри воздух. */
    public void hollow(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        fill(x1, y1, z1, x2, y2, z2, s);
        fill(x1 + 1, y1 + 1, z1 + 1, x2 - 1, y2 - 1, z2 - 1, Blocks.AIR.defaultBlockState());
    }

    /** Фундамент: заполняет пустоту под постройкой вниз до земли. */
    public void foundation(int x1, int z1, int x2, int z2, int topY, BlockState s, int maxDepth) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = topY; y > topY - maxDepth; y--) {
                    BlockPos p = at(x, y, z);
                    if (!inside(p)) continue;
                    BlockState cur = level.getBlockState(p);
                    if (y < topY && !cur.isAir() && cur.getFluidState().isEmpty() && !cur.canBeReplaced()) break;
                    level.setBlock(p, s, 2);
                }
            }
        }
    }

    public void door(int x, int y, int z, Block door, Direction facing) {
        BlockState lower = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        set(x, y, z, lower);
        set(x, y + 1, z, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Кровать: ноги в (x,y,z), голова в сторону facing. */
    public void bed(int x, int y, int z, Block bed, Direction facing) {
        BlockState foot = bed.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.FOOT);
        set(x, y, z, foot);
        set(x + facing.getStepX(), y, z + facing.getStepZ(), foot.setValue(BedBlock.PART, BedPart.HEAD));
    }

    public void stairs(int x, int y, int z, Block stairs, Direction facing) {
        set(x, y, z, stairs.defaultBlockState().setValue(StairBlock.FACING, facing));
    }

    public void lantern(int x, int y, int z, boolean hanging) {
        set(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, hanging));
    }

    public void candles(int x, int y, int z, int count) {
        set(x, y, z, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, count).setValue(CandleBlock.LIT, true));
    }

    /** Табличка на стене. facing — куда смотрит лицевая сторона. */
    public void wallSign(int x, int y, int z, Direction facing, String... lines) {
        BlockPos p = at(x, y, z);
        if (!inside(p)) return;
        level.setBlock(p, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), 2);
        BlockEntity be = level.getBlockEntity(p);
        if (be instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            for (int i = 0; i < Math.min(4, lines.length); i++) text = text.setMessage(i, Component.literal(lines[i]));
            sign.setText(text, true);
        }
    }

    public void ladder(int x, int y, int z, Direction facing) {
        set(x, y, z, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing));
    }
}
