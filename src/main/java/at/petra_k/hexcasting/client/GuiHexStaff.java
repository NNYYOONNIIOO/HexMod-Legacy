package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.math.HexAngle;
import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.common.network.MsgStaffPatternC2S;
import at.petrak.paucal.api.PaucalAPI;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hex's transparent spell-drawing overlay, adapted from the 1.20.1
 * GuiSpellcasting/PatternRenderer flow for the 1.12.2 GuiScreen API.
 */
public final class GuiHexStaff extends GuiScreen {
    private static final int HEX_SIZE = 42;
    private static final int GUIDE_RADIUS = 3;
    private static final double SNAP_DISTANCE_FACTOR = 2.0D;

    private final EnumHand hand;
    private final List<ResourceLocation> programIds = new ArrayList<>();
    private final List<DrawnPath> drawnPaths = new ArrayList<>();
    private final List<HexPattern> savedPatterns = new ArrayList<>();
    private final List<GridPoint> savedOrigins = new ArrayList<>();
    private final Set<GridPoint> usedSpots = new HashSet<>();
    private final List<GridPoint> currentPoints = new ArrayList<>();

    private boolean drawing;
    private GridPoint current;
    private HexPattern workingPattern;
    private String status = "";
    private int programCount;

    public GuiHexStaff(EnumHand hand) {
        this.hand = hand == null ? EnumHand.MAIN_HAND : hand;
    }

    @Override
    public void initGui() {
        refreshProgram();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();

        // This is the same order as Hex's renderer: the cursor guide is a
        // separate, distance-faded layer; paths and their connection nodes are
        // rendered above it.
        drawGuideSpots(mouseX, mouseY);
        drawExistingPaths();
        drawWorkingPath(mouseX, mouseY);

        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        if (!status.isEmpty()) {
            drawCenteredString(fontRenderer, status, width / 2, height - 18, 0xFFFFD0D0);
        }
    }

    private void drawGuideSpots(int mouseX, int mouseY) {
        GridPoint mouseCoord = pxToCoord(mouseX, mouseY);
        for (int q = -GUIDE_RADIUS; q <= GUIDE_RADIUS; q++) {
            for (int r = -GUIDE_RADIUS; r <= GUIDE_RADIUS; r++) {
                if (hexDistance(q, r) > GUIDE_RADIUS) {
                    continue;
                }
                GridPoint point = mouseCoord.add(q, r);
                if (usedSpots.contains(point) || currentPoints.contains(point)) {
                    continue;
                }
                int[] pixel = coordToPx(point);
                double dx = pixel[0] - mouseX;
                double dy = pixel[1] - mouseY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                double scaledDistance = clamp(
                    1.0D - ((distance - HEX_SIZE) / (GUIDE_RADIUS * (double) HEX_SIZE)),
                    0.0D, 1.0D);
                // Hex's guide spots are dynamic position-colour geometry.  Keep
                // the source colour (0x64c8ff) and apply the same distance fade.
                drawSpot(pixel[0], pixel[1], (float) (scaledDistance * 2.0D),
                    lerp(scaledDistance, 0.40D, 0.50D),
                    lerp(scaledDistance, 0.80D, 1.00D),
                    lerp(scaledDistance, 0.70D, 0.90D),
                    (float) scaledDistance);
            }
        }
    }

    private void drawSpot(int x, int y, float size,
                          double red, double green, double blue, float alpha) {
        float visibility = Math.max(0.0F, Math.min(1.0F, alpha));
        if (visibility <= 0.01F) {
            return;
        }
        float coreRadius = 1.25F + Math.min(2.5F, size * 0.9F);
        float glowRadius = coreRadius + 2.0F + 2.5F * visibility;
        drawCircle(x, y, glowRadius,
            color(red, green, blue, visibility * 0.30F),
            color(red, green, blue, 0.0F));
        drawCircle(x, y, coreRadius,
            color(red, green, blue, visibility * 0.96F),
            color(red, green, blue, visibility * 0.22F));
        drawCircle(x, y, Math.max(1.0F, coreRadius * 0.40F),
            color(0.82D, 1.0D, 1.0D, visibility),
            color(0.82D, 1.0D, 1.0D, visibility * 0.20F));
    }

