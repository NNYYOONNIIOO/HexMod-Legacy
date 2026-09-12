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
 * Hex's in-world spell drawing screen, adapted from the 1.20.1
 * GuiSpellcasting and PatternRenderer flow for the 1.12.2 GuiScreen API.
 */
public final class GuiHexStaff extends GuiScreen {
    private static final int HEX_SIZE = 42;
    private static final int GUIDE_RADIUS = 3;
    private static final int SEARCH_RADIUS = 32;
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
        // Hex does not put a dark container panel over the world. Its spell
        // screen is a transparent overlay rendered around the mouse position.
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        drawExistingPaths();
        drawWorkingPath(mouseX, mouseY);
        drawGuideSpots(mouseX, mouseY);
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
                double scaled = clamp(
                    1.0D - ((distance - HEX_SIZE) / (GUIDE_RADIUS * (double) HEX_SIZE)),
                    0.0D, 1.0D);
                // The upstream renderer deliberately keeps the outer part of
                // rangeAround(3) faint instead of removing those spots. This
                // makes the guide grow/shrink continuously as the mouse moves.
                drawSpot(pixel[0], pixel[1], (float) Math.max(0.12D, scaled));
            }
        }
    }

    private void drawSpot(int x, int y, float strength) {
        float visible = Math.max(0.12F, Math.min(1.0F, strength));
        int glowRadius = Math.max(2, Math.round(5.0F * visible));
        int glowAlpha = Math.min(220, Math.max(32, Math.round(110.0F + 120.0F * visible)));
        int glowColor = (glowAlpha << 24) | 0x70E8E8;
        drawRect(x - glowRadius, y - glowRadius,
            x + glowRadius + 1, y + glowRadius + 1, glowColor);

        int coreRadius = Math.max(1, Math.round(2.0F * visible));
        int coreAlpha = Math.min(255, Math.max(70, Math.round(180.0F + 75.0F * visible)));
        int coreColor = (coreAlpha << 24) | 0xB8FFFF;
        drawRect(x - coreRadius, y - coreRadius,
            x + coreRadius + 1, y + coreRadius + 1, coreColor);
    }

    private void drawExistingPaths() {
        for (DrawnPath path : drawnPaths) {
            drawPath(path.points, 0xB090E8E8, 0xE0D8FFFF);
        }
    }

    private void drawWorkingPath(int mouseX, int mouseY) {
        if (currentPoints.isEmpty()) {
            return;
        }
        drawPath(currentPoints, 0xE090FFFF, 0xFFF0FFFF);
        if (drawing && current != null) {
            GridPoint hover = pxToCoord(mouseX, mouseY);
            if (!hover.equals(current) && isAdjacent(current, hover)) {
                drawLine(current, hover, 0x8080FFFF, 0xA0D8FFFF);
            }
        }
    }

    private void drawPath(List<GridPoint> points, int lineColor, int nodeColor) {
        if (points.size() < 2) {
            if (points.size() == 1) {
                int[] pixel = coordToPx(points.get(0));
                drawSpot(pixel[0], pixel[1], 1.0F);
            }
            return;
        }
        GlStateManager.disableTexture2D();
        GL11.glLineWidth(3.0F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (GridPoint point : points) {
            int[] pixel = coordToPx(point);
            buffer.pos(pixel[0], pixel[1], 0).color(
                (lineColor >> 16) & 0xFF,
                (lineColor >> 8) & 0xFF,
                lineColor & 0xFF,
                (lineColor >> 24) & 0xFF).endVertex();
        }
        tessellator.draw();
        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();

        for (GridPoint point : points) {
            int[] pixel = coordToPx(point);
            drawRect(pixel[0] - 2, pixel[1] - 2,
                pixel[0] + 3, pixel[1] + 3, nodeColor);
        }
    }

    private void drawLine(GridPoint from, GridPoint to, int lineColor, int nodeColor) {
        int[] a = coordToPx(from);
        int[] b = coordToPx(to);
        GlStateManager.disableTexture2D();
        GL11.glLineWidth(2.0F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(a[0], a[1], 0).color(
            (lineColor >> 16) & 0xFF,
            (lineColor >> 8) & 0xFF,
            lineColor & 0xFF,
            (lineColor >> 24) & 0xFF).endVertex();
        buffer.pos(b[0], b[1], 0).color(
            (lineColor >> 16) & 0xFF,
            (lineColor >> 8) & 0xFF,
            lineColor & 0xFF,
            (lineColor >> 24) & 0xFF).endVertex();
        tessellator.draw();
        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();
        drawRect(b[0] - 2, b[1] - 2, b[0] + 3, b[1] + 3, nodeColor);
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
        int directionIndex = (int) Math.round(angle / (Math.PI * 2.0D) * 6.0D) + 1;
        directionIndex %= 6;
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
        drawnPaths.add(new DrawnPath(workingPattern, new ArrayList<>(currentPoints), id));
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

    private GridPoint pxToCoord(int x, int y) {
        GridPoint best = new GridPoint(0, 0);
        double bestDistance = Double.MAX_VALUE;
        for (int q = -SEARCH_RADIUS; q <= SEARCH_RADIUS; q++) {
            for (int r = -SEARCH_RADIUS; r <= SEARCH_RADIUS; r++) {
                GridPoint candidate = new GridPoint(q, r);
                int[] pixel = coordToPx(candidate);
                double dx = x - pixel[0];
                double dy = y - pixel[1];
                double distance = dx * dx + dy * dy;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
        }
        return best;
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
