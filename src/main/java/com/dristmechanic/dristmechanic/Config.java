package com.dristmechanic.dristmechanic;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CROP_VALUES = BUILDER
            .comment("Ценность культур для расчета рейда. Формат: 'modid:block_name=value'")
            .defineList("cropValues", Arrays.asList(
                    "minecraft:wheat=1",
                    "minecraft:carrots=2",
                    "minecraft:potatoes=3",
                    "minecraft:beetroots=1",
                    "minecraft:melon_stem=3",
                    "minecraft:pumpkin_stem=3"
            ), obj -> obj instanceof String);

    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Кэш обновляется лениво в CropScanningHandler
    }
}