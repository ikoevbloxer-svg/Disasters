package com.vladdisasters.registry;

import com.vladdisasters.VladDisasters;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VladDisasters.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.vladdisasters"))
            .icon(() -> ModItems.KOLBASA.get().getDefaultInstance())
            .displayItems((params, out) -> {
                out.accept(ModItems.DISASTER_IDENTIFIER.get());
                out.accept(ModItems.KOLBASA.get());
                out.accept(ModItems.MAMA_VLAD_SPAWN_EGG.get());
            })
            .build());

    private ModTabs() {}
}
