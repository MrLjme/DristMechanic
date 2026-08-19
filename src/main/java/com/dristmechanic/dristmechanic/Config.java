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
            .comment("Format: 'modid:blockname=value' - Each crop has a value that contributes to raid difficulty")
            .defineList("cropValues", Arrays.asList(
                    "minecraft:wheat=1",
                    "minecraft:carrots=2",
                    "minecraft:potatoes=3",
                    "minecraft:beetroots=1",
                    "minecraft:melon_stem=5",
                    "minecraft:pumpkin_stem=5",
                    "farmersdelight:budding_tomatoes=3",
                    "farmersdelight:onions=3",
                    "farmersdelight:cabbages=3"
            ), obj -> obj instanceof String);

    public static final ModConfigSpec.IntValue STABILITY_DELAY_TICKS = BUILDER
            .comment("Time in ticks (20 ticks = 1 second) before crops are considered 'stable' and their value is extracted")
            .defineInRange("stabilityDelayTicks", 60, 20, 600);

    public static final ModConfigSpec.IntValue RAID_COUNTDOWN_TICKS = BUILDER
            .comment("Time in ticks before raid starts after value is extracted")
            .defineInRange("raidCountdownTicks", 1200, 200, 3600);

    public static final ModConfigSpec.IntValue LOCKDOWN_RADIUS = BUILDER
            .comment("Radius in chunks around the farm where planting is blocked during raid")
            .defineInRange("lockdownRadius", 1, 0, 10);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> LEVEL_THRESHOLDS = BUILDER
            .defineList("levelThresholds", Arrays.asList(0, 50, 100, 550, 1000, 5500, 10001), obj -> obj instanceof Integer);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> MIN_BUDGET = BUILDER
            .defineList("minBudget", Arrays.asList(2, 20, 75, 125, 300, 500, 1000), obj -> obj instanceof Integer);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> MAX_BUDGET = BUILDER
            .defineList("maxBudget", Arrays.asList(30, 50, 135, 200, 500, 700, 5000), obj -> obj instanceof Integer);

    public static final ModConfigSpec.IntValue MAX_CROP_VALUE = BUILDER
            .defineInRange("maxCropValue", 100000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> PLAYER_MULTIPLIERS = BUILDER
            .defineList("playerMultipliers", Arrays.asList(1.0, 1.5, 2.0), obj -> obj instanceof Double);

    public static final ModConfigSpec.IntValue SPAWN_INTERVAL_TICKS = BUILDER
            .defineInRange("spawnIntervalTicks", 40, 1, 1200);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> GUARANTEED_SPAWNS = BUILDER
            .defineList("guaranteedSpawns", Arrays.asList(
                    "1:dristmechanic:haybot:1,dristmechanic:totebot:2",
                    "2:dristmechanic:haybot:3,dristmechanic:totebot:2",
                    "3:dristmechanic:haybot:3,dristmechanic:totebot:2,dristmechanic:totebot:1",
                    "4:random:dristmechanic:haybot:2,dristmechanic:totebot:1,dristmechanic:tapebot:1,dristmechanic:tapebot:1|dristmechanic:haybot:2,dristmechanic:totebot:2,dristmechanic:totebot:1,dristmechanic:tapebot:1|dristmechanic:totebot:2,dristmechanic:totebot:1",
                    "5:dristmechanic:farmbot:1",
                    "6:dristmechanic:farmbot:1",
                    "7:dristmechanic:farmbot:3"
            ), obj -> obj instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BUDGET_GROUPS = BUILDER
            .defineList("budgetGroups", Arrays.asList(
                    "1:2:1:dristmechanic:totebot:1",
                    "1:4:10:dristmechanic:totebot:2",
                    "1:5:100:dristmechanic:haybot:1",
                    "1:9:100:dristmechanic:totebot:2,dristmechanic:haybot:1",
                    "1:12:100:dristmechanic:totebot:1,dristmechanic:haybot:2",
                    "2:7:1:dristmechanic:totebot:1,dristmechanic:haybot:1",
                    "2:12:10:dristmechanic:totebot:1,dristmechanic:haybot:1,dristmechanic:totebot:1",
                    "2:17:50:dristmechanic:totebot:1,dristmechanic:haybot:2,dristmechanic:totebot:1",
                    "2:19:100:dristmechanic:totebot:2,dristmechanic:haybot:3",
                    "3:7:1:dristmechanic:totebot:1,dristmechanic:haybot:1",
                    "3:12:10:dristmechanic:totebot:1,dristmechanic:haybot:1,dristmechanic:totebot:1",
                    "3:17:50:dristmechanic:totebot:1,dristmechanic:haybot:2,dristmechanic:totebot:1",
                    "3:19:100:dristmechanic:totebot:2,dristmechanic:haybot:3",
                    "3:24:50:dristmechanic:totebot:2,dristmechanic:totebot:1",
                    "3:16:50:dristmechanic:totebot:3,dristmechanic:tapebot:1,dristmechanic:tapebot:1",
                    "4:7:1:dristmechanic:totebot:1,dristmechanic:haybot:1",
                    "4:12:10:dristmechanic:totebot:1,dristmechanic:haybot:1,dristmechanic:totebot:1",
                    "4:17:50:dristmechanic:totebot:1,dristmechanic:haybot:2,dristmechanic:totebot:1",
                    "4:19:100:dristmechanic:totebot:2,dristmechanic:haybot:3",
                    "4:19:50:dristmechanic:totebot:2,dristmechanic:totebot:1",
                    "4:19:50:dristmechanic:totebot:2,dristmechanic:totebot:1",
                    "4:16:50:dristmechanic:totebot:3,dristmechanic:tapebot:1,dristmechanic:tapebot:1",
                    "4:19:50:dristmechanic:totebot:2,dristmechanic:tapebot:2,dristmechanic:tapebot:1",
                    "5:7:1:dristmechanic:totebot:1,dristmechanic:haybot:1",
                    "5:14:10:dristmechanic:totebot:2,dristmechanic:haybot:2",
                    "5:21:100:dristmechanic:totebot:3,dristmechanic:haybot:3",
                    "5:21:100:dristmechanic:totebot:3,dristmechanic:totebot:3",
                    "5:36:100:dristmechanic:totebot:3,dristmechanic:totebot:2",
                    "5:36:100:dristmechanic:totebot:3,dristmechanic:totebot:2",
                    "5:31:100:dristmechanic:totebot:3,dristmechanic:tapebot:3,dristmechanic:tapebot:2",
                    "5:56:80:dristmechanic:totebot:3,dristmechanic:tapebot:2",
                    "5:75:60:dristmechanic:farmbot:1",
                    "6:7:1:dristmechanic:totebot:1,dristmechanic:haybot:1",
                    "6:14:10:dristmechanic:totebot:2,dristmechanic:haybot:2",
                    "6:21:100:dristmechanic:totebot:3,dristmechanic:haybot:3",
                    "6:21:100:dristmechanic:totebot:3,dristmechanic:totebot:3",
                    "6:36:100:dristmechanic:totebot:3,dristmechanic:totebot:2",
                    "6:36:100:dristmechanic:totebot:3,dristmechanic:totebot:2",
                    "6:31:100:dristmechanic:totebot:3,dristmechanic:tapebot:3,dristmechanic:tapebot:2",
                    "6:56:80:dristmechanic:totebot:3,dristmechanic:tapebot:2",
                    "6:75:60:dristmechanic:farmbot:1",
                    "7:7:1:dristmechanic:totebot:1,dristmechanic:haybot:1",
                    "7:14:10:dristmechanic:totebot:2,dristmechanic:haybot:2",
                    "7:21:100:dristmechanic:totebot:3,dristmechanic:haybot:3",
                    "7:21:100:dristmechanic:totebot:3,dristmechanic:totebot:3",
                    "7:36:100:dristmechanic:totebot:3,dristmechanic:totebot:2",
                    "7:36:100:dristmechanic:totebot:3,dristmechanic:totebot:2",
                    "7:31:100:dristmechanic:totebot:3,dristmechanic:tapebot:3,dristmechanic:tapebot:2",
                    "7:56:80:dristmechanic:totebot:3,dristmechanic:tapebot:2",
                    "7:75:60:dristmechanic:farmbot:1"
            ), obj -> obj instanceof String);

    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
    }
}