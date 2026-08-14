package com.dristmechanic.dristmechanic.network;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RaidHudPayload(boolean isActive, int currentValue, int maxValue) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RaidHudPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "raid_hud"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RaidHudPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RaidHudPayload decode(RegistryFriendlyByteBuf buffer) {
            return new RaidHudPayload(buffer.readBoolean(), buffer.readInt(), buffer.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RaidHudPayload payload) {
            buffer.writeBoolean(payload.isActive());
            buffer.writeInt(payload.currentValue());
            buffer.writeInt(payload.maxValue());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}