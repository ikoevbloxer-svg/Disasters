package com.vladdisasters.disaster;

import com.vladdisasters.disaster.impl.*;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Все бедствия мода. */
public enum DisasterType {
    // ---- Обычные ----
    BOMBARDMENT("Бомбардировка", false, ChatFormatting.RED, BombardmentDisaster::new),
    EARTHQUAKE("Землетрясение", false, ChatFormatting.GOLD, EarthquakeDisaster::new),
    METEOR_SHOWER("Метеоритный дождь", false, ChatFormatting.GOLD, MeteorShowerDisaster::new),
    MAMA_VLAD("Пришествие Мамки Влада", false, ChatFormatting.DARK_RED, MamaVladDisaster::new),
    ACID_RAIN("Кислотный дождь", false, ChatFormatting.GREEN, AcidRainDisaster::new),
    VOLCANO("Извержение вулкана", false, ChatFormatting.RED, VolcanoDisaster::create),
    BLIZZARD("Суперметель", false, ChatFormatting.AQUA, BlizzardDisaster::new),
    TORNADO("Торнадо", false, ChatFormatting.GRAY, TornadoDisaster::new),
    DROUGHT("Засуха", false, ChatFormatting.YELLOW, DroughtDisaster::new),
    // ---- Редкие (с предупреждением) ----
    TSUNAMI("Цунами", true, ChatFormatting.BLUE, TsunamiDisaster::new),
    NUKE("Взрыв атомной бомбы", true, ChatFormatting.DARK_RED, NukeDisaster::new),
    GIGA_ASTEROID("Падение Гига-Астероида", true, ChatFormatting.DARK_PURPLE, GigaAsteroidDisaster::new),
    BLACK_HOLE("Чёрная дыра", true, ChatFormatting.DARK_PURPLE, BlackHoleDisaster::new),
    // ---- Последствие, само не выпадает ----
    NUCLEAR_WINTER("Ядерная зима", true, ChatFormatting.DARK_GRAY, NuclearWinterDisaster::new);

    public interface Factory {
        @Nullable
        Disaster create(DisasterType type, ServerLevel level, ServerPlayer target);
    }

    public final String displayName;
    public final boolean rare;
    public final ChatFormatting color;
    private final Factory factory;

    DisasterType(String displayName, boolean rare, ChatFormatting color, Factory factory) {
        this.displayName = displayName;
        this.rare = rare;
        this.color = color;
        this.factory = factory;
    }

    @Nullable
    public Disaster create(ServerLevel level, ServerPlayer target) {
        return factory.create(this, level, target);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean randomlySelectable() {
        return this != NUCLEAR_WINTER;
    }

    public static List<DisasterType> common() {
        return Arrays.stream(values()).filter(t -> !t.rare).toList();
    }

    public static List<DisasterType> rareOnes() {
        return Arrays.stream(values()).filter(t -> t.rare && t.randomlySelectable()).toList();
    }

    @Nullable
    public static DisasterType byId(String id) {
        for (DisasterType t : values()) if (t.id().equalsIgnoreCase(id)) return t;
        return null;
    }

    /** Можно ли запустить два бедствия одновременно (двойное бедствие). */
    public static boolean compatible(DisasterType a, DisasterType b) {
        if (a == b) return false;
        if (pair(a, b, DROUGHT, ACID_RAIN)) return false;
        if (pair(a, b, DROUGHT, BLIZZARD)) return false;
        if (pair(a, b, BLIZZARD, ACID_RAIN)) return false;
        return true;
    }

    private static boolean pair(DisasterType a, DisasterType b, DisasterType x, DisasterType y) {
        return (a == x && b == y) || (a == y && b == x);
    }
}
