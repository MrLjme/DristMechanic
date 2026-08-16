package com.dristmechanic.dristmechanic.init;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

    public static final Supplier<AttachmentType<RaidHologramData>> RAID_HOLOGRAM =
            ATTACHMENT_TYPES.register("raid_hologram",
                    () -> AttachmentType.builder(RaidHologramData::new)
                            .serialize(RaidHologramData.CODEC)
                            .build());

    public static class RaidHologramData {
        public int currentValue = 0;
        public int maxValue = 0;
        public boolean raidActive = false;
        public boolean hasValue = false;

        public static final Codec<RaidHologramData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("currentValue").forGetter(d -> d.currentValue),
                        Codec.INT.fieldOf("maxValue").forGetter(d -> d.maxValue),
                        Codec.BOOL.fieldOf("raidActive").forGetter(d -> d.raidActive),
                        Codec.BOOL.fieldOf("hasValue").forGetter(d -> d.hasValue)
                ).apply(instance, RaidHologramData::new)
        );

        public RaidHologramData() {}

        public RaidHologramData(int currentValue, int maxValue, boolean raidActive, boolean hasValue) {
            this.currentValue = currentValue;
            this.maxValue = maxValue;
            this.raidActive = raidActive;
            this.hasValue = hasValue;
        }
    }
}