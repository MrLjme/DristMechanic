package com.dristmechanic.dristmechanic.network;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class ModNetworking {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Dristmechanic.MODID);

        registrar.playToClient(
                RaidDataPayload.TYPE,
                RaidDataPayload.STREAM_CODEC,
                RaidDataPayload::handle
        );
    }
}