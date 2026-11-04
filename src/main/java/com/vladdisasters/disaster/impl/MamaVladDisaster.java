package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.entity.MamaVladEntity;
import com.vladdisasters.registry.ModEntities;
import com.vladdisasters.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Пришествие Мамки Влада: огромное существо приходит и пожирает мир кусками 7x7. */
public class MamaVladDisaster extends Disaster {
    private MamaVladEntity mama;

    public MamaVladDisaster(DisasterType type, ServerLevel level, ServerPlayer target) {
        super(type, level, target, 150 * 20);
    }

    @Override
    public void start() {
        BlockPos spawn = null;
        for (int i = 0; i < 12 && spawn == null; i++) {
            spawn = DisasterUtil.randomSurface(level, origin, 22, 34);
        }
        if (spawn == null) spawn = origin;
        mama = ModEntities.MAMA_VLAD.get().create(level);
        if (mama == null) {
            done = true;
            return;
        }
        mama.setupVariant(MamaVladEntity.VARIANT_DISASTER);
        mama.setLifeTicks(duration + 40);
        mama.moveTo(spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5, random.nextFloat() * 360F, 0);
        if (target != null) mama.setTarget(target);
        level.addFreshEntity(mama);
        DisasterUtil.broadcast(level, Component.literal("Земля дрожит... Мамка Влада проголодалась и идёт сюда!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        DisasterUtil.soundAll(level, ModSounds.MAMA_SCREAM.get(), 1.5F, 0.9F);
    }

    @Override
    public void tick() {
        if (mama == null || !mama.isAlive()) {
            if (mama != null && mama.isDeadOrDying()) {
                DisasterUtil.broadcast(level, Component.literal("Мамка Влада повержена! Можно выдохнуть.").withStyle(ChatFormatting.GREEN));
            }
            done = true;
        }
    }

    @Override
    public void end() {
        if (mama != null && mama.isAlive()) mama.leave();
    }

    @Override
    public float shake(ServerPlayer player) {
        if (mama == null || !mama.isAlive()) return 0;
        double d = player.distanceTo(mama);
        return d < 40 ? (float) (0.35 * (1 - d / 40)) : 0;
    }
}
