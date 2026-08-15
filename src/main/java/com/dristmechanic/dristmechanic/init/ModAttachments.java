package com.dristmechanic.dristmechanic.init;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Dristmechanic.MODID);

    public static final Supplier<AttachmentType<Map<String, Long>>> RAW_CROPS =
            ATTACHMENT_TYPES.register("raw_crops",
                    () -> AttachmentType.builder((Supplier<Map<String, Long>>) HashMap::new)
                            .serialize(Codec.unboundedMap(Codec.STRING, Codec.LONG))
                            .build());

    public static final Supplier<AttachmentType<Long>> LOCKED_UNTIL =
            ATTACHMENT_TYPES.register("locked_until",
                    () -> AttachmentType.builder(() -> 0L)
                            .serialize(Codec.LONG)
                            .build());

    public static final Supplier<AttachmentType<Integer>> RAID_VALUE =
            ATTACHMENT_TYPES.register("raid_value",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .build());

    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}