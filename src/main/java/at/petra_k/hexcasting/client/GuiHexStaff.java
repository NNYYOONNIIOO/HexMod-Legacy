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
            drawPath(path.points, 0x5064C8FF, 0xE064C8FF, 0xF064C8FF);
        }
    }

    private void drawWorkingPath(int mouseX, int mouseY) {
        if (currentPoints.isEmpty()) {
            return;
        }
        drawPath(currentPoints, 0x7064C8FF, 0xFF64C8FF, 0xFF64C8FF);
        if (drawing && current != null) {
            GridPoint hover = pxToCoord(mouseX, mouseY);
            if (!hover.equals(current) && isAdjacent(current, hover)
                && !usedSpots.contains(hover)) {
                drawSegment(coordToPx(current), coordToPx(hover),
                    5.0F, 0x7064C8FF);
                drawSegment(coordToPx(current), coordToPx(hover),
                    2.0F, 0xFF64C8FF);
                int[] pixel = coordToPx(hover);
                drawSpot(pixel[0], pixel[1], 1.4F, 0.50D, 1.0D, 0.95D, 0.8F);
            }
        }
    }

    private void drawPath(List<GridPoint> points, int glowColor,
                          int lineColor, int nodeColor) {
        if (points.size() == 1) {
            int[] pixel = coordToPx(points.get(0));
            drawSpot(pixel[0], pixel[1], 1.4F, 0.50D, 1.0D, 0.95D, 0.9F);
            return;
        }
        for (int i = 1; i < points.size(); i++) {
            drawSegment(coordToPx(points.get(i - 1)), coordToPx(points.get(i)),
                5.0F, glowColor);
        }
        for (int i = 1; i < points.size(); i++) {
            drawSegment(coordToPx(points.get(i - 1)), coordToPx(points.get(i)),
                2.0F, lineColor);
        }
        for (GridPoint point : points) {
            int[] pixel = coordToPx(point);
            drawSpot(pixel[0], pixel[1], 1.8F,
                channel(nodeColor, 16) / 255.0D,
                channel(nodeColor, 8) / 255.0D,
                channel(nodeColor, 0) / 255.0D,
                channel(nodeColor, 24) / 255.0F);
        }
    }

    /** Draws a smooth quad strip rather than relying on GL_LINE_STRIP width. */
    private void drawSegment(int[] from, int[] to, float width, int argb) {
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001D) {
            return;
        }
        double px = -dy / length * width * 0.5D;
        double py = dx / length * width * 0.5D;
        int red = channel(argb, 16);
        int green = channel(argb, 8);
        int blue = channel(argb, 0);
        int alpha = channel(argb, 24);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(from[0] + px, from[1] + py, 0).color(red, green, blue, alpha).endVertex();
        buffer.pos(from[0] - px, from[1] - py, 0).color(red, green, blue, alpha).endVertex();
        buffer.pos(to[0] - px, to[1] - py, 0).color(red, green, blue, alpha).endVertex();
        buffer.pos(to[0] + px, to[1] + py, 0).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
    }

    /** Hex renders spots as dynamic position-colour geometry, not a texture. */
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
        final int segments = 24;
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

        double angle = Math.atan2(dy, dx);
        int directionIndex = ((int) Math.round(
            angle / (Math.PI * 2.0D) * 6.0D) + 1) % 6;
        if (directionIndex < 0) {
            directionIndex += 6;
        }
        HexDir newDir = HexDir.values()[directionIndex];
        GridPoint idealNext = current.add(newDir);
        if (usedSpots.contains(idealNext)) {
            return;
        }

        if (workingPattern == null) {
            workingPattern = new HexPattern(newDir);
            currentPoints.add(idealNext);
            current = idealNext;
            return;
        }

        HexDir lastDir = workingPattern.finalDir();
        if (newDir == lastDir.rotatedBy(HexAngle.BACK)) {
            if (workingPattern.getAngles().isEmpty()) {
                currentPoints.clear();
                currentPoints.add(idealNext);
                current = idealNext;
                workingPattern = null;
            } else {
                workingPattern.getAngles().remove(workingPattern.getAngles().size() - 1);
                if (currentPoints.size() > 1) {
                    currentPoints.remove(currentPoints.size() - 1);
                }
                current = idealNext;
            }
            return;
        }

        if (workingPattern.tryAppendDir(newDir)) {
            currentPoints.add(idealNext);
            current = idealNext;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state != 0 || !drawing) {
            return;
        }
        drawing = false;
        if (workingPattern == null) {
            resetWorkingPath();
            return;
        }
        if (programCount >= ItemHexStaff.MAX_PROGRAM_SIZE) {
            status = I18n.format("hexcasting.message.program_full", ItemHexStaff.MAX_PROGRAM_SIZE);
            resetWorkingPath();
            return;
        }

        GridPoint origin = currentPoints.get(0);
        PaucalAPI.sendToServer(new MsgStaffPatternC2S(
            hand, workingPattern, origin.q, origin.r));
        HexActionRegistry.bootstrap();
        HexAction action = HexActionRegistry.get(workingPattern);
        ResourceLocation id = action == null ? null : HexActionRegistry.idFor(action);
        if (id != null) {
            programIds.add(id);
        }
        programCount++;
        drawnPaths.add(new DrawnPath(workingPattern,
            new ArrayList<>(currentPoints), id));
        usedSpots.addAll(currentPoints);
        currentPoints.clear();
        current = null;
        workingPattern = null;
        status = I18n.format("hexcasting.message.program_added",
            id == null ? I18n.format("hexcasting.tooltip.pattern") : localizeAction(id),
            programCount, ItemHexStaff.MAX_PROGRAM_SIZE);
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
            PaucalAPI.sendToServer(new MsgStaffPatternC2S(hand, null));
            programIds.clear();
            programCount = 0;
            drawnPaths.clear();
            usedSpots.clear();
            resetWorkingPath();
            drawing = false;
            status = I18n.format("hexcasting.message.program_cleared");
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void refreshProgram() {
        HexActionRegistry.bootstrap();
        programIds.clear();
        programCount = 0;
        drawnPaths.clear();
        usedSpots.clear();
        if (mc == null || mc.player == null) {
            return;
        }
        for (ItemHexStaff.ProgramEntry entry : ItemHexStaff.getProgramEntries(
            mc.player.getHeldItem(hand))) {
            programCount++;
            ResourceLocation id = entry.getActionId();
            if (id != null) {
                programIds.add(id);
            }
            GridPoint origin = new GridPoint(entry.getOriginQ(), entry.getOriginR());
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
