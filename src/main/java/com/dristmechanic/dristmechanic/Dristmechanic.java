package com.dristmechanic.dristmechanic;

import com.dristmechanic.dristmechanic.client.FarmbotRenderer;
import com.dristmechanic.dristmechanic.client.FlashParticle;
import com.dristmechanic.dristmechanic.client.HaybotRenderer;
import com.dristmechanic.dristmechanic.client.RedTapeRenderer;
import com.dristmechanic.dristmechanic.client.RedTapebotRenderer;
import com.dristmechanic.dristmechanic.client.ScrapParticle;
import com.dristmechanic.dristmechanic.client.TapeRenderer;
import com.dristmechanic.dristmechanic.client.TapebotRenderer;
import com.dristmechanic.dristmechanic.client.TotebotRenderer;
import com.dristmechanic.dristmechanic.client.model.Modelfarmbot;
import com.dristmechanic.dristmechanic.client.model.Modelhaybot;
import com.dristmechanic.dristmechanic.client.model.Modelred_tapebot;
import com.dristmechanic.dristmechanic.client.model.Modeltape;
import com.dristmechanic.dristmechanic.client.model.Modeltapebot;
import com.dristmechanic.dristmechanic.entity.FarmbotEntity;
import com.dristmechanic.dristmechanic.entity.HaybotEntity;
import com.dristmechanic.dristmechanic.entity.RedTapebotEntity;
import com.dristmechanic.dristmechanic.entity.TapebotEntity;
import com.dristmechanic.dristmechanic.entity.TotebotEntity;
import com.dristmechanic.dristmechanic.init.ModAttachments;
import com.dristmechanic.dristmechanic.init.ModEntities;
import com.dristmechanic.dristmechanic.network.ClientPayloadHandler;
import com.dristmechanic.dristmechanic.network.RaidHudPayload;
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
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
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

    public static final DeferredItem<SpawnEggItem> TOTEBOT_SPAWN_EGG = ITEMS.registerItem("totebot_spawn_egg",
            properties -> new SpawnEggItem(ModEntities.TOTEBOT.get(), 0x4A4A4A, 0xFF6600, properties));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DRIST_TAB = CREATIVE_MODE_TABS.register("drist_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dristmechanic"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> TOTEBOT_SPAWN_EGG.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(TOTEBOT_SPAWN_EGG.get());
            })
            .build());

    public Dristmechanic(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        PARTICLES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);

        modEventBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(ModEntities.TOTEBOT.get(), TotebotEntity.createAttributes().build());
            event.put(ModEntities.FARMBOT.get(), FarmbotEntity.createAttributes().build());
            event.put(ModEntities.HAYBOT.get(), HaybotEntity.createAttributes().build());
            event.put(ModEntities.RED_TAPEBOT.get(), RedTapebotEntity.createAttributes().build());
            event.put(ModEntities.TAPEBOT.get(), TapebotEntity.createAttributes().build());
        });

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        modEventBus.addListener((RegisterPayloadHandlersEvent event) -> {
            final PayloadRegistrar registrar = event.registrar(Dristmechanic.MODID).versioned("1.0");
            registrar.playToClient(RaidHudPayload.TYPE, RaidHudPayload.STREAM_CODEC, ClientPayloadHandler::handleRaidHud);
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(TOTEBOT_SPAWN_EGG);
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
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(Modelfarmbot.LAYER_LOCATION, Modelfarmbot::createBodyLayer);
            event.registerLayerDefinition(Modelhaybot.LAYER_LOCATION, Modelhaybot::createBodyLayer);
            event.registerLayerDefinition(Modelred_tapebot.LAYER_LOCATION, Modelred_tapebot::createBodyLayer);
            event.registerLayerDefinition(Modeltapebot.LAYER_LOCATION, Modeltapebot::createBodyLayer);
            event.registerLayerDefinition(Modeltape.LAYER_LOCATION, Modeltape::createBodyLayer);
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