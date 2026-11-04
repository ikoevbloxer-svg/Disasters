package com.vladdisasters.registry;

import com.vladdisasters.VladDisasters;
import com.vladdisasters.block.TsunamiWaterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VladDisasters.MODID);

    /** Вода цунами: выглядит как вода, но не растекается и сама исчезает — после волны воды не остаётся. */
    public static final DeferredBlock<TsunamiWaterBlock> TSUNAMI_WATER = BLOCKS.register("tsunami_water",
            () -> new TsunamiWaterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .replaceable()
                    .randomTicks()
                    .strength(-1.0F, 3600000.0F)
                    .pushReaction(PushReaction.DESTROY)));

    private ModBlocks() {}
}
