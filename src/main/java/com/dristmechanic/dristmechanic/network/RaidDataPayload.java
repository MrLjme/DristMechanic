package com.dristmechanic.dristmechanic.network;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RaidDataPayload(boolean active, int currentValue, int maxValue, boolean raidActive) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "raid_data");

    public static final CustomPacketPayload.Type<RaidDataPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RaidDataPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RaidDataPayload::active,
            ByteBufCodecs.INT, RaidDataPayload::currentValue,
            ByteBufCodecs.INT, RaidDataPayload::maxValue,
            ByteBufCodecs.BOOL, RaidDataPayload::raidActive,
            RaidDataPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaidDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            com.dristmechanic.dristmechanic.client.gui.RaidHUD.updateData(
                    payload.active(),
                    payload.currentValue(),
                    payload.maxValue(),
                    payload.raidActive()
            );
        });
    }
}