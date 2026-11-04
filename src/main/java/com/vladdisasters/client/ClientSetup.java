package com.vladdisasters.client;

import com.vladdisasters.VladDisasters;
import com.vladdisasters.client.model.MamaVladModel;
import com.vladdisasters.client.render.BlackHoleRenderer;
import com.vladdisasters.client.render.MamaVladRenderer;
import com.vladdisasters.client.render.MeteorRenderer;
import com.vladdisasters.registry.ModBlocks;
import com.vladdisasters.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = VladDisasters.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MamaVladModel.LAYER, MamaVladModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MAMA_VLAD.get(), MamaVladRenderer::new);
        event.registerEntityRenderer(ModEntities.METEOR.get(), MeteorRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), BlackHoleRenderer::new);
    }

    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> 0x2F6FE0, ModBlocks.TSUNAMI_WATER.get());
    }

    @SubscribeEvent
    public static void guiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(VladDisasters.id("flash"), Overlays::renderFlash);
        event.registerAboveAll(VladDisasters.id("screamer"), Overlays::renderScreamer);
    }

    private ClientSetup() {}
}
