package com.vladdisasters.screamer;

import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.network.ModNetwork;
import com.vladdisasters.registry.ModStructures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Скример: картинка на весь экран + громкий резкий звук.
 * Вылезает у каждого игрока минимум раз в 5–15 минут,
 * а внутри Церкви имени Давида Икоева — ещё и с небольшим шансом в любой момент.
 */
public final class ScreamerManager {
    public static boolean enabled = true;
    public static int minMinutes = 5;
    public static int maxMinutes = 15;
    /** Шанс в церкви — проверяется раз в 2 секунды. */
    public static float churchChance = 0.025F;

    private static final int DURATION = 32; // 1.6 секунды
    private static final Map<UUID, Integer> TIMERS = new HashMap<>();
    private static int tick;

    public static void tick(ServerLevel level) {
        tick++;
        if (!enabled) return;
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p.isSpectator()) continue;
            int t = TIMERS.computeIfAbsent(p.getUUID(), k -> nextDelay(level));
            t--;
            if (t <= 0) {
                trigger(p);
                t = nextDelay(level);
            }
            TIMERS.put(p.getUUID(), t);

            if (tick % 40 == 0 && p.level() instanceof ServerLevel pl && pl == level) {
                boolean inChurch = level.structureManager().getStructureWithPieceAt(p.blockPosition(), ModStructures.CHURCHES).isValid();
                if (inChurch && level.random.nextFloat() < churchChance) {
                    trigger(p);
                }
            }
        }
    }

    private static int nextDelay(ServerLevel level) {
        int min = Math.max(1, minMinutes) * 60 * 20;
        int max = Math.max(min, maxMinutes * 60 * 20);
        return min + level.random.nextInt(max - min + 1);
    }

    public static void trigger(ServerPlayer p) {
        ModNetwork.send(p, new ModNetwork.ScreamerPayload(DURATION));
    }

    public static void triggerAll(ServerLevel level) {
        for (ServerPlayer p : DisasterUtil.players(level)) trigger(p);
    }

    public static void reset() {
        TIMERS.clear();
    }

    private ScreamerManager() {}
}
