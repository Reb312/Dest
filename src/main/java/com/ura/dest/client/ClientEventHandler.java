package com.ura.dest.client;

import com.ura.dest.Config;
import com.ura.dest.Dest;
import com.ura.dest.network.KeyStatePacket;
import com.ura.dest.network.ModNetwork;
import com.ura.dest.util.VeinScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Dest.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {

    private static boolean lastVeinState = false;
    private static BlockPos lastTargetPos = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (mc.screen != null) {
            if (lastVeinState) {
                lastVeinState = false;
                ModNetwork.CHANNEL.sendToServer(new KeyStatePacket(false));
            }
            clearPreview();
            return;
        }

        boolean pressed = KeyBindings.VEINMINE_KEY.isDown();

        if (pressed != lastVeinState) {
            lastVeinState = pressed;
            ModNetwork.CHANNEL.sendToServer(new KeyStatePacket(pressed));
        }

        if (pressed) {
            if (mc.hitResult instanceof BlockHitResult blockHit) {
                BlockPos target = blockHit.getBlockPos();
                if (!target.equals(lastTargetPos)) {
                    lastTargetPos = target;
                    var state = mc.level.getBlockState(target);
                    if (!state.isAir()) {
                        int maxBlocks = Config.BLOCK_LIMIT.get();
                        var positions = VeinScanner.scan(mc.level, target, state, maxBlocks);
                        // 数量与轮廓使用同一个列表，保证一致
                        VeinHUD.setPreview(positions.size(), positions);
                    } else {
                        VeinHUD.setPreviewCount(0);
                    }
                }
            } else if (lastTargetPos != null) {
                clearPreview();
            }
        } else if (lastTargetPos != null || VeinHUD.getPreviewCount() > 0) {
            clearPreview();
        }
    }

    private static void clearPreview() {
        lastTargetPos = null;
        VeinHUD.setPreviewCount(0);
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        resetState();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        resetState();
    }

    private static void resetState() {
        lastVeinState = false;
        lastTargetPos = null;
        VeinHUD.setPreviewCount(0);
    }
}