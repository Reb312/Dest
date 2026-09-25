package com.ura.dest.network;

import com.ura.dest.Dest;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Dest.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;

    public static void registerPackets() {
        CHANNEL.registerMessage(id++, KeyStatePacket.class,
                KeyStatePacket::encode,
                KeyStatePacket::decode,
                KeyStatePacket::handle);
    }
}