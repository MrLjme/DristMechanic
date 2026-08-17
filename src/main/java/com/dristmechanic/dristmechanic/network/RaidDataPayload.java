package com.dristmechanic.dristmechanic.network;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RaidDataPayload(
        int farmX,
        int farmZ,
        double centerX,
        double centerZ,
        boolean active,
        int currentValue,
        int maxValue,
        boolean raidActive
) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "raid_data");
    public static final Type<RaidDataPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, RaidDataPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public RaidDataPayload decode(RegistryFriendlyByteBuf buf) {
                    return new RaidDataPayload(
                            buf.readInt(),
                            buf.readInt(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readBoolean(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readBoolean()
                    );
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, RaidDataPayload p) {
                    buf.writeInt(p.farmX());
                    buf.writeInt(p.farmZ());
                    buf.writeDouble(p.centerX());
                    buf.writeDouble(p.centerZ());
                    buf.writeBoolean(p.active());
                    buf.writeInt(p.currentValue());
                    buf.writeInt(p.maxValue());
                    buf.writeBoolean(p.raidActive());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RaidDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                com.dristmechanic.dristmechanic.client.gui.RaidHUD.updateData(
                        payload.farmX(), payload.farmZ(),
                        payload.centerX(), payload.centerZ(),
                        payload.active(), payload.currentValue(),
                        payload.maxValue(), payload.raidActive()
                )
        );
    }
}