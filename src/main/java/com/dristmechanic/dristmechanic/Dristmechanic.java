package com.dristmechanic.dristmechanic;

import com.dristmechanic.dristmechanic.client.ChemicalProjectileRenderer;
import com.dristmechanic.dristmechanic.client.FarmbotRenderer;
import com.dristmechanic.dristmechanic.client.FlashParticle;
import com.dristmechanic.dristmechanic.client.HaybotRenderer;
import com.dristmechanic.dristmechanic.client.RedTapeRenderer;
import com.dristmechanic.dristmechanic.client.RedTapebotRenderer;
import com.dristmechanic.dristmechanic.client.ScrapParticle;
import com.dristmechanic.dristmechanic.client.TapeRenderer;
import com.dristmechanic.dristmechanic.client.TapebotRenderer;
import com.dristmechanic.dristmechanic.client.TotebotRenderer;
import com.dristmechanic.dristmechanic.client.model.*;
import com.dristmechanic.dristmechanic.entity.FarmbotEntity;
import com.dristmechanic.dristmechanic.entity.HaybotEntity;
import com.dristmechanic.dristmechanic.entity.RedTapebotEntity;
import com.dristmechanic.dristmechanic.entity.TapebotEntity;
import com.dristmechanic.dristmechanic.entity.TotebotEntity;
import com.dristmechanic.dristmechanic.init.ModAttachments;
import com.dristmechanic.dristmechanic.init.ModEntities;
import com.dristmechanic.dristmechanic.init.ModSounds;
import com.dristmechanic.dristmechanic.network.ModNetworking;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(Dristmechanic.MODID)
public class Dristmechanic {
    public static final String MODID = "dristmechanic";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLASH = PARTICLES.register("flash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SCRAP = PARTICLES.register("scrap", () -> new SimpleParticleType(false));

    // Spawn Eggs
    public static final DeferredItem<SpawnEggItem> TOTEBOT_SPAWN_EGG = ITEMS.registerItem("totebot_spawn_egg",
            properties -> new SpawnEggItem(ModEntities.TOTEBOT.get(), 0x648e3d, 0xa5c5e5, properties));

    public static final DeferredItem<SpawnEggItem> HAYBOT_SPAWN_EGG = ITEMS.registerItem("haybot_spawn_egg",
            properties -> new SpawnEggItem(ModEntities.HAYBOT.get(), 0xdb6241, 0xf3b93f, properties));

    public static final DeferredItem<SpawnEggItem> FARMBOT_SPAWN_EGG = ITEMS.registerItem("farmbot_spawn_egg",
            properties -> new SpawnEggItem(ModEntities.FARMBOT.get(), 0xec5234, 0xffffff, properties));

    public static final DeferredItem<SpawnEggItem> TAPEBOT_SPAWN_EGG = ITEMS.registerItem("tapebot_spawn_egg",
            properties -> new SpawnEggItem(ModEntities.TAPEBOT.get(), 0x2859c9, 0xcda03a, properties));

    public static final DeferredItem<SpawnEggItem> RED_TAPEBOT_SPAWN_EGG = ITEMS.registerItem("red_tapebot_spawn_egg",
            properties -> new SpawnEggItem(ModEntities.RED_TAPEBOT.get(), 0xc63530, 0xcda03a, properties));

    public Dristmechanic(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        PARTICLES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(ModEntities.TOTEBOT.get(), TotebotEntity.createAttributes().build());
            event.put(ModEntities.FARMBOT.get(), FarmbotEntity.createAttributes().build());
            event.put(ModEntities.HAYBOT.get(), HaybotEntity.createAttributes().build());
            event.put(ModEntities.RED_TAPEBOT.get(), RedTapebotEntity.createAttributes().build());
            event.put(ModEntities.TAPEBOT.get(), TapebotEntity.createAttributes().build());
        });

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(TOTEBOT_SPAWN_EGG);
            event.accept(HAYBOT_SPAWN_EGG);
            event.accept(FARMBOT_SPAWN_EGG);
            event.accept(TAPEBOT_SPAWN_EGG);
            event.accept(RED_TAPEBOT_SPAWN_EGG);
        }
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.TOTEBOT.get(), TotebotRenderer::new);
            event.registerEntityRenderer(ModEntities.FARMBOT.get(), FarmbotRenderer::new);
            event.registerEntityRenderer(ModEntities.HAYBOT.get(), HaybotRenderer::new);
            event.registerEntityRenderer(ModEntities.RED_TAPEBOT.get(), RedTapebotRenderer::new);
            event.registerEntityRenderer(ModEntities.TAPEBOT.get(), TapebotRenderer::new);
            event.registerEntityRenderer(ModEntities.TAPE.get(), TapeRenderer::new);
            event.registerEntityRenderer(ModEntities.RED_TAPE.get(), RedTapeRenderer::new);
            event.registerEntityRenderer(ModEntities.CHEMICAL_PROJECTILE.get(), ChemicalProjectileRenderer::new);
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(Modelfarmbot.LAYER_LOCATION, Modelfarmbot::createBodyLayer);
            event.registerLayerDefinition(Modelhaybot.LAYER_LOCATION, Modelhaybot::createBodyLayer);
            event.registerLayerDefinition(Modelred_tapebot.LAYER_LOCATION, Modelred_tapebot::createBodyLayer);
            event.registerLayerDefinition(Modeltapebot.LAYER_LOCATION, Modeltapebot::createBodyLayer);
            event.registerLayerDefinition(Modeltape.LAYER_LOCATION, Modeltape::createBodyLayer);
            event.registerLayerDefinition(ChemicalProjectile.LAYER_LOCATION, ChemicalProjectile::createBodyLayer);
        }

        @SubscribeEvent
        public static void registerParticles(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(Dristmechanic.FLASH.get(), spriteSet ->
                    (options, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                            new FlashParticle(level, x, y, z, xSpeed, ySpeed, zSpeed)
            );

            event.registerSpecial(Dristmechanic.SCRAP.get(), new ScrapParticle.Factory());
        }
    }
}