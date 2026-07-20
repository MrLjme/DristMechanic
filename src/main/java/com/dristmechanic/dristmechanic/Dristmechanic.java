package com.dristmechanic.dristmechanic;

import com.dristmechanic.dristmechanic.client.FlashParticle;
import com.dristmechanic.dristmechanic.client.ScrapParticle;
import com.dristmechanic.dristmechanic.client.TotebotRenderer;
import com.dristmechanic.dristmechanic.entity.TotebotEntity;
import com.dristmechanic.dristmechanic.init.ModAttachments;
import com.dristmechanic.dristmechanic.init.ModEntities;
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
        });

        modEventBus.addListener(this::addCreative);

        // ИЗМЕНЕНО: SERVER вместо COMMON, чтобы конфиг генерировался в папке serverconfig каждого мира
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
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