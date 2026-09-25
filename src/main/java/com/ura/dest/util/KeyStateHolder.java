package com.ura.dest.util;

import com.ura.dest.Dest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Dest.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KeyStateHolder {

    private static final Set<UUID> PRESSED = ConcurrentHashMap.newKeySet();

    private KeyStateHolder() {}

    public static void setPressed(ServerPlayer player, boolean pressed) {
        if (pressed) {
            PRESSED.add(player.getUUID());
        } else {
            PRESSED.remove(player.getUUID());
        }
    }

    public static boolean isPressed(ServerPlayer player) {
        return PRESSED.contains(player.getUUID());
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