package com.ura.dest.network;

import com.ura.dest.util.KeyStateHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KeyStatePacket(boolean pressed) {
    public static void encode(KeyStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.pressed);
    }

    public static KeyStatePacket decode(FriendlyByteBuf buf) {
        return new KeyStatePacket(buf.readBoolean());
    }

    public static void handle(KeyStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                KeyStateHolder.setPressed(player, msg.pressed);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}