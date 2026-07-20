package com.dristmechanic.dristmechanic.init;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Registry for chunk data attachments
 */
public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Dristmechanic.MODID);

    /**
     * Stores the weighted crop score for each chunk
     */
    public static final Supplier<AttachmentType<Integer>> CROP_COUNT =
            ATTACHMENT_TYPES.register("crop_count",
                    () -> AttachmentType.builder(() -> 0)
                            .build());

    /**
     * Registers attachment types to the mod event bus
     */
    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
