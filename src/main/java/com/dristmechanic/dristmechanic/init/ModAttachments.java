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

    public static final Supplier<AttachmentType<Integer>> RAIDED_CROP_VALUE =
            ATTACHMENT_TYPES.register("raided_crop_value",
                    () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    public static final Supplier<AttachmentType<Long>> RAIDED_SUM_X =
            ATTACHMENT_TYPES.register("raided_sum_x",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final Supplier<AttachmentType<Long>> RAIDED_SUM_Y =
            ATTACHMENT_TYPES.register("raided_sum_y",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final Supplier<AttachmentType<Long>> RAIDED_SUM_Z =
            ATTACHMENT_TYPES.register("raided_sum_z",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final Supplier<AttachmentType<Long>> RAIDED_CROP_COUNT =
            ATTACHMENT_TYPES.register("raided_crop_count",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final Supplier<AttachmentType<Long>> NEW_SUM_X =
            ATTACHMENT_TYPES.register("new_sum_x",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final Supplier<AttachmentType<Long>> NEW_SUM_Y =
            ATTACHMENT_TYPES.register("new_sum_y",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final Supplier<AttachmentType<Long>> NEW_SUM_Z =
            ATTACHMENT_TYPES.register("new_sum_z",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final Supplier<AttachmentType<Long>> NEW_CROP_BLOCK_COUNT =
            ATTACHMENT_TYPES.register("new_crop_block_count",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final Supplier<AttachmentType<Boolean>> RAID_ACTIVE =
            ATTACHMENT_TYPES.register("raid_active",
                    () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    public static final Supplier<AttachmentType<Integer>> RAID_TOTAL_MOB_VALUE =
            ATTACHMENT_TYPES.register("raid_total_mob_value",
                    () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    public static final Supplier<AttachmentType<Integer>> RAID_KILLED_MOB_VALUE =
            ATTACHMENT_TYPES.register("raid_killed_mob_value",
                    () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    public static final Supplier<AttachmentType<String>> RAID_WAVES_DATA =
            ATTACHMENT_TYPES.register("raid_waves_data",
                    () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).build());

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}