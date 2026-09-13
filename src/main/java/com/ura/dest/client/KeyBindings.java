package com.ura.dest.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ura.dest.Dest;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Dest.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyBindings {
    public static final String KEY_CATEGORY = "key.dest.dest";
    public static final String KEY_VEINMINE = "key.dest.key";

    public static final KeyMapping VEINMINE_KEY = new KeyMapping(
            KEY_VEINMINE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(VEINMINE_KEY);
    }
}