    private void drawExistingPaths() {
        for (DrawnPath path : drawnPaths) {
            drawPath(path.points, 0xFF64C8FF, 0xFFFECBE6, 0xFFFECBE6);
        }
    }

    private void drawWorkingPath(int mouseX, int mouseY) {
        if (currentPoints.isEmpty()) {
            return;
        }
        // Hex renders the snapped path and the unsnapped cursor as one
        // continuous preview. The cursor segment is important both for
        // readable connections and for seeing the next snapped direction.
        drawPath(currentPoints, 0xFF64C8FF, 0xFFFECBE6, 0xFFFECBE6,
            mouseX, mouseY, drawing && current != null);
    }

    private void drawPath(List<GridPoint> points, int glowColor,
                          int lineColor, int nodeColor) {
        drawPath(points, glowColor, lineColor, nodeColor, -1, -1, false);
    }

    private void drawPath(List<GridPoint> points, int glowColor,
                          int lineColor, int nodeColor,
                          int cursorX, int cursorY, boolean includeCursor) {
        if (points.isEmpty()) {
            return;
        }
        List<float[]> pixelPoints = new ArrayList<>();
        for (GridPoint point : points) {
            int[] pixel = coordToPx(point);
            pixelPoints.add(new float[] {pixel[0], pixel[1]});
        }
        int nodeCount = pixelPoints.size();
        if (includeCursor) {
            pixelPoints.add(new float[] {cursorX, cursorY});
        }
        if (pixelPoints.size() > 1) {
            // RenderLib.drawPatternFromPoints expands segments with makeZappy
            // before the 5 px outer and 2 px inner drawLineSeq passes.
            List<float[]> zappyPoints = makeZappyPoints(pixelPoints, points.size());
            // Hex's blue outer ribbon is surrounded by a soft additive-looking
            // halo before the narrow blue ribbon and pink readability core.
            drawLineSequence(zappyPoints, 8.0F,
                withAlpha(glowColor, 0x58), withAlpha(glowColor, 0x58));
            drawLineSequence(zappyPoints, 5.0F, glowColor, glowColor);
            drawLineSequence(zappyPoints, 2.0F, lineColor, lineColor);
        }
        for (int i = 0; i < nodeCount; i++) {
            float[] pixel = pixelPoints.get(i);
            if (i == nodeCount - 1 && nodeCount > 1
                && samePixel(pixel, pixelPoints.get(0))) {
                continue;
            }
            drawConnectionSpot(pixel[0], pixel[1], glowColor, nodeColor);
        }
    }

