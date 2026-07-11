package com.dristmechanic.dristmechanic;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==========================================
    // ЗДЕСЬ ДОБАВЛЯЙТЕ СВОИ ПАРАМЕТРЫ КОНФИГА
    // ==========================================
    /*
    Пример добавления настроек с категорией:

    BUILDER.push("general");
        public static final ModConfigSpec.BooleanValue ENABLE_EXAMPLE_FEATURE = BUILDER
                .comment("Включить пример фичи")
                .define("enableExampleFeature", true);
    BUILDER.pop();
    */

    static final ModConfigSpec SPEC = BUILDER.build();

    // ==========================================
    // ЗДЕСЬ ОБЪЯВЛЯЙТЕ СТАТИЧЕСКИЕ ПОЛЯ ДЛЯ КЭШИРОВАНИЯ
    // ==========================================
    /*
    Используйте volatile для потокобезопасности при перезагрузке конфига!
    public static volatile boolean exampleFeature;
    */

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Здесь присваивайте значения из конфига вашим кэшированным полям
        // exampleFeature = ENABLE_EXAMPLE_FEATURE.get();
    }
}