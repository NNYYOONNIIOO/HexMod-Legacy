package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.interop.inline.HexPatternChatGeometry;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Shared face transform for patterns rendered by block entities. */
@SideOnly(Side.CLIENT)
final class HexWorldPatternRenderer {
    private static final int PREVIEW_SIZE = 128;
    private static final double FACE_OFFSET = 0.003D;
    private static final int DEFAULT_OUTER = 0xFFD2C8C8;
    private static final int DEFAULT_INNER = 0xFF554D54;
    private static final int ENERGIZED_OUTER = 0xFFCFA0F3;
    private static final int ENERGIZED_INNER = 0xFF9B68C2;

    private HexWorldPatternRenderer() {
    }

    static void render(HexPattern pattern, EnumFacing facing, boolean energized) {
        if (pattern == null || facing == null) {
            return;
        }

        GlStateManager.pushMatrix();
        transformToFace(facing);
        GlStateManager.scale(1.0D / PREVIEW_SIZE, 1.0D / PREVIEW_SIZE,
            1.0D / PREVIEW_SIZE);
        HexPatternChatGeometry.drawWorldPreview(pattern, -PREVIEW_SIZE / 2,
            -PREVIEW_SIZE / 2, PREVIEW_SIZE, 255,
            energized ? ENERGIZED_OUTER : DEFAULT_OUTER,
            energized ? ENERGIZED_INNER : DEFAULT_INNER, false);
        GlStateManager.popMatrix();
    }

    private static void transformToFace(EnumFacing facing) {
        switch (facing) {
            case DOWN:
                GlStateManager.translate(0.5D, -FACE_OFFSET, 0.5D);
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                break;
            case UP:
                GlStateManager.translate(0.5D, 1.0D + FACE_OFFSET, 0.5D);
                GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                break;
            case NORTH:
                GlStateManager.translate(0.5D, 0.5D, -FACE_OFFSET);
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                break;
            case SOUTH:
                GlStateManager.translate(0.5D, 0.5D, 1.0D + FACE_OFFSET);
                break;
            case WEST:
                GlStateManager.translate(-FACE_OFFSET, 0.5D, 0.5D);
                GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                break;
            case EAST:
            default:
                GlStateManager.translate(1.0D + FACE_OFFSET, 0.5D, 0.5D);
                GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                break;
        }
    }
}
