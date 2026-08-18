package com.dristmechanic.dristmechanic;

import com.dristmechanic.dristmechanic.client.*;
import com.dristmechanic.dristmechanic.client.model.*;
import com.dristmechanic.dristmechanic.init.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = Dristmechanic.MODID, value = Dist.CLIENT)
public class ClientModEvents {

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