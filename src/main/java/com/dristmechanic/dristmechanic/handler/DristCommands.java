package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class DristCommands {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        // Временно пусто - команды будут переписаны под новую структуру
    }
}