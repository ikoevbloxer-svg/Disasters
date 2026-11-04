package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.DisasterManager;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.network.ClientDisasterState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Ядерная зима — 30 минут после атомного взрыва.
 * Серое небо и туман, снег и холод по всему миру, вода замерзает, у эпицентра — радиация.
 * Пока идёт зима, новые бедствия не начинаются.
 */
public class NuclearWinterDisaster extends BlizzardDisaster {
    private static final int DURATION = 30 * 60 * 20;
    private final BlockPos groundZero;

    public NuclearWinterDisaster(DisasterType type, ServerLevel level, ServerPlayer target) {
        this(type, level, target, target != null ? target.blockPosition() : level.getSharedSpawnPos());
    }

    public NuclearWinterDisaster(DisasterType type, ServerLevel level, ServerPlayer target, BlockPos groundZero) {
        super(type, level, target, DURATION);
        this.groundZero = groundZero;
        this.airTemp = -15F;
        this.freezeRadius = 24;
        this.coldRate = 0.35F;
    }

    @Override
    public void start() {
        level.setWeatherParameters(0, duration + 200, true, false);
        DisasterUtil.titleAll(level, Component.literal("Ядерная зима").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD),
                Component.literal("30 минут холода и пепла").withStyle(ChatFormatting.GRAY), 70);
    }

    @Override
    public void tick() {
        // Холод мягче, чем в метели, но держится долго
        super.tick();
        if (age % 100 == 0) airTemp = Math.max(-30F, airTemp); // не уходим ниже −30
        for (ServerPlayer p : players()) {
            // Радиоактивный пепел
            if (age % 4 == 0) {
                level.sendParticles(p, ParticleTypes.WHITE_ASH, false, p.getX(), p.getY() + 4, p.getZ(), 20, 10, 4, 10, 0.01);
            }
            // Радиация в зоне 100 блоков от эпицентра
            if (age % 40 == 0 && !p.isCreative()) {
                double d = Math.sqrt(p.blockPosition().distSqr(groundZero));
                if (d < 100) {
                    p.addEffect(new MobEffectInstance(MobEffects.POISON, 60, d < 45 ? 1 : 0, false, true));
                    p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));
                    if (d < 45) p.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, true));
                    p.displayClientMessage(Component.literal("☢ РАДИАЦИЯ! Уходи от эпицентра!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
                }
            }
        }
        // Напоминание раз в 5 минут
        if (age > 0 && age % (5 * 60 * 20) == 0) {
            DisasterUtil.broadcast(level, Component.literal("❄ До конца ядерной зимы: " + DisasterManager.formatTime(remainingTicks())).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public int effectFlags(ServerPlayer player) {
        return ClientDisasterState.WINTER;
    }

    @Override
    public float shake(ServerPlayer player) {
        return 0F;
    }
}
