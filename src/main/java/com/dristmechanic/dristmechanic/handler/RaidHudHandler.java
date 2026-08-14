package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.network.RaidHudPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber
public class RaidHudHandler {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 == 0) {
            event.getServer().getPlayerList().getPlayers().forEach(player -> {
                RaidManager.HudData data = RaidManager.getNearestRaidData(player.serverLevel(), player.position());
                PacketDistributor.sendToPlayer(player, new RaidHudPayload(data.isActive(), data.currentValue(), data.maxValue()));
            });
        }
    }
}