package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.entity.MeteorEntity;
import com.vladdisasters.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Падение Гига-Астероида: огромная глыба пролетает над головой и врезается в землю.
 * Кратер ~60 блоков в диаметре, ударная волна, огненные обломки.
 */
public class GigaAsteroidDisaster extends Disaster {
    private static final int FLIGHT_TICKS = 12 * 20;
    private MeteorEntity rock;
    private BlockPos impact;
    private int impactAge = -1;

    public GigaAsteroidDisaster(DisasterType type, ServerLevel level, ServerPlayer target) {
        super(type, level, target, 60 * 20);
    }

    @Override
    public void start() {
        BlockPos imp = null;
        for (int i = 0; i < 10 && imp == null; i++) imp = DisasterUtil.randomSurface(level, origin, 70, 100);
        if (imp == null) imp = origin;
        impact = imp;
        // Летит прямо над игроком в сторону точки удара — видно всем
        Vec3 to = Vec3.atCenterOf(impact);
        Vec3 flat = new Vec3(impact.getX() - origin.getX(), 0, impact.getZ() - origin.getZ());
        Vec3 dir = flat.lengthSqr() < 1 ? new Vec3(1, 0, 0) : flat.normalize();
        double topY = Math.min(level.getMaxBuildHeight() + 60, impact.getY() + 230);
        Vec3 from = new Vec3(origin.getX() - dir.x * 40, topY, origin.getZ() - dir.z * 40);
        Vec3 vel = to.subtract(from).scale(1.0 / FLIGHT_TICKS);
        rock = new MeteorEntity(level, from, vel, 14.0F, MeteorEntity.KIND_GIGA);
        level.addFreshEntity(rock);
        DisasterUtil.broadcast(level, Component.literal("☄ Гига-Астероид входит в атмосферу! Место падения: " + impact.getX() + " " + impact.getZ())
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        DisasterUtil.soundAll(level, ModSounds.METEOR_WHOOSH.get(), 1.0F, 0.4F);
    }

    @Override
    public void tick() {
        if (impactAge < 0) {
            if (rock == null || rock.isRemoved()) {
                impactAge = age;
            } else if (age % 15 == 0) {
                for (ServerPlayer p : players()) {
                    p.playNotifySound(ModSounds.METEOR_WHOOSH.get(), SoundSource.MASTER, 0.7F + age / (float) FLIGHT_TICKS, 0.35F);
                }
            }
            if (age > FLIGHT_TICKS + 200) impactAge = age; // страховка
            return;
        }
        int t = age - impactAge;
        com.vladdisasters.disaster.ImpactEffects.mushroomCloud(level, impact, t, 0.9F);
        if (t > 30 * 20) done = true;
    }

    @Override
    public float shake(ServerPlayer player) {
        if (impactAge < 0) return Math.min(0.5F, age / (float) FLIGHT_TICKS * 0.5F);
        int t = age - impactAge;
        double d = Math.sqrt(player.blockPosition().distSqr(impact));
        float dist = d < 350 ? (float) (1 - d / 350) : 0;
        return dist * Math.max(0.15F, 1F - t / 160F);
    }
}
