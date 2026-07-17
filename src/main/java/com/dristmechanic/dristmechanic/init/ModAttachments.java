package com.dristmechanic.dristmechanic.init;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Dristmechanic.MODID);

    public static final Supplier<AttachmentType<Integer>> CROP_COUNT = ATTACHMENT_TYPES.register(
            "crop_count",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build()
    );
}