    /**
     * Port of RenderLib.drawLineSeq's continuous triangle strip/fan geometry.
     * The 1.12.2 BufferBuilder has no Matrix4f vertex helper, so the screen
     * coordinates are supplied directly while retaining the source join math.
     */
    private static List<float[]> makeZappyPoints(List<float[]> points, int seedSalt) {
        if (points.size() < 2) {
            return points;
        }
        List<float[]> result = new ArrayList<>();
        result.add(new float[] {points.get(0)[0], points.get(0)[1]});
        long seed = 0x5DEECE66DL ^ (long) seedSalt * 0x9E3779B97F4A7C15L;
        for (int i = 0; i < points.size() - 1; i++) {
            float[] from = points.get(i);
            float[] to = points.get(i + 1);
            double dx = to[0] - from[0];
            double dy = to[1] - from[1];
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length < 0.001D) {
                continue;
            }
            for (int hop = 1; hop < 10; hop++) {
                float t = hop / 10.0F;
                double envelope = Math.sin(Math.PI * t);
                long hopSeed = seed ^ (long) i * 0xBF58476D1CE4E5B9L
                    ^ (long) hop * 0x94D049BB133111EBL;
                double offset = zappyNoise(hopSeed) * 2.5D * envelope;
                result.add(new float[] {
                    (float) (from[0] + dx * t - dy / length * offset),
                    (float) (from[1] + dy * t + dx / length * offset)
                });
            }
            result.add(new float[] {to[0], to[1]});
        }
        return result;
    }

    private static double zappyNoise(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return ((value >>> 11) / (double) (1L << 53)) * 2.0D - 1.0D;
    }


    private void drawLineSequence(List<float[]> points, float width,
                                  int tailColor, int headColor) {
        if (points.size() < 2 || width <= 0.0F) {
            return;
        }
        final float halfWidth = width * 0.5F;
        final int count = points.size();
        final boolean closed = count > 2 && samePixel(points.get(0), points.get(count - 1));
        float[] normalsX = new float[count - 1];
        float[] normalsY = new float[count - 1];
        for (int i = 0; i < count - 1; i++) {
            float dx = points.get(i + 1)[0] - points.get(i)[0];
            float dy = points.get(i + 1)[1] - points.get(i)[1];
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 0.001F) {
                return;
            }
            normalsX[i] = -dy / length;
            normalsY[i] = dx / length;
        }

        int tailA = channel(tailColor, 24);
        int tailR = channel(tailColor, 16);
        int tailG = channel(tailColor, 8);
        int tailB = channel(tailColor, 0);
        int headA = channel(headColor, 24);
        int headR = channel(headColor, 16);
        int headG = channel(headColor, 8);
        int headB = channel(headColor, 0);

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < count - 1; i++) {
            float[] from = points.get(i);
            float[] to = points.get(i + 1);
            float[] startOffset = lineOffset(points, i, normalsX, normalsY, halfWidth);
            float[] endOffset = lineOffset(points, i + 1, normalsX, normalsY, halfWidth);

            float sx = from[0] + startOffset[0];
            float sy = from[1] + startOffset[1];
            float slx = from[0] - startOffset[0];
            float sly = from[1] - startOffset[1];
            float ex = to[0] + endOffset[0];
            float ey = to[1] + endOffset[1];
            float elx = to[0] - endOffset[0];
            float ely = to[1] - endOffset[1];

            putColorVertex(buffer, sx, sy, tailR, tailG, tailB, tailA);
            putColorVertex(buffer, slx, sly, tailR, tailG, tailB, tailA);
            putColorVertex(buffer, ex, ey, headR, headG, headB, headA);
            putColorVertex(buffer, slx, sly, tailR, tailG, tailB, tailA);
            putColorVertex(buffer, elx, ely, headR, headG, headB, headA);
            putColorVertex(buffer, ex, ey, headR, headG, headB, headA);
        }
        tessellator.draw();

        // RenderLib uses triangle fans for caps and rounded/miter joins. The
        // six-sided spot pass below supplies the visible node, while these
        // small fans remove the gaps at sharp corners of the ribbon.
        for (int i = 1; i < count - 1; i++) {
            drawJoinFan(points.get(i)[0], points.get(i)[1], halfWidth,
                tailColor, normalsX[i - 1], normalsY[i - 1], normalsX[i], normalsY[i]);
        }
        if (closed) {
            int last = normalsX.length - 1;
            drawJoinFan(points.get(0)[0], points.get(0)[1], halfWidth,
                tailColor, normalsX[last], normalsY[last], normalsX[0], normalsY[0]);
        } else {
            drawCapFan(points.get(0)[0], points.get(0)[1], halfWidth,
                tailColor, normalsX[0], normalsY[0], false);
            drawCapFan(points.get(count - 1)[0], points.get(count - 1)[1], halfWidth,
                headColor, normalsX[count - 2], normalsY[count - 2], true);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /**
     * Returns the signed miter vector at one vertex. This is the 2-D
     * equivalent of RenderLib's joinAngles/joinOffsets calculation: the two
     * adjacent normals are merged, then the miter is clamped so a near-180°
     * turn cannot create an unbounded spike.
     */
    private static float[] lineOffset(List<float[]> points, int index,
                                      float[] normalsX, float[] normalsY,
                                      float halfWidth) {
        boolean closed = points.size() > 2 && samePixel(points.get(0), points.get(points.size() - 1));
        if (closed && (index == 0 || index == points.size() - 1)) {
            int last = normalsX.length - 1;
            return miterOffset(normalsX[last], normalsY[last], normalsX[0], normalsY[0], halfWidth);
        }
        if (index <= 0) {
            return new float[] {normalsX[0] * halfWidth, normalsY[0] * halfWidth};
        }
        if (index >= points.size() - 1) {
            int last = normalsX.length - 1;
            return new float[] {normalsX[last] * halfWidth, normalsY[last] * halfWidth};
        }

        float mx = normalsX[index - 1] + normalsX[index];
        float my = normalsY[index - 1] + normalsY[index];
        float miterLength = (float) Math.sqrt(mx * mx + my * my);
        if (miterLength < 0.001F) {
            return new float[] {normalsX[index] * halfWidth, normalsY[index] * halfWidth};
        }

        float miterX = mx / miterLength;
        float miterY = my / miterLength;
        float denominator = miterX * normalsY[index] - miterY * normalsX[index];
        if (Math.abs(denominator) < 0.25F) {
            return new float[] {normalsX[index] * halfWidth, normalsY[index] * halfWidth};
        }
        float scale = Math.min(halfWidth * 2.5F, Math.abs(halfWidth / denominator));
        float turn = normalsX[index - 1] * normalsY[index]
            - normalsY[index - 1] * normalsX[index];
        if (turn < 0.0F) {
            scale = -scale;
        }
        return new float[] {miterX * scale, miterY * scale};
    }

    private static float[] miterOffset(float previousX, float previousY,
                                       float nextX, float nextY, float halfWidth) {
        float miterX = previousX + nextX;
        float miterY = previousY + nextY;
        float length = (float) Math.sqrt(miterX * miterX + miterY * miterY);
        if (length < 0.001F) {
            return new float[] {nextX * halfWidth, nextY * halfWidth};
        }
        miterX /= length;
        miterY /= length;
        float denominator = miterX * nextX + miterY * nextY;
        if (Math.abs(denominator) < 0.25F) {
            return new float[] {nextX * halfWidth, nextY * halfWidth};
        }
        float scale = halfWidth / denominator;
        scale = Math.max(-halfWidth * 2.5F, Math.min(halfWidth * 2.5F, scale));
        return new float[] {miterX * scale, miterY * scale};
    }

    private void drawJoinFan(float x, float y, float radius, int color,
                             float previousNormalX, float previousNormalY,
                             float nextNormalX, float nextNormalY) {
        drawFan(x, y, radius, color, previousNormalX, previousNormalY,
            nextNormalX, nextNormalY, false);
    }

    private void drawCapFan(float x, float y, float radius, int color,
                            float normalX, float normalY, boolean end) {
        drawFan(x, y, radius, color, normalX, normalY, -normalX, -normalY, true);
    }

    private void drawFan(float x, float y, float radius, int color,
                         float fromNormalX, float fromNormalY,
                         float toNormalX, float toNormalY, boolean cap) {
        int red = channel(color, 16);
        int green = channel(color, 8);
        int blue = channel(color, 0);
        int alpha = channel(color, 24);
        double fromAngle = Math.atan2(fromNormalY, fromNormalX);
        double toAngle = Math.atan2(toNormalY, toNormalX);
        double delta = toAngle - fromAngle;
        while (delta <= -Math.PI) delta += Math.PI * 2.0D;
        while (delta > Math.PI) delta -= Math.PI * 2.0D;
        if (cap) {
            delta = Math.PI;
        }
        int steps = Math.max(1, (int) Math.ceil(Math.abs(delta) / (Math.PI / 6.0D)));
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y, 0).color(red, green, blue, alpha).endVertex();
        for (int i = 0; i <= steps; i++) {
            double angle = fromAngle + delta * i / steps;
            buffer.pos(x + Math.cos(angle) * radius,
                y + Math.sin(angle) * radius, 0).color(red, green, blue, alpha).endVertex();
        }
        Tessellator.getInstance().draw();
    }

    private static void putColorVertex(BufferBuilder buffer, double x, double y,
                                       int red, int green, int blue, int alpha) {
        buffer.pos(x, y, 0.0D).color(red, green, blue, alpha).endVertex();
    }

    /** Hex's drawSpot is a dynamic triangle fan, not a texture lookup. */
    private void drawHexSpot(float x, float y, float radius, int argb) {
        if (radius <= 0.0F) {
            return;
        }
        int red = channel(argb, 16);
        int green = channel(argb, 8);
        int blue = channel(argb, 0);
        int alpha = channel(argb, 24);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y, 0).color(red, green, blue, alpha).endVertex();
        for (int i = 0; i <= 6; i++) {
            double angle = Math.PI * 2.0D * i / 6.0D;
            buffer.pos(x + Math.cos(angle) * radius,
                y + Math.sin(angle) * radius, 0).color(red, green, blue, alpha).endVertex();
        }
        Tessellator.getInstance().draw();
    }

    /**
     * Hex's connection points are layered geometry rather than a flat dot:
     * an outer colored fade, a six-sided colored core, and a bright center.
     */
    private void drawConnectionSpot(float x, float y, int glowColor, int coreColor) {
        drawCircle(x, y, 6.0F,
            withAlpha(glowColor, 0x72), withAlpha(glowColor, 0x00));
        drawHexSpot(x, y, 3.0F, withAlpha(coreColor, 0xE8));
        drawHexSpot(x, y, 1.15F, 0xFFF8FFFF);
    }

    private static int withAlpha(int argb, int alpha) {
        return ((alpha & 0xFF) << 24) | (argb & 0x00FFFFFF);
    }

    /** Hex renders the guide glow as dynamic position-colour geometry, not a texture. */
    private void drawCircle(float x, float y, float radius, int centerArgb, int edgeArgb) {
        if (radius <= 0.0F) {
            return;
        }
        int centerRed = channel(centerArgb, 16);
        int centerGreen = channel(centerArgb, 8);
        int centerBlue = channel(centerArgb, 0);
        int centerAlpha = channel(centerArgb, 24);
        int edgeRed = channel(edgeArgb, 16);
        int edgeGreen = channel(edgeArgb, 8);
        int edgeBlue = channel(edgeArgb, 0);
        int edgeAlpha = channel(edgeArgb, 24);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y, 0).color(centerRed, centerGreen, centerBlue, centerAlpha).endVertex();
        final int segments = 6;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0D * (double) i / (double) segments;
            buffer.pos(x + (float) Math.cos(angle) * radius,
                    y + (float) Math.sin(angle) * radius, 0)
                .color(edgeRed, edgeGreen, edgeBlue, edgeAlpha).endVertex();
        }
        tessellator.draw();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0 || drawing) {
            return;
        }
        GridPoint start = pxToCoord(mouseX, mouseY);
        if (usedSpots.contains(start)) {
            return;
        }
        currentPoints.clear();
        currentPoints.add(start);
        current = start;
        workingPattern = null;
        drawing = true;
        status = "";
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton,
                                  long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (clickedMouseButton == 0) {
            drawMove(mouseX, mouseY);
        }
    }

    private void drawMove(int mouseX, int mouseY) {
        if (!drawing || current == null) {
            return;
        }
        int[] anchorPixel = coordToPx(current);
        double dx = mouseX - anchorPixel[0];
        double dy = mouseY - anchorPixel[1];
        double snapDistance = HEX_SIZE * (double) HEX_SIZE * SNAP_DISTANCE_FACTOR;
        if (dx * dx + dy * dy < snapDistance) {
            return;
        }

        // Hex snaps the mouse vector into one of six angular sectors. Rounding
        // the mouse position to an axial cell first changes direction at the
        // sector boundaries and produces different patterns from Hex.
        double normalizedAngle = Math.atan2(dy, dx) / (Math.PI * 2.0D);
        normalizedAngle -= Math.floor(normalizedAngle);
        int directionIndex = (int) Math.round(
            normalizedAngle * HexDir.values().length + 1.0D)
            % HexDir.values().length;
        if (directionIndex < 0) {
            directionIndex += HexDir.values().length;
        }
        HexDir direction = HexDir.values()[directionIndex];
        GridPoint next = current.add(direction);
        if (usedSpots.contains(next)) {
            return;
        }
        appendSnappedPoint(next);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state != 0 || !drawing) {
            return;
        }
        drawing = false;
        if (workingPattern == null || currentPoints.size() < 2) {
            resetWorkingPath();
            return;
        }
        if (programCount >= ItemHexStaff.MAX_PROGRAM_SIZE) {
            status = I18n.format("hexcasting.message.program_full", ItemHexStaff.MAX_PROGRAM_SIZE);
            resetWorkingPath();
            return;
        }

        GridPoint origin = currentPoints.get(0);
        HexPattern submittedPattern = workingPattern;
        savedPatterns.add(submittedPattern);
        savedOrigins.add(origin);
        sendProgramSnapshot();
        HexActionRegistry.bootstrap();
        HexAction action = HexActionRegistry.get(submittedPattern);
        ResourceLocation id = action == null ? null : HexActionRegistry.idFor(action);
        if (id != null) {
            programIds.add(id);
        }
        programCount++;
        drawnPaths.add(new DrawnPath(submittedPattern,
            new ArrayList<>(currentPoints), id));
        usedSpots.addAll(currentPoints);
        currentPoints.clear();
        current = null;
        workingPattern = null;
        if (id == null) {
            status = I18n.format("hexcasting.message.pattern_unregistered",
                patternDescription(submittedPattern));
        } else {
            status = I18n.format("hexcasting.message.program_added", localizeAction(id),
                programCount, ItemHexStaff.MAX_PROGRAM_SIZE);
        }
    }

    private boolean isClosedStroke() {
        return currentPoints.size() > 2 && current != null
            && current.equals(currentPoints.get(0));
    }

    private void appendSnappedPoint(GridPoint next) {
        if (next == null || current == null || !isAdjacent(current, next)) {
            return;
        }
        boolean closing = next.equals(currentPoints.get(0));
        if (!closing && usedSpots.contains(next)) {
            return;
        }
        HexDir direction = directionBetween(current, next);
        if (direction == null) {
            return;
        }
        if (workingPattern == null) {
            workingPattern = new HexPattern(direction);
            currentPoints.add(next);
            current = next;
            return;
        }
        HexDir last = workingPattern.finalDir();
        if (!closing && direction == last.rotatedBy(HexAngle.BACK)) {
            if (workingPattern.getAngles().isEmpty()) {
                currentPoints.clear();
                currentPoints.add(next);
                current = next;
                workingPattern = null;
            } else {
                workingPattern.getAngles().remove(workingPattern.getAngles().size() - 1);
                if (currentPoints.size() > 1) {
                    currentPoints.remove(currentPoints.size() - 1);
                }
                current = currentPoints.get(currentPoints.size() - 1);
            }
            return;
        }
        if (workingPattern.tryAppendDir(direction)) {
            currentPoints.add(next);
            current = next;
        }
    }

    private static HexDir directionBetween(GridPoint from, GridPoint to) {
        for (HexDir direction : HexDir.values()) {
            if (from.add(direction).equals(to)) {
                return direction;
            }
        }
        return null;
    }

    private static String patternDescription(HexPattern pattern) {
        return pattern == null ? I18n.format("hexcasting.tooltip.pattern") : pattern.signature();
    }

    private static boolean samePixel(float[] first, float[] second) {
        return first != null && second != null
            && Math.abs(first[0] - second[0]) < 0.01F
            && Math.abs(first[1] - second[1]) < 0.01F;
    }


    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            if (drawing) {
                resetWorkingPath();
                drawing = false;
            } else {
                mc.displayGuiScreen(null);
            }
            return;
        }
        if (typedChar == 'c' || typedChar == 'C') {
            if (mc != null && mc.player != null) {
                ItemHexStaff.clearProgram(mc.player, mc.player.getHeldItem(hand));
            }
            PaucalAPI.sendToServer(new MsgStaffPatternC2S(hand, (ResourceLocation) null));
            programIds.clear();
            programCount = 0;
            drawnPaths.clear();
            savedPatterns.clear();
            savedOrigins.clear();
            usedSpots.clear();
            resetWorkingPath();
            drawing = false;
            status = I18n.format("hexcasting.message.program_cleared");
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void sendProgramSnapshot() {
        List<Integer> originQ = new ArrayList<>();
        List<Integer> originR = new ArrayList<>();
        for (GridPoint origin : savedOrigins) {
            originQ.add(origin.q);
            originR.add(origin.r);
        }
        if (mc != null && mc.player != null) {
            NBTTagList snapshot = new NBTTagList();
            for (int i = 0; i < savedPatterns.size(); i++) {
                HexPattern pattern = savedPatterns.get(i);
                GridPoint origin = i < savedOrigins.size()
                    ? savedOrigins.get(i) : new GridPoint(0, 0);
                NBTTagCompound entry = pattern.serializeToNBT();
                entry.setInteger("origin_q", origin.q);
                entry.setInteger("origin_r", origin.r);
                snapshot.appendTag(entry);
            }
            ItemHexStaff.replaceProgram(mc.player, mc.player.getHeldItem(hand), snapshot);
        }
        PaucalAPI.sendToServer(new MsgStaffPatternC2S(
            hand, new ArrayList<>(savedPatterns), originQ, originR));
    }

    private void refreshProgram() {
        HexActionRegistry.bootstrap();
        programIds.clear();
        programCount = 0;
        drawnPaths.clear();
        savedPatterns.clear();
        savedOrigins.clear();
        usedSpots.clear();
        if (mc == null || mc.player == null) {
            return;
        }
        for (ItemHexStaff.ProgramEntry entry : ItemHexStaff.getProgramEntries(
            mc.player, mc.player.getHeldItem(hand))) {
            programCount++;
            ResourceLocation id = entry.getActionId();
            if (id != null) {
                programIds.add(id);
            }
            GridPoint origin = new GridPoint(entry.getOriginQ(), entry.getOriginR());
            savedPatterns.add(entry.getPattern());
            savedOrigins.add(origin);
            List<GridPoint> points = patternPoints(entry.getPattern(), origin);
            drawnPaths.add(new DrawnPath(entry.getPattern(), points, id));
            usedSpots.addAll(points);
        }
    }

    private static List<GridPoint> patternPoints(HexPattern pattern, GridPoint origin) {
        List<GridPoint> result = new ArrayList<>();
        GridPoint cursor = origin;
        result.add(cursor);
        for (HexDir direction : pattern.directions()) {
            cursor = cursor.add(direction);
            result.add(cursor);
        }
        return result;
    }

    private void resetWorkingPath() {
        currentPoints.clear();
        current = null;
        workingPattern = null;
    }

    /** Exact axial-to-cube rounding, matching Hex's pixel-to-coordinate snap. */
    private GridPoint pxToCoord(int mouseX, int mouseY) {
        double dx = (mouseX - width / 2.0D) / HEX_SIZE;
        double r = (mouseY - height / 2.0D) / (HEX_SIZE * 0.866025403784D);
        double q = dx - r * 0.5D;
        double cubeX = q;
        double cubeZ = r;
        double cubeY = -cubeX - cubeZ;
        double roundedX = Math.round(cubeX);
        double roundedY = Math.round(cubeY);
        double roundedZ = Math.round(cubeZ);
        double xDiff = Math.abs(roundedX - cubeX);
        double yDiff = Math.abs(roundedY - cubeY);
        double zDiff = Math.abs(roundedZ - cubeZ);
        if (xDiff > yDiff && xDiff > zDiff) {
            roundedX = -roundedY - roundedZ;
        } else if (yDiff > zDiff) {
            roundedY = -roundedX - roundedZ;
        } else {
            roundedZ = -roundedX - roundedY;
        }
        return new GridPoint((int) roundedX, (int) roundedZ);
    }

    private int[] coordToPx(GridPoint point) {
        double x = width / 2.0D + (point.q + point.r * 0.5D) * HEX_SIZE;
        double y = height / 2.0D + point.r * HEX_SIZE * 0.866025403784D;
        return new int[] {(int) Math.round(x), (int) Math.round(y)};
    }

    private static boolean isAdjacent(GridPoint from, GridPoint to) {
        int dq = to.q - from.q;
        int dr = to.r - from.r;
        return Math.max(Math.abs(dq), Math.max(Math.abs(dr), Math.abs(dq + dr))) == 1;
    }

    private static int hexDistance(int q, int r) {
        return Math.max(Math.abs(q), Math.max(Math.abs(r), Math.abs(q + r)));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double lerp(double value, double from, double to) {
        return from + (to - from) * value;
    }

    private static int color(double red, double green, double blue, float alpha) {
        return ((int) (Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24)
            | ((int) (Math.max(0.0D, Math.min(1.0D, red)) * 255.0D) << 16)
            | ((int) (Math.max(0.0D, Math.min(1.0D, green)) * 255.0D) << 8)
            | (int) (Math.max(0.0D, Math.min(1.0D, blue)) * 255.0D);
    }

    private static int channel(int color, int shift) {
        return (color >> shift) & 0xFF;
    }

    private static String localizeAction(ResourceLocation id) {
        String key = "hexcasting.action." + id.getResourcePath();
        String translated = I18n.format(key);
        return key.equals(translated) ? id.getResourcePath() : translated;
    }

    private static final class DrawnPath {
        private final HexPattern pattern;
        private final List<GridPoint> points;
        private final ResourceLocation id;

        private DrawnPath(HexPattern pattern, List<GridPoint> points, ResourceLocation id) {
            this.pattern = pattern;
            this.points = points;
            this.id = id;
        }
    }

    private static final class GridPoint {
        private final int q;
        private final int r;

        private GridPoint(int q, int r) {
            this.q = q;
            this.r = r;
        }

        private GridPoint add(int deltaQ, int deltaR) {
            return new GridPoint(q + deltaQ, r + deltaR);
        }

        private GridPoint add(HexDir direction) {
            switch (direction) {
                case NORTH_EAST: return add(1, -1);
                case EAST: return add(1, 0);
                case SOUTH_EAST: return add(0, 1);
                case SOUTH_WEST: return add(-1, 1);
                case WEST: return add(-1, 0);
                case NORTH_WEST: return add(0, -1);
                default: throw new AssertionError(direction);
            }
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof GridPoint
                && q == ((GridPoint) other).q && r == ((GridPoint) other).r;
        }

        @Override
        public int hashCode() {
            return 31 * q + r;
        }
    }
}
