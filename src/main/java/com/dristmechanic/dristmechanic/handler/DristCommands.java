package com.dristmechanic.dristmechanic.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.dristmechanic.dristmechanic.Dristmechanic;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class DristCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("drist").then(
                Commands.literal("center").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ServerLevel l = (ServerLevel) p.level();
                    FarmManager.FarmData f = FarmManager.findNearestFarm(l, p.blockPosition(), 32);
                    if (f.isEmpty()) ctx.getSource().sendSuccess(() -> Component.literal("[DRIST] Рядом нет ферм.").withStyle(ChatFormatting.RED), false);
                    else {
                        List<BlockPos> sp = RaidManager.findRaidSpawnPoints(l, f, 5, 0.6F, 1.8F);
                        StringBuilder sb = new StringBuilder();
                        if (sp.isEmpty()) sb.append("Не найдено");
                        else for (int i = 0; i < sp.size(); i++) { if (i > 0) sb.append("\n"); sb.append(sp.get(i).toShortString()); }
                        Vec3 c = f.center(); String cs = String.format("%.2f, %.2f, %.2f", c.x, c.y, c.z);
                        ctx.getSource().sendSuccess(() -> Component.literal("[DRIST] Найдена связанная ферма!\n").withStyle(ChatFormatting.GREEN)
                                .append(Component.literal("Точный центр: ").withStyle(ChatFormatting.YELLOW)).append(Component.literal(cs).withStyle(ChatFormatting.GRAY)).append("\n")
                                .append(Component.literal("Размер: ").withStyle(ChatFormatting.YELLOW)).append(Component.literal(f.chunks().size() + " участков").withStyle(ChatFormatting.GRAY)).append("\n")
                                .append(Component.literal("Общая ценность: ").withStyle(ChatFormatting.YELLOW)).append(Component.literal(String.valueOf(f.totalValue())).withStyle(ChatFormatting.GRAY)).append("\n")
                                .append(Component.literal("Точки появления рейда:\n").withStyle(ChatFormatting.YELLOW)).append(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY)), false);
                    }
                    return 1;
                })
        ));
    }
}