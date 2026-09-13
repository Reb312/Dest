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
                        VeinHUD.setPreviewCount(positions.size());
                    } else {
                        VeinHUD.setPreviewCount(0);
                    }
                }
            } else if (lastTargetPos != null) {
                lastTargetPos = null;
                VeinHUD.setPreviewCount(0);
            }
        } else if (lastTargetPos != null || VeinHUD.getPreviewCount() > 0) {
            lastTargetPos = null;
            VeinHUD.setPreviewCount(0);
        }
    }
}