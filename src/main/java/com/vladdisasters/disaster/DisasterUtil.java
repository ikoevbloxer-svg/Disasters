package com.vladdisasters.disaster;

import com.vladdisasters.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class DisasterUtil {
    /** Блоки прочнее этого (обсидиан, бункер) бедствия не ломают. */
    public static final float MAX_BREAKABLE_HARDNESS = 49.0F;

    public static List<ServerPlayer> players(ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isSpectator()) out.add(p);
        }
        return out;
    }

    public static boolean loaded(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos);
    }

    /** Блок можно разрушить бедствием. */
    public static boolean breakable(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL_FRAME) || state.is(Blocks.BARRIER)
                || state.is(Blocks.COMMAND_BLOCK) || state.is(Blocks.STRUCTURE_BLOCK)) return false;
        float h = state.getDestroySpeed(level, pos);
        return h >= 0 && h < MAX_BREAKABLE_HARDNESS;
    }

    /** Слабые блоки: листва, растения, стекло, шерсть и т.п. */
    public static boolean weak(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (state.is(ModBlocks.TSUNAMI_WATER.get())) return false;
        if (!state.getFluidState().isEmpty() && state.getFluidState().isSource()) return false;
        if (state.is(BlockTags.LEAVES) || state.canBeReplaced()) return true;
        float h = state.getDestroySpeed(level, pos);
        return h >= 0 && h <= 0.9F;
    }

    /** Y самого верхнего твёрдого (не воздушного) блока. */
    public static int topY(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
    }

    public static int topYNoLeaves(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    /** Безопасно выбрать поверхность в радиусе от точки. null — если чанк не загружен. */
    @Nullable
    public static BlockPos randomSurface(ServerLevel level, BlockPos center, int minR, int maxR) {
        double ang = level.random.nextDouble() * Math.PI * 2;
        double r = minR + level.random.nextDouble() * (maxR - minR);
        int x = center.getX() + (int) Math.round(Math.cos(ang) * r);
        int z = center.getZ() + (int) Math.round(Math.sin(ang) * r);
        BlockPos probe = new BlockPos(x, center.getY(), z);
        if (!level.isLoaded(probe)) return null;
        return new BlockPos(x, topY(level, x, z), z);
    }

    /** Подбросить блок в воздух как падающий блок. */
    @Nullable
    public static FallingBlockEntity launch(ServerLevel level, BlockPos pos, Vec3 velocity) {
        BlockState state = level.getBlockState(pos);
        if (!breakable(level, pos, state) || state.hasBlockEntity() || !state.getFluidState().isEmpty()) return null;
        if (state.is(BlockTags.LEAVES) || state.canBeReplaced()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return null;
        }
        FallingBlockEntity fb = FallingBlockEntity.fall(level, pos, state);
        fb.dropItem = false;
        fb.setDeltaMovement(velocity);
        fb.hurtMarked = true;
        return fb;
    }

    public static void push(Entity e, Vec3 add) {
        e.setDeltaMovement(e.getDeltaMovement().add(add));
        e.hurtMarked = true;
        if (add.y > 0.1) e.resetFallDistance();
    }

    public static void title(ServerPlayer p, Component title, Component subtitle, int stay) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(5, stay, 15));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    public static void titleAll(ServerLevel level, Component title, Component subtitle, int stay) {
        for (ServerPlayer p : players(level)) title(p, title, subtitle, stay);
    }

    public static void soundAll(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        for (ServerPlayer p : players(level)) p.playNotifySound(sound, SoundSource.MASTER, volume, pitch);
    }

    public static void broadcast(ServerLevel level, Component msg) {
        level.getServer().getPlayerList().broadcastSystemMessage(msg, false);
    }

    public static double horizontalDist(BlockPos a, double x, double z) {
        double dx = a.getX() + 0.5 - x, dz = a.getZ() + 0.5 - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private DisasterUtil() {}
}
