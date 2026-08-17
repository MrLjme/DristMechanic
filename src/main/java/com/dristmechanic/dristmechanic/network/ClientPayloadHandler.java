package com.dristmechanic.dristmechanic.network;

import com.dristmechanic.dristmechanic.client.gui.RaidHUD;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {

    public static void handleRaidData(RaidDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                RaidHUD.updateData(
                        payload.farmX(),
                        payload.farmZ(),
                        payload.centerX(),
                        payload.centerZ(),
                        payload.active(),
                        payload.currentValue(),
                        payload.maxValue(),
                        payload.raidActive()
                )
        );
    }

    public static void handleRaidHud(RaidHudPayload payload, IPayloadContext context) {
    }
}