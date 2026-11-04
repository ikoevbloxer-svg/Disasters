package com.vladdisasters.network;

import com.vladdisasters.VladDisasters;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {

    /** Флаги визуальных эффектов (туман, снег, пепел) и сила тряски камеры. */
    public record EffectsPayload(int flags, float shake) implements CustomPacketPayload {
        public static final Type<EffectsPayload> TYPE = new Type<>(VladDisasters.id("effects"));
        public static final StreamCodec<ByteBuf, EffectsPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, EffectsPayload::flags,
                ByteBufCodecs.FLOAT, EffectsPayload::shake,
                EffectsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Скример на весь экран. */
    public record ScreamerPayload(int ticks) implements CustomPacketPayload {
        public static final Type<ScreamerPayload> TYPE = new Type<>(VladDisasters.id("screamer"));
        public static final StreamCodec<ByteBuf, ScreamerPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ScreamerPayload::ticks, ScreamerPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Белая вспышка (ядерный взрыв, гига-астероид). */
    public record FlashPayload(int ticks) implements CustomPacketPayload {
        public static final Type<FlashPayload> TYPE = new Type<>(VladDisasters.id("flash"));
        public static final StreamCodec<ByteBuf, FlashPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, FlashPayload::ticks, FlashPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");
        reg.playToClient(EffectsPayload.TYPE, EffectsPayload.CODEC, (p, ctx) -> {
            ClientDisasterState.flags = p.flags();
            ClientDisasterState.shake = p.shake();
        });
        reg.playToClient(ScreamerPayload.TYPE, ScreamerPayload.CODEC, (p, ctx) -> {
            ClientDisasterState.screamerTicks = p.ticks();
            ClientDisasterState.screamerSoundPending = true;
        });
        reg.playToClient(FlashPayload.TYPE, FlashPayload.CODEC, (p, ctx) -> {
            ClientDisasterState.flashTicks = p.ticks();
            ClientDisasterState.flashMax = Math.max(1, p.ticks());
        });
    }

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    private ModNetwork() {}
}
