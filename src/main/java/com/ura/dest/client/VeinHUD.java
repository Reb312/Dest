package com.ura.dest.client;

import com.ura.dest.Dest;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = Dest.MOD_ID, value = Dist.CLIENT)
public class VeinHUD {
    private static int previewCount = 0;
    private static List<BlockPos> previewPositions = Collections.emptyList();

    /** 同时设置数量与轮廓位置 */
    public static void setPreview(int count, List<BlockPos> positions) {
        previewCount = count;
        previewPositions = (positions == null || positions.isEmpty())
                ? Collections.emptyList()
                : positions;
    }

    /** 只设置数量（会清空轮廓，用于无有效目标时） */
    public static void setPreviewCount(int count) {
        previewCount = count;
        previewPositions = Collections.emptyList();
    }

    public static int getPreviewCount() {
        return previewCount;
    }

    public static List<BlockPos> getPreviewPositions() {
        return previewPositions;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (previewCount <= 0) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.options.hideGui) return; // F1 隐藏 GUI 时同样隐藏

        String text = Component.translatable("hud.dest.quantity", previewCount).getString();
        float scale = 0.7f;

        var poseStack = event.getGuiGraphics().pose();
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);

        int textWidth = mc.font.width(text);
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = (int) ((screenWidth / 2f - textWidth * scale / 2) / scale);
        int y = (int) ((screenHeight / 2f + 5) / scale);

        event.getGuiGraphics().drawString(mc.font, text, x, y, 0xFFFFFF);

        poseStack.popPose();
    }
}