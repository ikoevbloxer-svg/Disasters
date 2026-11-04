package com.vladdisasters.disaster.impl;

import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.PrimedTnt;

import java.util.ArrayList;
import java.util.List;

/** Бомбардировка: с неба падает динамит и взрывается при касании земли. */
public class BombardmentDisaster extends Disaster {
    private final List<PrimedTnt> bombs = new ArrayList<>();

    public BombardmentDisaster(DisasterType type, ServerLevel level, ServerPlayer target) {
        super(type, level, target, 60 * 20);
    }

    @Override
    public void start() {
        DisasterUtil.soundAll(level, ModSounds.SIREN.get(), 1.5F, 0.9F);
    }

    @Override
    public void tick() {
        if (age % 9 == 0 && age < duration - 60) {
            for (ServerPlayer p : players()) {
                BlockPos s = DisasterUtil.randomSurface(level, p.blockPosition(), 3, 42);
                if (s == null) continue;
                double y = Math.max(s.getY(), p.getY()) + 55 + random.nextInt(20);
                PrimedTnt tnt = new PrimedTnt(level, s.getX() + 0.5, y, s.getZ() + 0.5, null);
                tnt.setFuse(200);
                tnt.setDeltaMovement((random.nextDouble() - 0.5) * 0.2, -1.2, (random.nextDouble() - 0.5) * 0.2);
                level.addFreshEntity(tnt);
                bombs.add(tnt);
                level.playSound(null, s.getX(), s.getY() + 20, s.getZ(), ModSounds.METEOR_WHOOSH.get(), SoundSource.HOSTILE, 2.0F, 1.4F);
            }
        }
        // Взрыв при ударе о землю/воду
        bombs.removeIf(t -> {
            if (t.isRemoved()) return true;
            if (t.onGround() || t.isInWater() || t.horizontalCollision) {
                t.setFuse(1);
                return true;
            }
            return false;
        });
    }

    @Override
    public float shake(ServerPlayer player) {
        return 0.15F;
    }
}
