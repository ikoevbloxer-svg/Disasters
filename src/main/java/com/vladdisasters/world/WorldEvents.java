package com.vladdisasters.world;

import com.vladdisasters.VladDisasters;
import com.vladdisasters.command.DisasterCommand;
import com.vladdisasters.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = VladDisasters.MODID)
public final class WorldEvents {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        BunkerData data = BunkerData.get(overworld);
        if (data.generated) return;
        try {
            BlockPos pos = BunkerBuilder.buildNearSpawn(overworld);
            data.generated = true;
            data.pos = pos;
            data.setDirty();
            VladDisasters.LOGGER.info("Бункер построен: {}", pos);
        } catch (Exception e) {
            VladDisasters.LOGGER.error("Не удалось построить бункер", e);
            data.generated = true;
            data.setDirty();
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ModNetwork.send(p, new ModNetwork.EffectsPayload(0, 0));
        CompoundTag data = p.getPersistentData();
        if (!data.getBoolean("vladdisasters_hint")) {
            data.putBoolean("vladdisasters_hint", true);
            p.sendSystemMessage(Component.literal("☢ В этом мире случаются бедствия. Ходят слухи, что примерно в 150 блоках от спавна спрятан бункер...")
                    .withStyle(ChatFormatting.GOLD));
            p.sendSystemMessage(Component.literal("Говорят, Мама Влада обожает колбасу. А в церкви Давида Икоева лучше не задерживаться.")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        DisasterCommand.register(event.getDispatcher());
    }

    private WorldEvents() {}
}
