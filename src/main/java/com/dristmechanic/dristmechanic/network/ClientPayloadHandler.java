package com.dristmechanic.dristmechanic.network;

import com.dristmechanic.dristmechanic.client.gui.RaidHUD;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleRaidHud(RaidHudPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            RaidHUD.updateData(payload.isActive(), payload.currentValue(), payload.maxValue());
        });
    }
}