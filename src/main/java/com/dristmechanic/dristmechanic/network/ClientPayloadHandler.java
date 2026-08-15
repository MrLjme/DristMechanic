package com.dristmechanic.dristmechanic.network;

import com.dristmechanic.dristmechanic.client.gui.RaidHUD;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handle(RaidDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            RaidHUD.updateData(
                    payload.active(),
                    payload.currentValue(),
                    payload.maxValue(),
                    payload.raidActive()
            );
        });
    }
}