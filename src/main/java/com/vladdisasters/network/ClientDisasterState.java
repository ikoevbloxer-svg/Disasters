package com.vladdisasters.network;

/**
 * Состояние эффектов на клиенте. Класс без клиентских импортов,
 * поэтому его безопасно трогать из общего кода сетевых обработчиков.
 */
public final class ClientDisasterState {
    public static final int ACID = 1;
    public static final int BLIZZARD = 2;
    public static final int WINTER = 4;
    public static final int ASH = 8;
    public static final int DROUGHT = 16;
    public static final int BLACK_HOLE = 32;

    public static volatile int flags;
    public static volatile float shake;
    public static volatile int screamerTicks;
    public static volatile boolean screamerSoundPending;
    public static volatile int flashTicks;
    public static volatile int flashMax = 1;

    public static boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public static void reset() {
        flags = 0;
        shake = 0;
        screamerTicks = 0;
        flashTicks = 0;
        screamerSoundPending = false;
    }

    private ClientDisasterState() {}
}
