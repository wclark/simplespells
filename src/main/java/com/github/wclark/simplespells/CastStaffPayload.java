package com.github.wclark.simplespells;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CastStaffPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CastStaffPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SimpleSpells.MODID, "cast_staff"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastStaffPayload> STREAM_CODEC = StreamCodec.unit(new CastStaffPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CastStaffPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            StaffCasting.cast(player);
        }
    }
}
