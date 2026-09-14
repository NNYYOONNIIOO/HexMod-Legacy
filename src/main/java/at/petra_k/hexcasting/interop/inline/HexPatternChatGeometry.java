package at.petra_k.hexcasting.interop.inline;

import at.petra_k.hexcasting.api.casting.math.HexAngle;
import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Lightweight immediate-mode pattern drawing for the 1.12.2 chat overlay. */
final class HexPatternChatGeometry {
    private HexPatternChatGeometry() {
    }

    static void draw(String signature, int x, int y) {
        HexPattern pattern = HexPattern.fromSignature(signature);
        List<Vec3d> points = new ArrayList<>();
        Vec3d cursor = Vec3d.ZERO;
        points.add(cursor);
        for (HexDir dir : pattern.directions()) {
            cursor = cursor.add(dir == HexDir.NORTH_EAST ? new Vec3d(6, -3, 0)
                : dir == HexDir.EAST ? new Vec3d(6, 0, 0)
                : dir == HexDir.SOUTH_EAST ? new Vec3d(0, 3, 0)
                : dir == HexDir.SOUTH_WEST ? new Vec3d(-6, 3, 0)
                : dir == HexDir.WEST ? new Vec3d(-6, 0, 0)
                : new Vec3d(0, -3, 0));
            points.add(cursor);
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 3, y + 8, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
        net.minecraft.client.renderer.Tessellator tess =
            net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.BufferBuilder buffer = tess.getBuffer();
        buffer.begin(1, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i + 1 < points.size(); i++) {
            Vec3d a = points.get(i);
            Vec3d b = points.get(i + 1);
            buffer.pos(a.x, a.y, 0).color(255, 255, 255, 255).endVertex();
            buffer.pos(b.x, b.y, 0).color(255, 255, 255, 255).endVertex();
        }
        tess.draw();
        buffer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < points.size(); i++) {
            Vec3d point = points.get(i);
            double radius = i == 0 ? 2.5D : 2.0D;
            buffer.pos(point.x - radius, point.y - radius, 0).color(255, 255, 255, 255).endVertex();
            buffer.pos(point.x - radius, point.y + radius, 0).color(255, 255, 255, 255).endVertex();
            buffer.pos(point.x + radius, point.y + radius, 0).color(255, 255, 255, 255).endVertex();
            buffer.pos(point.x + radius, point.y - radius, 0).color(255, 255, 255, 255).endVertex();
        }
        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
