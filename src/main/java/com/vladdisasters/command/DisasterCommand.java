package com.vladdisasters.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.vladdisasters.disaster.Disaster;
import com.vladdisasters.disaster.DisasterManager;
import com.vladdisasters.disaster.DisasterType;
import com.vladdisasters.screamer.ScreamerManager;
import com.vladdisasters.world.BunkerData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * /disasters start <тип> [предупреждение]
 * /disasters double <тип> <тип>
 * /disasters stop | status | list
 * /disasters next <секунды>
 * /disasters interval <мин_минут> <макс_минут>
 * /disasters auto <true|false>
 * /disasters screamer now|on|off
 * /disasters bunker
 */
public final class DisasterCommand {
    private static final SuggestionProvider<CommandSourceStack> TYPES = (ctx, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(DisasterType.values()).map(DisasterType::id), b);

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("disasters")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("start")
                        .then(Commands.argument("type", StringArgumentType.word()).suggests(TYPES)
                                .executes(c -> start(c, false))
                                .then(Commands.argument("warning", BoolArgumentType.bool())
                                        .executes(c -> start(c, BoolArgumentType.getBool(c, "warning"))))))
                .then(Commands.literal("double")
                        .then(Commands.argument("a", StringArgumentType.word()).suggests(TYPES)
                                .then(Commands.argument("b", StringArgumentType.word()).suggests(TYPES)
                                        .executes(DisasterCommand::doubleStart))))
                .then(Commands.literal("stop").executes(c -> {
                    DisasterManager.stopAll(c.getSource().getServer().overworld());
                    ok(c, "Все бедствия остановлены.");
                    return 1;
                }))
                .then(Commands.literal("status").executes(c -> {
                    c.getSource().sendSuccess(DisasterManager::statusLine, false);
                    if (DisasterManager.active().isEmpty() && DisasterManager.pending().isEmpty()) {
                        ok(c, "Следующее бедствие через " + DisasterManager.formatTime(DisasterManager.cooldownTicks())
                                + (DisasterManager.autoEnabled ? "" : " (авто выключено)"));
                    }
                    return 1;
                }))
                .then(Commands.literal("list").executes(c -> {
                    StringBuilder sb = new StringBuilder("Бедствия: ");
                    for (DisasterType t : DisasterType.values()) sb.append(t.id()).append(t.rare ? "*" : "").append(" ");
                    ok(c, sb + "  (* — редкие)");
                    return 1;
                }))
                .then(Commands.literal("next")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 36000)).executes(c -> {
                            DisasterManager.setCooldownSeconds(IntegerArgumentType.getInteger(c, "seconds"));
                            ok(c, "Следующее бедствие через " + IntegerArgumentType.getInteger(c, "seconds") + " с.");
                            return 1;
                        })))
                .then(Commands.literal("interval")
                        .then(Commands.argument("min", IntegerArgumentType.integer(1, 600))
                                .then(Commands.argument("max", IntegerArgumentType.integer(1, 600)).executes(c -> {
                                    int min = IntegerArgumentType.getInteger(c, "min"), max = IntegerArgumentType.getInteger(c, "max");
                                    DisasterManager.minIntervalSec = Math.min(min, max) * 60;
                                    DisasterManager.maxIntervalSec = Math.max(min, max) * 60;
                                    ok(c, "Интервал между бедствиями: " + Math.min(min, max) + "–" + Math.max(min, max) + " мин.");
                                    return 1;
                                }))))
                .then(Commands.literal("auto")
                        .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(c -> {
                            DisasterManager.autoEnabled = BoolArgumentType.getBool(c, "enabled");
                            ok(c, "Случайные бедствия: " + (DisasterManager.autoEnabled ? "включены" : "выключены"));
                            return 1;
                        })))
                .then(Commands.literal("screamer")
                        .then(Commands.literal("now").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayer();
                            if (p != null) ScreamerManager.trigger(p);
                            else ScreamerManager.triggerAll(c.getSource().getServer().overworld());
                            return 1;
                        }))
                        .then(Commands.literal("all").executes(c -> {
                            ScreamerManager.triggerAll(c.getSource().getServer().overworld());
                            return 1;
                        }))
                        .then(Commands.literal("on").executes(c -> {
                            ScreamerManager.enabled = true;
                            ok(c, "Скример включён.");
                            return 1;
                        }))
                        .then(Commands.literal("off").executes(c -> {
                            ScreamerManager.enabled = false;
                            ok(c, "Скример выключен.");
                            return 1;
                        })))
                .then(Commands.literal("bunker").executes(c -> {
                    BunkerData data = BunkerData.get(c.getSource().getServer().overworld());
                    if (data.pos == null) ok(c, "Бункер не найден.");
                    else ok(c, "Бункер: " + data.pos.getX() + " " + data.pos.getY() + " " + data.pos.getZ());
                    return 1;
                })));
    }

    private static int start(CommandContext<CommandSourceStack> c, boolean warning) {
        DisasterType t = DisasterType.byId(StringArgumentType.getString(c, "type"));
        if (t == null) {
            c.getSource().sendFailure(Component.literal("Нет такого бедствия. /disasters list"));
            return 0;
        }
        ServerLevel level = c.getSource().getServer().overworld();
        if (!DisasterManager.forceStart(level, t, warning)) {
            c.getSource().sendFailure(Component.literal(t == DisasterType.VOLCANO
                    ? "Рядом нет вулкана (ищи его через /locate structure vladdisasters:volcano)."
                    : "Не удалось начать бедствие (в Верхнем мире нет игроков?)."));
            return 0;
        }
        ok(c, "Запущено: " + t.displayName);
        return 1;
    }

    private static int doubleStart(CommandContext<CommandSourceStack> c) {
        DisasterType a = DisasterType.byId(StringArgumentType.getString(c, "a"));
        DisasterType b = DisasterType.byId(StringArgumentType.getString(c, "b"));
        if (a == null || b == null || a.rare || b.rare) {
            c.getSource().sendFailure(Component.literal("Для двойного бедствия нужны два обычных бедствия."));
            return 0;
        }
        if (!DisasterManager.forceDouble(c.getSource().getServer().overworld(), a, b)) {
            c.getSource().sendFailure(Component.literal("Эти бедствия несовместимы."));
            return 0;
        }
        return 1;
    }

    private static void ok(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendSuccess(() -> Component.literal(msg).withStyle(ChatFormatting.YELLOW), false);
    }

    @SuppressWarnings("unused")
    private static String describe(Disaster d) {
        return d.type.displayName;
    }

    private DisasterCommand() {}
}
