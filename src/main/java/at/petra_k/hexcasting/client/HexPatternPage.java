package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.casting.math.HexCoord;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.ClientBookRegistry;
import vazkii.patchouli.client.book.BookEntry;

import java.util.List;

/**
 * The 1.12.2 Patchouli equivalent of Hex's custom pattern page.
 *
 * <p>Patchouli 1.0-23.6 does not ship the 1.20.1 {@code hexcasting:pattern}
 * page type, so the port has to provide it on the client.  The page resolves
 * the action id against the same Java registry used by the staff GUI; this
 * keeps the guide and the actual caster on one source of truth.</p>
 */
@SideOnly(Side.CLIENT)
public final class HexPatternPage extends BookPage {
    private String op_id;
    private String text;
    private String input;
    private String output;
    private int hex_size = 8;

    private transient HexPattern pattern;
    private transient String actionName;

    public static void register() {
        ClientBookRegistry.INSTANCE.pageTypes.put(
            "hexcasting:pattern", HexPatternPage.class);
    }

    @Override
    public void build(BookEntry entry, int pageNum) {
        super.build(entry, pageNum);
        pattern = null;
        actionName = op_id == null ? "" : op_id;
        try {
            HexActionRegistry.bootstrap();
            ResourceLocation id = op_id == null
                ? null : new ResourceLocation(op_id);
            if (id != null) {
                pattern = HexActionRegistry.getPattern(id);
                String key = "hexcasting.action." + id.getResourcePath();
                String translated = I18n.format(key);
                if (!key.equals(translated)) {
                    actionName = translated;
                } else {
                    actionName = id.getResourcePath();
                }
            }
        } catch (RuntimeException ignored) {
            // Keep the page readable even when an optional action is absent.
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (fontRenderer == null) {
            return;
        }
        int textColor = book == null ? 0x404040 : book.textColor;
        int headerColor = book == null ? 0x202020 : book.headerColor;
        FontRenderer font = fontRenderer;

        font.drawString(actionName == null ? "" : actionName,
            left + 5, top + 4, headerColor);
        if (pattern != null) {
            drawPattern(left + 64, top + 54);
        } else {
            font.drawString(I18n.format("hexcasting.gui.staff.unknown"),
                left + 8, top + 48, 0xAA3333);
        }

        int textTop = top + 96;
        if (input != null && !input.isEmpty()) {
            font.drawString("Input: " + input, left + 5, textTop, 0x666666);
            textTop += 10;
        }
        if (output != null && !output.isEmpty()) {
            font.drawString("Output: " + output, left + 5, textTop, 0x666666);
            textTop += 10;
        }
        if (text != null && !text.isEmpty()) {
            String translated = I18n.format(text);
            font.drawSplitString(translated, left + 5, textTop + 2,
                118, textColor);
        }
    }

    private void drawPattern(int centerX, int centerY) {
        List<HexCoord> positions = pattern.positions();
        if (positions == null || positions.size() < 2) {
            return;
        }

        float base = hex_size <= 0 ? 8.0F : (float) hex_size;
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (HexCoord point : positions) {
            float x = axialX(point, base);
            float y = axialY(point, base);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        float span = Math.max(maxX - minX, maxY - minY);
        float fit = span <= 1.0F ? 1.0F : Math.min(1.0F, 84.0F / span);
        float offsetX = (minX + maxX) * 0.5F;
        float offsetY = (minY + maxY) * 0.5F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(
            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.5F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (HexCoord point : positions) {
            float x = centerX + (axialX(point, base) - offsetX) * fit;
            float y = centerY + (axialY(point, base) - offsetY) * fit;
            buffer.pos(x, y, 0).color(112, 133, 222, 255).endVertex();
        }
        tessellator.draw();

        buffer.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);
        for (HexCoord point : positions) {
            float x = centerX + (axialX(point, base) - offsetX) * fit;
            float y = centerY + (axialY(point, base) - offsetY) * fit;
            buffer.pos(x, y, 0).color(254, 203, 230, 255).endVertex();
        }
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static float axialX(HexCoord point, float size) {
        return (float) ((1.7320508D * point.getQ()
            + 0.8660254D * point.getR()) * size);
    }

    private static float axialY(HexCoord point, float size) {
        return (float) (1.5D * point.getR() * size);
    }
}
