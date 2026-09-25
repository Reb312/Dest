package com.ura.dest.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.ura.dest.Config;
import com.ura.dest.Dest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Dest.MOD_ID, value = Dist.CLIENT)
public class VeinPreviewRenderer {

    private static final float ALPHA = 1.0f;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        List<BlockPos> positions = VeinHUD.getPreviewPositions();
        if (positions.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        float[] rgb = parseColor(Config.CONTOUR_COLOR.get());
        float r = rgb[0], g = rgb[1], b = rgb[2];
        float thickness = (float) (double) Config.CONTOUR_THICKNESS.get();

        Set<BlockPos> selected = new HashSet<>(positions);
        Vec3 cam = event.getCamera().getPosition();
        double camX = cam.x, camY = cam.y, camZ = cam.z;

        List<Line> lines = generateExposedEdges(selected);
        if (lines.isEmpty()) return;

        // ===== 手动渲染状态：完全绕过 RenderType 系统 =====
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();   // 穿透关键
        RenderSystem.depthMask(false);     // 不写深度

        Matrix4f pose = event.getPoseStack().last().pose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Line line : lines) {
            addEdgeBox(builder, pose,
                    line.x1 - camX, line.y1 - camY, line.z1 - camZ,
                    line.x2 - camX, line.y2 - camY, line.z2 - camZ,
                    r, g, b, ALPHA, thickness);
        }

        tesselator.end();

        // 恢复状态
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void addEdgeBox(VertexConsumer c, Matrix4f pose,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2,
                                   float r, float g, float b, float a, float t) {
        double minX, minY, minZ, maxX, maxY, maxZ;

        if (Math.abs(y2 - y1) > 0.001) {
            double px = (x1 + x2) * 0.5, pz = (z1 + z2) * 0.5;
            minX = px - t; maxX = px + t;
            minY = Math.min(y1, y2); maxY = Math.max(y1, y2);
            minZ = pz - t; maxZ = pz + t;
        } else if (Math.abs(x2 - x1) > 0.001) {
            double py = (y1 + y2) * 0.5, pz = (z1 + z2) * 0.5;
            minX = Math.min(x1, x2); maxX = Math.max(x1, x2);
            minY = py - t; maxY = py + t;
            minZ = pz - t; maxZ = pz + t;
        } else {
            double px = (x1 + x2) * 0.5, py = (y1 + y2) * 0.5;
            minX = px - t; maxX = px + t;
            minY = py - t; maxY = py + t;
            minZ = Math.min(z1, z2); maxZ = Math.max(z1, z2);
        }

        addQuad(c, pose, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        addQuad(c, pose, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);
        addQuad(c, pose, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
        addQuad(c, pose, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, r, g, b, a);
        addQuad(c, pose, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, r, g, b, a);
        addQuad(c, pose, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
    }

    private static void addQuad(VertexConsumer c, Matrix4f pose,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                double x3, double y3, double z3,
                                double x4, double y4, double z4,
                                float r, float g, float b, float a) {
        c.vertex(pose, (float) x1, (float) y1, (float) z1).color(r, g, b, a).endVertex();
        c.vertex(pose, (float) x2, (float) y2, (float) z2).color(r, g, b, a).endVertex();
        c.vertex(pose, (float) x3, (float) y3, (float) z3).color(r, g, b, a).endVertex();
        c.vertex(pose, (float) x4, (float) y4, (float) z4).color(r, g, b, a).endVertex();
    }

    /** 照搬 Liteminer 的暴露边线生成 */
    private static List<Line> generateExposedEdges(Set<BlockPos> selected) {
        List<Line> lines = new ArrayList<>();
        for (BlockPos pos : selected) {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            boolean west  = !selected.contains(pos.relative(Direction.WEST));
            boolean east  = !selected.contains(pos.relative(Direction.EAST));
            boolean down  = !selected.contains(pos.relative(Direction.DOWN));
            boolean up    = !selected.contains(pos.relative(Direction.UP));
            boolean north = !selected.contains(pos.relative(Direction.NORTH));
            boolean south = !selected.contains(pos.relative(Direction.SOUTH));

            if (west && north) lines.add(new Line(x, y, z, x, y + 1, z));
            if (east && north) lines.add(new Line(x + 1, y, z, x + 1, y + 1, z));
            if (west && south) lines.add(new Line(x, y, z + 1, x, y + 1, z + 1));
            if (east && south) lines.add(new Line(x + 1, y, z + 1, x + 1, y + 1, z + 1));

            if (west && down) lines.add(new Line(x, y, z, x, y, z + 1));
            if (east && down) lines.add(new Line(x + 1, y, z, x + 1, y, z + 1));
            if (west && up)   lines.add(new Line(x, y + 1, z, x, y + 1, z + 1));
            if (east && up)   lines.add(new Line(x + 1, y + 1, z, x + 1, y + 1, z + 1));

            if (north && down) lines.add(new Line(x, y, z, x + 1, y, z));
            if (south && down) lines.add(new Line(x, y, z + 1, x + 1, y, z + 1));
            if (north && up)   lines.add(new Line(x, y + 1, z, x + 1, y + 1, z));
            if (south && up)   lines.add(new Line(x, y + 1, z + 1, x + 1, y + 1, z + 1));
        }
        return lines;
    }

    private record Line(double x1, double y1, double z1, double x2, double y2, double z2) {}

    private static float[] parseColor(String hex) {
        if (hex == null) return new float[]{1f, 1f, 1f};
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 3) {
            s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
        }
        if (s.length() != 6) return new float[]{1f, 1f, 1f};
        try {
            int rgb = Integer.parseInt(s, 16);
            return new float[]{
                    ((rgb >> 16) & 0xFF) / 255f,
                    ((rgb >> 8) & 0xFF) / 255f,
                    (rgb & 0xFF) / 255f
            };
        } catch (NumberFormatException e) {
            return new float[]{1f, 1f, 1f};
        }
    }
}