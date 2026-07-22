package com.dristmechanic.dristmechanic.init;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Dristmechanic.MODID);

    public static final Supplier<AttachmentType<Integer>> CROP_COUNT =
            ATTACHMENT_TYPES.register("crop_count",
                    () -> AttachmentType.builder(() -> 0).build());

    public static final Supplier<AttachmentType<Long>> LAST_CHANGE_TICK =
            ATTACHMENT_TYPES.register("last_change_tick",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final Supplier<AttachmentType<Long>> SUM_X =
            ATTACHMENT_TYPES.register("sum_x",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final Supplier<AttachmentType<Long>> SUM_Y =
            ATTACHMENT_TYPES.register("sum_y",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final Supplier<AttachmentType<Long>> SUM_Z =
            ATTACHMENT_TYPES.register("sum_z",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final Supplier<AttachmentType<Long>> CROP_BLOCK_COUNT =
            ATTACHMENT_TYPES.register("crop_block_count",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}