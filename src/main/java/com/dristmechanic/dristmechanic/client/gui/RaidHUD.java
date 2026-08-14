package com.dristmechanic.dristmechanic.client.gui;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Dristmechanic.MODID, value = Dist.CLIENT)
public class RaidHUD {
    private static boolean isActive = false;
    private static int currentValue = 0;
    private static int maxValue = 100000;

    private static final int YELLOW_MAX = 100;
    private static final int ORANGE_MAX = 1000;
    private static final int RED_MAX = 10000;
    private static final int PURPLE_MAX = 100000;

    private static final int COLOR_YELLOW = 0xFFF2D24F;
    private static final int COLOR_YELLOW_DIM = 0xFF6E5F23;
    private static final int COLOR_ORANGE = 0xFFE1862C;
    private static final int COLOR_ORANGE_DIM = 0xFF6E3F14;
    private static final int COLOR_RED = 0xFFD21C1C;
    private static final int COLOR_RED_DIM = 0xFF6E0E0E;
    private static final int COLOR_PURPLE = 0xFF453A75;
    private static final int COLOR_PURPLE_DIM = 0xFF2B2450;
    private static final int COLOR_PURPLE_FRAME = 0xFF9C8FE0;
    private static final int COLOR_FRAME = 0xFF9E1B1B;

    public static void updateData(boolean active, int current, int max) {
        isActive = active;
        currentValue = current;
        maxValue = max;
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "raid_hud"), new LayeredDraw.Layer() {
            @Override
            public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
                if (Minecraft.getInstance().player == null || currentValue <= 0) return;
                draw(guiGraphics);
            }
        });
    }

    private static void draw(GuiGraphics g) {
        int width = 200;
        int height = 12;
        int x = 10;
        int y = 10;
        int frame = 2;

        boolean purple = currentValue >= RED_MAX;

        g.fill(x - frame - 1, y - frame - 1, x + width + frame + 1, y + height + frame + 1, 0xFF000000);
        g.fill(x - frame, y - frame, x + width + frame, y + height + frame, purple ? COLOR_PURPLE_FRAME : COLOR_FRAME);

        if (purple) {
            g.fill(x, y, x + width, y + height, COLOR_PURPLE_DIM);
            float ratio = Mth.clamp((currentValue - RED_MAX) / (float) (PURPLE_MAX - RED_MAX), 0.0F, 1.0F);
            int fillW = (int) (width * ratio);
            if (fillW > 0) g.fill(x, y, x + fillW, y + height, COLOR_PURPLE);
        } else {
            int segW = width / 3;
            int[] xs = {x, x + segW, x + segW * 2};
            int[] dim = {COLOR_YELLOW_DIM, COLOR_ORANGE_DIM, COLOR_RED_DIM};
            int[] bright = {COLOR_YELLOW, COLOR_ORANGE, COLOR_RED};
            int[] from = {0, YELLOW_MAX, ORANGE_MAX};
            int[] to = {YELLOW_MAX, ORANGE_MAX, RED_MAX};

            for (int i = 0; i < 3; i++) {
                int w = (i == 2) ? (x + width - xs[i]) : segW;
                g.fill(xs[i], y, xs[i] + w, y + height, dim[i]);
                float ratio = Mth.clamp((currentValue - from[i]) / (float) (to[i] - from[i]), 0.0F, 1.0F);
                int fillW = (int) (w * ratio);
                if (fillW > 0) g.fill(xs[i], y, xs[i] + fillW, y + height, bright[i]);
            }

            drawDiamond(g, x + segW, y + height / 2, COLOR_RED);
            drawDiamond(g, x + segW * 2, y + height / 2, COLOR_RED);

            g.fill(x + width - 2, y, x + width, y + height, 0xFFE8E8E8);
        }

        Component text;
        if (purple) {
            text = Component.literal("THREAT: " + currentValue + "/" + PURPLE_MAX).withStyle(ChatFormatting.DARK_PURPLE);
        } else if (isActive) {
            text = Component.literal("LICENSE BREACH: " + currentValue).withStyle(ChatFormatting.RED);
        } else {
            text = Component.literal("THREAT: " + currentValue).withStyle(ChatFormatting.YELLOW);
        }
        g.drawString(Minecraft.getInstance().font, text, x, y + height + 6, 0xFFFFFF, true);
    }

    private static void drawDiamond(GuiGraphics g, int cx, int cy, int color) {
        int[] bw = {1, 3, 5, 7, 5, 3, 1};
        for (int i = 0; i < bw.length; i++) {
            int yy = cy - 3 + i;
            int w = bw[i];
            g.fill(cx - w / 2, yy, cx - w / 2 + w, yy + 1, 0xFF000000);
        }
        int[] cw = {1, 3, 5, 3, 1};
        for (int i = 0; i < cw.length; i++) {
            int yy = cy - 2 + i;
            int w = cw[i];
            g.fill(cx - w / 2, yy, cx - w / 2 + w, yy + 1, color);
        }
    }
}