package com.dristmechanic.dristmechanic.init;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.entity.TotebotEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Dristmechanic.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<TotebotEntity>> TOTEBOT =
            ENTITIES.register("totebot",
                    () -> EntityType.Builder.of(TotebotEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build("totebot")
            );
}