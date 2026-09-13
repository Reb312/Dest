package com.ura.dest.util;

import com.ura.dest.Dest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Dest.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KeyStateHolder {

    private static final Map<UUID, Boolean> PRESSED = new ConcurrentHashMap<>();

    private KeyStateHolder() {}

    public static void setPressed(ServerPlayer player, boolean pressed) {
        PRESSED.put(player.getUUID(), pressed);
    }

    public static boolean isPressed(ServerPlayer player) {
        return PRESSED.getOrDefault(player.getUUID(), false);
    }

    public static void remove(ServerPlayer player) {
        PRESSED.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            remove(player);
        }
    }
}