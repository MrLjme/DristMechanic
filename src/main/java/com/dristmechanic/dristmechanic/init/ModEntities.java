package com.dristmechanic.dristmechanic.init;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.entity.*;
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

    public static final DeferredHolder<EntityType<?>, EntityType<FarmbotEntity>> FARMBOT =
            ENTITIES.register("farmbot",
                    () -> EntityType.Builder.of(FarmbotEntity::new, MobCategory.CREATURE)
                            .sized(1.9F, 2.8F)
                            .clientTrackingRange(10)
                            .build("farmbot")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<HaybotEntity>> HAYBOT =
            ENTITIES.register("haybot",
                    () -> EntityType.Builder.of(HaybotEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.5F)
                            .clientTrackingRange(10)
                            .build("haybot")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<RedTapebotEntity>> RED_TAPEBOT =
            ENTITIES.register("red_tapebot",
                    () -> EntityType.Builder.of(RedTapebotEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.9F)
                            .clientTrackingRange(10)
                            .build("red_tapebot")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<TapebotEntity>> TAPEBOT =
            ENTITIES.register("tapebot",
                    () -> EntityType.Builder.of(TapebotEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.9F)
                            .clientTrackingRange(10)
                            .build("tapebot")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<TapeEntity>> TAPE =
            ENTITIES.register("tape",
                    () -> EntityType.Builder.<TapeEntity>of(TapeEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .build("tape")
            );
    public static final DeferredHolder<EntityType<?>, EntityType<RedTapeEntity>> RED_TAPE =
            ENTITIES.register("red_tape",
                    () -> EntityType.Builder.<RedTapeEntity>of(RedTapeEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .build("red_tape")
            );
}