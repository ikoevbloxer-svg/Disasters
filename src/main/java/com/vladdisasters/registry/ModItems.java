package com.vladdisasters.registry;

import com.vladdisasters.VladDisasters;
import com.vladdisasters.item.DisasterIdentifierItem;
import com.vladdisasters.item.KolbasaItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VladDisasters.MODID);

    public static final DeferredItem<DisasterIdentifierItem> DISASTER_IDENTIFIER = ITEMS.register("disaster_identifier",
            () -> new DisasterIdentifierItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<KolbasaItem> KOLBASA = ITEMS.register("kolbasa",
            () -> new KolbasaItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredItem<DeferredSpawnEggItem> MAMA_VLAD_SPAWN_EGG = ITEMS.register("mama_vlad_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MAMA_VLAD, 0x8E3B2F, 0xE0622B, new Item.Properties()));

    private ModItems() {}
}
