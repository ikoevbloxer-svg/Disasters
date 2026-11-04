package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.disaster.ImpactEffects;
import com.vladdisasters.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

/**
 * Взрыв атомной бомбы. Сначала падает боеголовка (видно след в небе), затем вспышка,
 * огромный кратер, ударная волна, грибовидное облако. После — ядерная зима на 30 минут.
 */
public class NukeDisaster extends Disaster {
    private static final int DROP_TICKS = 6 * 20;
    private BlockPos groundZero;

    public NukeDisaster(DisasterType type, ServerLevel level, ServerPlayer target) {
        super(type, level, target, DROP_TICKS + 30 * 20);
    }

    @Override
    public void start() {
        BlockPos gz = null;
        for (int i = 0; i < 10 && gz == null; i++) gz = DisasterUtil.randomSurface(level, origin, 75, 120);
        if (gz == null) gz = origin;
        groundZero = gz;
        DisasterUtil.broadcast(level, Component.literal("☢ Боеголовка в воздухе! Цель: " + gz.getX() + " " + gz.getZ())
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        DisasterUtil.soundAll(level, ModSounds.SIREN.get(), 2.0F, 0.7F);
    }

    @Override
    public void tick() {
        double x = groundZero.getX() + 0.5, z = groundZero.getZ() + 0.5;
        if (age < DROP_TICKS) {
            // След падающей боеголовки
            double h = (1.0 - (double) age / DROP_TICKS) * 160;
            for (ServerPlayer p : players()) {
                level.sendParticles(p, ParticleTypes.FLAME, true, x, groundZero.getY() + h, z, 6, 0.2, 0.6, 0.2, 0.01);
                level.sendParticles(p, ParticleTypes.LARGE_SMOKE, true, x, groundZero.getY() + h + 2, z, 4, 0.3, 1.0, 0.3, 0.01);
            }
            if (age % 20 == 0) {
                level.playSound(null, x, groundZero.getY() + h, z, ModSounds.METEOR_WHOOSH.get(), SoundSource.HOSTILE, 10F, 0.5F);
            }
            return;
        }
        if (age == DROP_TICKS) {
            ImpactEffects.bigImpact(level, groundZero, 34, 20, 85, 160F, false);
            DisasterUtil.titleAll(level, Component.literal("☢").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                    Component.literal("Ядерный удар").withStyle(ChatFormatting.RED), 60);
        }
        int t = age - DROP_TICKS;
        ImpactEffects.mushroomCloud(level, groundZero, t, 1.3F);
        // Вторичные подрывы вдоль ударной волны
        if (t < 60 && t % 6 == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 20 + t;
            level.explode(null, x + Math.cos(a) * r, groundZero.getY() + 2, z + Math.sin(a) * r, 4.0F, true, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    public float shake(ServerPlayer player) {
        if (groundZero == null || age < DROP_TICKS) return 0.05F;
        int t = age - DROP_TICKS;
        double d = Math.sqrt(player.blockPosition().distSqr(groundZero));
        float dist = d < 400 ? (float) (1 - d / 400) : 0;
        return dist * Math.max(0.2F, 1F - t / 200F);
    }

    @Override
    @Nullable
    public Disaster followUp() {
        if (groundZero == null || age < DROP_TICKS) return null;
        return new NuclearWinterDisaster(DisasterType.NUCLEAR_WINTER, level, target, groundZero);
    }
}
