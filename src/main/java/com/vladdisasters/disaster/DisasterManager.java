package com.vladdisasters.disaster;

import com.vladdisasters.VladDisasters;
import com.vladdisasters.network.ModNetwork;
import com.vladdisasters.registry.ModItems;
import com.vladdisasters.registry.ModSounds;
import com.vladdisasters.screamer.ScreamerManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Мозг мода: решает, когда и какое бедствие начнётся.
 * Бедствия НЕ накладываются друг на друга: новое начинается только когда закончилось предыдущее.
 * Исключение — «Двойное бедствие», которое всегда объявляется заранее.
 */
@EventBusSubscriber(modid = VladDisasters.MODID)
public final class DisasterManager {
    // Настройки (меняются командой /disasters)
    public static int minIntervalSec = 8 * 60;
    public static int maxIntervalSec = 15 * 60;
    public static boolean autoEnabled = true;
    public static float rareChance = 0.15F;
    public static float doubleChance = 0.08F;

    private static final int RARE_WARNING_TICKS = 30 * 20;
    private static final int DOUBLE_WARNING_TICKS = 20 * 20;

    private static final List<Disaster> ACTIVE = new ArrayList<>();
    private static final List<DisasterType> PENDING = new ArrayList<>();
    private static int warningTicks;
    private static int warningTotal;
    private static int cooldown = 5 * 60 * 20; // первое бедствие через ~5 минут
    private static int ticker;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level == null) return;
        ticker++;
        BlockJobs.tick(level);
        ScreamerManager.tick(level);
        tickDisasters(level);
        if (ticker % 10 == 0) syncEffects(level);
        if (ticker % 20 == 0) identifierHud(level);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (Disaster d : ACTIVE) {
            try {
                d.end();
            } catch (Exception e) {
                VladDisasters.LOGGER.error("Ошибка при остановке бедствия", e);
            }
        }
        ACTIVE.clear();
        PENDING.clear();
        BlockJobs.clear();
        cooldown = 5 * 60 * 20;
    }

    private static void tickDisasters(ServerLevel level) {
        if (!ACTIVE.isEmpty()) {
            for (Disaster d : new ArrayList<>(ACTIVE)) {
                try {
                    d.baseTick();
                } catch (Exception e) {
                    VladDisasters.LOGGER.error("Бедствие {} упало, останавливаю", d.type, e);
                    d.done = true;
                }
                if (d.isFinished()) {
                    finish(level, d);
                }
            }
            if (ACTIVE.isEmpty() && PENDING.isEmpty()) {
                cooldown = nextCooldown(level.random);
            }
            return;
        }

        if (!PENDING.isEmpty()) {
            warningTicks--;
            if (warningTicks % 20 == 0 && warningTicks > 0) {
                int sec = warningTicks / 20;
                if (sec % 10 == 0 || sec <= 5) {
                    for (ServerPlayer p : DisasterUtil.players(level)) {
                        p.playNotifySound(sec <= 5 ? ModSounds.ALARM.get() : ModSounds.SIREN.get(),
                                net.minecraft.sounds.SoundSource.MASTER, sec <= 5 ? 1.0F : 1.6F, 1.0F);
                    }
                }
            }
            if (warningTicks <= 0) {
                List<DisasterType> types = new ArrayList<>(PENDING);
                PENDING.clear();
                for (DisasterType t : types) startNow(level, t, false);
                if (ACTIVE.isEmpty()) cooldown = 60 * 20;
            }
            return;
        }

        if (!autoEnabled) return;
        if (DisasterUtil.players(level).isEmpty()) return;
        if (--cooldown <= 0) rollNext(level);
    }

    private static void finish(ServerLevel level, Disaster d) {
        try {
            d.end();
        } catch (Exception e) {
            VladDisasters.LOGGER.error("Ошибка при завершении бедствия", e);
        }
        ACTIVE.remove(d);
        Disaster next = null;
        try {
            next = d.followUp();
        } catch (Exception e) {
            VladDisasters.LOGGER.error("Ошибка последствия", e);
        }
        if (next != null) {
            begin(level, next, false);
        } else {
            DisasterUtil.broadcast(level, Component.literal("✔ Бедствие «" + d.type.displayName + "» закончилось.").withStyle(ChatFormatting.GREEN));
        }
    }

    private static int nextCooldown(RandomSource r) {
        int min = Math.max(10, minIntervalSec), max = Math.max(min, maxIntervalSec);
        return (min + r.nextInt(max - min + 1)) * 20;
    }

    /** Случайный выбор следующего бедствия. */
    private static void rollNext(ServerLevel level) {
        RandomSource r = level.random;
        float roll = r.nextFloat();
        if (roll < doubleChance) {
            List<DisasterType> pool = new ArrayList<>(DisasterType.common());
            DisasterType a = pool.get(r.nextInt(pool.size()));
            pool.removeIf(t -> !DisasterType.compatible(a, t) || t == DisasterType.VOLCANO);
            if (a != DisasterType.VOLCANO && !pool.isEmpty()) {
                DisasterType b = pool.get(r.nextInt(pool.size()));
                announceDouble(level, a, b);
                return;
            }
        }
        if (roll < doubleChance + rareChance) {
            List<DisasterType> rare = DisasterType.rareOnes();
            announceRare(level, rare.get(r.nextInt(rare.size())));
            return;
        }
        List<DisasterType> common = new ArrayList<>(DisasterType.common());
        // Несколько попыток: вулкан может не найтись поблизости
        for (int i = 0; i < 6 && !common.isEmpty(); i++) {
            DisasterType t = common.get(r.nextInt(common.size()));
            if (startNow(level, t, true)) return;
            common.remove(t);
        }
        cooldown = 60 * 20;
    }

    private static void announceRare(ServerLevel level, DisasterType t) {
        PENDING.clear();
        PENDING.add(t);
        warningTicks = warningTotal = RARE_WARNING_TICKS;
        String hint = switch (t) {
            case TSUNAMI -> "Уходите на возвышенность!";
            case NUKE -> "Немедленно ищите бункер!";
            case GIGA_ASTEROID -> "Объект размером с гору летит к Земле!";
            case BLACK_HOLE -> "Гравитационная аномалия! Держитесь подальше!";
            default -> "Приготовьтесь!";
        };
        DisasterUtil.titleAll(level, Component.literal("⚠ ВНИМАНИЕ ⚠").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                Component.literal(t.displayName + " через 30 секунд!").withStyle(t.color), 80);
        DisasterUtil.broadcast(level, Component.literal("☢ ПРЕДУПРЕЖДЕНИЕ: ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                .append(Component.literal(t.displayName).withStyle(t.color, ChatFormatting.BOLD))
                .append(Component.literal(" через 30 секунд. " + hint).withStyle(ChatFormatting.YELLOW)));
        DisasterUtil.soundAll(level, ModSounds.SIREN.get(), 2.0F, 1.0F);
    }

    private static void announceDouble(ServerLevel level, DisasterType a, DisasterType b) {
        PENDING.clear();
        PENDING.add(a);
        PENDING.add(b);
        warningTicks = warningTotal = DOUBLE_WARNING_TICKS;
        DisasterUtil.titleAll(level, Component.literal("☠ ДВОЙНОЕ БЕДСТВИЕ ☠").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                Component.literal(a.displayName + " + " + b.displayName).withStyle(ChatFormatting.GOLD), 80);
        DisasterUtil.broadcast(level, Component.literal("☠ ДВОЙНОЕ БЕДСТВИЕ через 20 секунд: ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                .append(Component.literal(a.displayName).withStyle(a.color))
                .append(Component.literal(" и ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(b.displayName).withStyle(b.color)));
        DisasterUtil.soundAll(level, ModSounds.SIREN.get(), 2.0F, 0.8F);
    }

    /** Немедленно запустить бедствие. */
    public static boolean startNow(ServerLevel level, DisasterType type, boolean announce) {
        List<ServerPlayer> ps = DisasterUtil.players(level);
        if (ps.isEmpty()) return false;
        ServerPlayer target = ps.get(level.random.nextInt(ps.size()));
        Disaster d;
        try {
            d = type.create(level, target);
        } catch (Exception e) {
            VladDisasters.LOGGER.error("Не удалось создать бедствие {}", type, e);
            return false;
        }
        if (d == null) return false;
        begin(level, d, announce || !type.rare);
        return true;
    }

    private static void begin(ServerLevel level, Disaster d, boolean title) {
        ACTIVE.add(d);
        try {
            d.start();
        } catch (Exception e) {
            VladDisasters.LOGGER.error("Ошибка старта бедствия {}", d.type, e);
            ACTIVE.remove(d);
            return;
        }
        MutableComponent name = Component.literal(d.type.displayName).withStyle(d.type.color, ChatFormatting.BOLD);
        if (title) {
            DisasterUtil.titleAll(level, name, Component.literal("Бедствие началось!").withStyle(ChatFormatting.GRAY), 50);
        }
        DisasterUtil.broadcast(level, Component.literal("⚠ Началось бедствие: ").withStyle(ChatFormatting.RED).append(name));
        DisasterUtil.soundAll(level, SoundEvents.ELDER_GUARDIAN_CURSE, 0.7F, 0.6F);
    }

    // ---------------------- API для команд ----------------------

    public static boolean forceStart(ServerLevel level, DisasterType type, boolean withWarning) {
        stopAll(level);
        if (withWarning && type.rare && type.randomlySelectable()) {
            announceRare(level, type);
            return true;
        }
        return startNow(level, type, true);
    }

    public static boolean forceDouble(ServerLevel level, DisasterType a, DisasterType b) {
        if (!DisasterType.compatible(a, b)) return false;
        stopAll(level);
        announceDouble(level, a, b);
        return true;
    }

    public static void stopAll(ServerLevel level) {
        for (Disaster d : new ArrayList<>(ACTIVE)) {
            try {
                d.end();
            } catch (Exception ignored) {
            }
        }
        ACTIVE.clear();
        PENDING.clear();
        cooldown = nextCooldown(level.random);
        syncEffects(level);
    }

    public static void setCooldownSeconds(int sec) {
        cooldown = Math.max(1, sec * 20);
    }

    public static int cooldownTicks() {
        return cooldown;
    }

    public static List<Disaster> active() {
        return ACTIVE;
    }

    public static List<DisasterType> pending() {
        return PENDING;
    }

    public static int warningTicks() {
        return warningTicks;
    }

    @Nullable
    public static Disaster activeOf(DisasterType t) {
        for (Disaster d : ACTIVE) if (d.type == t) return d;
        return null;
    }

    // ---------------------- Клиентские эффекты и HUD ----------------------

    private static void syncEffects(ServerLevel level) {
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            int flags = 0;
            float shake = 0;
            if (p.level() == level) {
                for (Disaster d : ACTIVE) {
                    flags |= d.effectFlags(p);
                    shake = Math.max(shake, d.shake(p));
                }
            }
            ModNetwork.send(p, new ModNetwork.EffectsPayload(flags, Math.min(1F, shake)));
        }
    }

    private static boolean hasIdentifier(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(ModItems.DISASTER_IDENTIFIER.get())) return true;
        }
        return false;
    }

    /** Идентификатор бедствий: пока он в инвентаре — показывает текущее бедствие над хотбаром. */
    private static void identifierHud(ServerLevel level) {
        for (ServerPlayer p : DisasterUtil.players(level)) {
            if (!hasIdentifier(p)) continue;
            p.displayClientMessage(statusLine(), true);
        }
    }

    public static Component statusLine() {
        MutableComponent line = Component.literal("📡 ").withStyle(ChatFormatting.YELLOW);
        if (!ACTIVE.isEmpty()) {
            boolean first = true;
            for (Disaster d : ACTIVE) {
                if (!first) line.append(Component.literal(" + ").withStyle(ChatFormatting.WHITE));
                first = false;
                line.append(Component.literal(d.type.displayName).withStyle(d.type.color, ChatFormatting.BOLD));
                line.append(Component.literal(" (" + formatTime(d.remainingTicks()) + ")").withStyle(ChatFormatting.GRAY));
            }
            return line;
        }
        if (!PENDING.isEmpty()) {
            line.append(Component.literal("Приближается: ").withStyle(ChatFormatting.RED));
            boolean first = true;
            for (DisasterType t : PENDING) {
                if (!first) line.append(Component.literal(" + ").withStyle(ChatFormatting.WHITE));
                first = false;
                line.append(Component.literal(t.displayName).withStyle(t.color, ChatFormatting.BOLD));
            }
            line.append(Component.literal(" через " + Math.max(0, warningTicks / 20) + " с").withStyle(ChatFormatting.GRAY));
            return line;
        }
        return line.append(Component.literal("Бедствий нет. Затишье...").withStyle(ChatFormatting.GREEN));
    }

    public static String formatTime(int ticks) {
        int s = ticks / 20;
        return s >= 60 ? (s / 60) + " мин " + (s % 60) + " с" : s + " с";
    }

    private DisasterManager() {}
}
