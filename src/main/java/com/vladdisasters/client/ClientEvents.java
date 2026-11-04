package com.vladdisasters.client;

import com.vladdisasters.VladDisasters;
import com.vladdisasters.network.ClientDisasterState;
import com.vladdisasters.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Туман, тряска камеры, снегопад/пепел вокруг игрока, звук скримера. */
@EventBusSubscriber(modid = VladDisasters.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final RandomSource RAND = RandomSource.create();
    private static float fogBlend; // плавное появление тумана

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (ClientDisasterState.screamerTicks > 0) ClientDisasterState.screamerTicks--;
        if (ClientDisasterState.flashTicks > 0) ClientDisasterState.flashTicks--;
        if (ClientDisasterState.screamerSoundPending) {
            ClientDisasterState.screamerSoundPending = false;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SCREAMER.get(), 1.0F, 1.0F));
        }
        int flags = ClientDisasterState.flags;
        float targetFog = flags != 0 ? 1F : 0F;
        fogBlend += (targetFog - fogBlend) * 0.03F;

        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.isPaused()) return;
        if (ClientDisasterState.has(ClientDisasterState.BLIZZARD)) {
            for (int i = 0; i < 45; i++) {
                double x = p.getX() + (RAND.nextDouble() - 0.5) * 18;
                double y = p.getY() + RAND.nextDouble() * 8 - 1;
                double z = p.getZ() + (RAND.nextDouble() - 0.5) * 18;
                mc.level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.6 + RAND.nextDouble() * 0.4, -0.15, 0.25);
            }
        }
        if (ClientDisasterState.has(ClientDisasterState.WINTER)) {
            for (int i = 0; i < 12; i++) {
                mc.level.addParticle(ParticleTypes.WHITE_ASH, p.getX() + (RAND.nextDouble() - 0.5) * 24, p.getY() + RAND.nextDouble() * 10,
                        p.getZ() + (RAND.nextDouble() - 0.5) * 24, 0, -0.02, 0);
            }
        }
        if (ClientDisasterState.has(ClientDisasterState.ASH)) {
            for (int i = 0; i < 25; i++) {
                mc.level.addParticle(ParticleTypes.ASH, p.getX() + (RAND.nextDouble() - 0.5) * 24, p.getY() + RAND.nextDouble() * 10,
                        p.getZ() + (RAND.nextDouble() - 0.5) * 24, 0, -0.05, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientDisasterState.reset();
        fogBlend = 0;
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        float s = ClientDisasterState.shake;
        if (s <= 0.001F || Minecraft.getInstance().isPaused()) return;
        float amp = s * 2.6F;
        event.setYaw(event.getYaw() + (RAND.nextFloat() - 0.5F) * amp);
        event.setPitch(event.getPitch() + (RAND.nextFloat() - 0.5F) * amp);
        event.setRoll(event.getRoll() + (RAND.nextFloat() - 0.5F) * amp * 1.5F);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (fogBlend < 0.01F) return;
        float[] c = fogColor();
        if (c == null) return;
        float k = fogBlend;
        event.setRed(event.getRed() * (1 - k) + c[0] * k);
        event.setGreen(event.getGreen() * (1 - k) + c[1] * k);
        event.setBlue(event.getBlue() * (1 - k) + c[2] * k);
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (fogBlend < 0.01F || event.getType() != FogType.NONE) return;
        float[] d = fogDistance();
        if (d == null) return;
        float far = event.getFarPlaneDistance();
        float near = event.getNearPlaneDistance();
        float k = fogBlend;
        float newFar = far * (1 - k) + Math.min(far, d[1]) * k;
        float newNear = near * (1 - k) + Math.min(near, d[0]) * k;
        if (event.getMode() == FogRenderer.FogMode.FOG_SKY) {
            newNear = 0;
        }
        event.setNearPlaneDistance(newNear);
        event.setFarPlaneDistance(newFar);
        event.setCanceled(true);
    }

    private static float[] fogColor() {
        if (ClientDisasterState.has(ClientDisasterState.BLIZZARD)) return new float[]{0.86F, 0.9F, 0.95F};
        if (ClientDisasterState.has(ClientDisasterState.WINTER)) return new float[]{0.32F, 0.32F, 0.34F};
        if (ClientDisasterState.has(ClientDisasterState.ASH)) return new float[]{0.28F, 0.24F, 0.22F};
        if (ClientDisasterState.has(ClientDisasterState.ACID)) return new float[]{0.42F, 0.6F, 0.16F};
        if (ClientDisasterState.has(ClientDisasterState.BLACK_HOLE)) return new float[]{0.12F, 0.04F, 0.18F};
        if (ClientDisasterState.has(ClientDisasterState.DROUGHT)) return new float[]{0.9F, 0.78F, 0.5F};
        return null;
    }

    /** {near, far} */
    private static float[] fogDistance() {
        if (ClientDisasterState.has(ClientDisasterState.BLIZZARD)) return new float[]{0F, 7F};
        if (ClientDisasterState.has(ClientDisasterState.WINTER)) return new float[]{0F, 42F};
        if (ClientDisasterState.has(ClientDisasterState.ASH)) return new float[]{0F, 55F};
        if (ClientDisasterState.has(ClientDisasterState.ACID)) return new float[]{0F, 80F};
        if (ClientDisasterState.has(ClientDisasterState.BLACK_HOLE)) return new float[]{0F, 120F};
        if (ClientDisasterState.has(ClientDisasterState.DROUGHT)) return new float[]{10F, 140F};
        return null;
    }

    private ClientEvents() {}
}
