package com.ura.dest.client;

import com.ura.dest.Dest;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Dest.MOD_ID, value = Dist.CLIENT)
public class VeinHUD {
    private static int previewCount = 0;

    public static void setPreviewCount(int count) {
        previewCount = count;
    }

    public static int getPreviewCount() {
        return previewCount;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (previewCount <= 0) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String text = "Quantity: " + previewCount;
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