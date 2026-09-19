package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.entity.EntityWallScroll;
import at.petra_k.hexcasting.interop.inline.HexPatternChatGeometry;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/** 1.12.2 renderer for a wall-mounted pattern scroll. */
@SideOnly(Side.CLIENT)
public final class RenderWallScroll extends Render<EntityWallScroll> {
    private static final ResourceLocation PRISTINE_LARGE =
        new ResourceLocation("hexcasting", "textures/entity/scroll_large.png");
    private static final ResourceLocation PRISTINE_MEDIUM =
        new ResourceLocation("hexcasting", "textures/entity/scroll_medium.png");
    private static final ResourceLocation PRISTINE_SMALL =
        new ResourceLocation("hexcasting", "textures/blocks/scroll_paper.png");
    private static final ResourceLocation ANCIENT_LARGE =
        new ResourceLocation("hexcasting", "textures/entity/scroll_ancient_large.png");
    private static final ResourceLocation ANCIENT_MEDIUM =
        new ResourceLocation("hexcasting", "textures/entity/scroll_ancient_medium.png");
    private static final ResourceLocation ANCIENT_SMALL =
        new ResourceLocation("hexcasting", "textures/blocks/ancient_scroll_paper.png");

    public RenderWallScroll(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntityWallScroll entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(0.0625F, 0.0625F, 0.0625F);

        int size = entity.getBlockSize() * 16;
        bindEntityTexture(entity);
        drawQuad(size, -0.52D);

        if (entity.getPattern() != null) {
            GlStateManager.translate(0.0D, 0.0D, -0.03D);
            HexPatternChatGeometry.drawPreview(entity.getPattern(),
                -size / 2, -size / 2, size, 255,
                0xC80C0A0C, 0xFF333030,
                entity.getShowsStrokeOrder());
        }

        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private void drawQuad(int size, double depth) {
        float half = size / 2.0F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);
        buffer.pos(-half, -half, depth).tex(0.0D, 1.0D)
            .normal(0.0F, 0.0F, -1.0F).endVertex();
        buffer.pos(half, -half, depth).tex(1.0D, 1.0D)
            .normal(0.0F, 0.0F, -1.0F).endVertex();
        buffer.pos(half, half, depth).tex(1.0D, 0.0D)
            .normal(0.0F, 0.0F, -1.0F).endVertex();
        buffer.pos(-half, half, depth).tex(0.0D, 0.0D)
            .normal(0.0F, 0.0F, -1.0F).endVertex();
        tessellator.draw();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityWallScroll entity) {
        if (entity.isAncient()) {
            if (entity.getBlockSize() <= 1) {
                return ANCIENT_SMALL;
            }
            return entity.getBlockSize() == 2 ? ANCIENT_MEDIUM : ANCIENT_LARGE;
        }
        if (entity.getBlockSize() <= 1) {
            return PRISTINE_SMALL;
        }
        return entity.getBlockSize() == 2 ? PRISTINE_MEDIUM : PRISTINE_LARGE;
    }
}
