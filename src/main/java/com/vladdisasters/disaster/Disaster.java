package com.vladdisasters.disaster;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Базовый класс бедствия. Живёт на сервере в Верхнем мире. */
public abstract class Disaster {
    public final DisasterType type;
    protected final ServerLevel level;
    protected final RandomSource random;
    /** Игрок, возле которого происходит бедствие (для локальных бедствий). */
    protected ServerPlayer target;
    /** Точка бедствия, запоминаем на случай если игрок вышел. */
    protected BlockPos origin;
    protected int age;
    protected int duration;
    protected boolean done;

    protected Disaster(DisasterType type, ServerLevel level, ServerPlayer target, int duration) {
        this.type = type;
        this.level = level;
        this.random = level.getRandom();
        this.target = target;
        this.origin = target != null ? target.blockPosition() : level.getSharedSpawnPos();
        this.duration = duration;
    }

    public final void baseTick() {
        age++;
        if (target != null && (target.isRemoved() || target.level() != level)) {
            List<ServerPlayer> ps = players();
            target = ps.isEmpty() ? null : ps.get(random.nextInt(ps.size()));
        }
        if (target != null) origin = target.blockPosition();
        tick();
    }

    public void start() {}

    public void tick() {}

    public void end() {}

    public boolean isFinished() {
        return done || age >= duration;
    }

    public int remainingTicks() {
        return Math.max(0, duration - age);
    }

    /** Флаги клиентских эффектов для конкретного игрока. */
    public int effectFlags(ServerPlayer player) {
        return 0;
    }

    /** Сила тряски камеры 0..1 для игрока. */
    public float shake(ServerPlayer player) {
        return 0F;
    }

    /** Следующее бедствие, которое начнётся сразу (ядерный взрыв → ядерная зима). */
    @Nullable
    public Disaster followUp() {
        return null;
    }

    protected List<ServerPlayer> players() {
        return DisasterUtil.players(level);
    }

    public ServerLevel level() {
        return level;
    }
}
