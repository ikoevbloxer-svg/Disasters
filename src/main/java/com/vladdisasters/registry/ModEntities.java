package com.vladdisasters.registry;

import com.vladdisasters.VladDisasters;
import com.vladdisasters.entity.BlackHoleEntity;
import com.vladdisasters.entity.MamaVladEntity;
import com.vladdisasters.entity.MeteorEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, VladDisasters.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MamaVladEntity>> MAMA_VLAD = ENTITIES.register("mama_vlad",
            () -> EntityType.Builder.<MamaVladEntity>of(MamaVladEntity::new, MobCategory.MONSTER)
                    .sized(2.6F, 5.4F)
                    .eyeHeight(4.6F)
                    .clientTrackingRange(12)
                    .build("mama_vlad"));

    public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR = ENTITIES.register("meteor",
            () -> EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(20)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .build("meteor"));

    public static final DeferredHolder<EntityType<?>, EntityType<BlackHoleEntity>> BLACK_HOLE = ENTITIES.register("black_hole",
            () -> EntityType.Builder.<BlackHoleEntity>of(BlackHoleEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .clientTrackingRange(20)
                    .updateInterval(2)
                    .fireImmune()
                    .noSave()
                    .build("black_hole"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MAMA_VLAD.get(), MamaVladEntity.createAttributes().build());
    }

    private ModEntities() {}
}
