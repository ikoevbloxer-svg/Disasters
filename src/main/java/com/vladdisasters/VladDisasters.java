package com.vladdisasters;

import com.mojang.logging.LogUtils;
import com.vladdisasters.network.ModNetwork;
import com.vladdisasters.registry.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Случайные Бедствия: Влад Эдишн.
 * Главный класс мода — регистрирует всё содержимое.
 */
@Mod(VladDisasters.MODID)
public class VladDisasters {
    public static final String MODID = "vladdisasters";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VladDisasters(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModStructures.STRUCTURE_TYPES.register(modBus);
        ModStructures.PIECES.register(modBus);
        ModTabs.TABS.register(modBus);

        modBus.addListener(ModNetwork::register);
        modBus.addListener(ModEntities::registerAttributes);
        LOGGER.info("Мамка Влада проголодалась. Бедствия загружены.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
