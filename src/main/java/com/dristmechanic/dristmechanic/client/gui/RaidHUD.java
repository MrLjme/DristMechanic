package com.dristmechanic.dristmechanic.client.gui;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Dristmechanic.MODID, value = Dist.CLIENT)
public class RaidHUD {

    private static final long EXPIRE_MS = 5000;
    private static final Map<Long, FarmDisplayData> farmsData = new ConcurrentHashMap<>();
    private static final double MAX_RENDER_DISTANCE_SQ = 70.0 * 70.0;

    private record FarmDisplayData(
            double centerX, double centerZ, boolean active,
            int currentValue, int maxValue, boolean raidActive, long lastUpdateMs) {}

    public static void updateData(int farmX, int farmZ, double centerX, double centerZ,
                                  boolean active, int current, int max, boolean raid) {
        farmsData.put(chunkKey(farmX, farmZ), new FarmDisplayData(
                centerX, centerZ, active, current, max, raid, System.currentTimeMillis()));
        cleanupExpired();
    }

    private static long chunkKey(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    private static void cleanupExpired() {
        long cutoff = System.currentTimeMillis() - EXPIRE_MS;
        farmsData.entrySet().removeIf(e -> e.getValue().lastUpdateMs() < cutoff);
    }

    private static FarmDisplayData findNearest() {
        Player player = Minecraft.getInstance().player;
        if (player == null || farmsData.isEmpty()) return null;

        FarmDisplayData best = null;
        double bestDist = Double.MAX_VALUE;

        for (FarmDisplayData d : farmsData.values()) {
            double dx = player.getX() - d.centerX();
            double dz = player.getZ() - d.centerZ();
            double distSq = dx * dx + dz * dz;
            if (distSq > MAX_RENDER_DISTANCE_SQ) continue;
            if (distSq < bestDist) {
                bestDist = distSq;
                best = d;
            }
        }
        return best;
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "raid_hud"),
                (g, delta) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) return;
                    cleanupExpired();
                    FarmDisplayData nearest = findNearest();
                    if (nearest == null || nearest.currentValue() <= 0) return;
                    draw(g, nearest);
                }
        );
    }

    private static final int YELLOW_MAX = 100;
    private static final int ORANGE_MAX = 1000;
    private static final int RED_MAX = 10000;
    private static final int PURPLE_MAX = 100000;

    private static final int INNER_ALPHA = 0xD9;

    private static final int COLOR_YELLOW = 0xFFFDDB48;
    private static final int COLOR_ORANGE = 0xFFF08229;
    private static final int COLOR_RED = 0xFFC80403;
    private static final int COLOR_DIM = 0xFF5E2A13;
    private static final int COLOR_PURPLE = 0xFF5B4BC8;
    private static final int COLOR_PURPLE_OUTLINE = 0xFF57508D;
    private static final int COLOR_PURPLE_BAND = 0xFF453A75;
    private static final int COLOR_FRAME = 0xFF3B0E0B;

    private static void draw(GuiGraphics g, FarmDisplayData data) {
        int gw = g.guiWidth();
        int gh = g.guiHeight();
        int width = Math.max(120, (int) (gw * 0.25F));
        int height = Math.max(6, (int) (gh * 0.025F) - 1);
        int x = (int) (gw * 0.025F);
        int y = (int) (gh * 0.045F);
        int value = data.currentValue();
        boolean purple = value >= RED_MAX;

        if (purple) {
            g.fill(x - 3, y - 3, x + width + 3, y + height + 3, COLOR_PURPLE_OUTLINE);
            g.fill(x - 2, y - 2, x + width + 2, y + height + 2, COLOR_PURPLE_BAND);
        } else {
            g.fill(x - 3, y - 3, x + width + 3, y + height + 3, 0xFF7D1E17);
            g.fill(x - 2, y - 2, x + width + 2, y + height + 2, COLOR_FRAME);
        }

        if (purple) {
            float ratio = Mth.clamp((value - RED_MAX) / (float) (PURPLE_MAX - RED_MAX), 0F, 1F);
            int fillW = (int) (width * ratio);
            g.fillGradient(x, y, x + width, y + height,
                    withAlpha(shade(COLOR_PURPLE, 0.25F), INNER_ALPHA),
                    withAlpha(shade(COLOR_PURPLE, 0.45F), INNER_ALPHA));
            if (fillW > 0) {
                g.fillGradient(x, y, x + fillW, y + height,
                        withAlpha(shade(COLOR_PURPLE, 0.7F), INNER_ALPHA),
                        withAlpha(shade(COLOR_PURPLE, 1.3F), INNER_ALPHA));
                g.fill(x, y, x + fillW, y + 1, 0x66FFFFFF);
                g.fill(x, y + height - 1, x + fillW, y + height, 0x66000000);
            }
        } else {
            int segW = width / 3;
            int[] from = {0, YELLOW_MAX, ORANGE_MAX};
            int[] to = {YELLOW_MAX, ORANGE_MAX, RED_MAX};
            int[] bright = {COLOR_YELLOW, COLOR_ORANGE, COLOR_RED};

            for (int i = 0; i < 3; i++) {
                int sx = x + i * segW;
                int sw = (i == 2) ? width - segW * 2 : segW;
                g.fill(sx, y, sx + sw, y + height, withAlpha(COLOR_DIM, INNER_ALPHA));
                float ratio = Mth.clamp((value - from[i]) / (float) (to[i] - from[i]), 0F, 1F);
                int fillW = (int) (sw * ratio);
                if (fillW > 0) {
                    g.fillGradient(sx, y, sx + fillW, y + height,
                            withAlpha(shade(bright[i], 0.75F), INNER_ALPHA),
                            withAlpha(shade(bright[i], 1.25F), INNER_ALPHA));
                    g.fill(sx, y, sx + fillW, y + 1, 0x55FFFFFF);
                    g.fill(sx, y + height - 1, sx + fillW, y + height, 0x55000000);
                }
                if (i > 0) g.fill(sx, y, sx + 1, y + height, 0xB3000000);
            }
            drawDiamond(g, x + segW, y + height / 2, COLOR_RED);
            drawDiamond(g, x + segW * 2, y + height / 2, COLOR_RED);
        }
    }

    private static void drawDiamond(GuiGraphics g, int cx, int cy, int color) {
        int[] bw = {1, 3, 5, 7, 5, 3, 1};
        for (int i = 0; i < bw.length; i++) {
            g.fill(cx - bw[i] / 2, cy - 3 + i, cx - bw[i] / 2 + bw[i], cy - 2 + i, 0xFF000000);
        }
        int[] cw = {1, 3, 5, 3, 1};
        for (int i = 0; i < cw.length; i++) {
            g.fill(cx - cw[i] / 2, cy - 2 + i, cx - cw[i] / 2 + cw[i], cy - 1 + i, color);
        }
    }

    private static int shade(int c, float f) {
        int r = Mth.clamp((int) (((c >> 16) & 0xFF) * f), 0, 255);
        int g2 = Mth.clamp((int) (((c >> 8) & 0xFF) * f), 0, 255);
        int b = Mth.clamp((int) ((c & 0xFF) * f), 0, 255);
        return 0xFF000000 | (r << 16) | (g2 << 8) | b;
    }

    private static int withAlpha(int c, int a) {
        return (Mth.clamp(a, 0, 255) << 24) | (c & 0xFFFFFF);
    }
}