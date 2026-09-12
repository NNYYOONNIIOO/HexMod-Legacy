package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.casting.math.HexAngle;
import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.common.network.MsgStaffPatternC2S;
import at.petrak.paucal.api.PaucalAPI;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A 1.12.2 native drawing screen for programming a Hex staff. */
public final class GuiHexStaff extends GuiScreen {
    private static final int CLEAR_BUTTON = 1;
    private static final int GRID_RADIUS = 5;
    private static final int CELL = 34;
    private static final int GRID_TOP = 64;

    private final EnumHand hand;
    private final List<GridPoint> points = new ArrayList<>();
    private boolean drawing;
    private HexPattern workingPattern;
    private String status = "";

    public GuiHexStaff(EnumHand hand) {
        this.hand = hand == null ? EnumHand.MAIN_HAND : hand;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(CLEAR_BUTTON, width / 2 - 55, height - 34, 110, 20,
            I18n.format("hexcasting.gui.staff.clear")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int centerX = width / 2;
        drawRect(18, 18, width - 18, height - 18, 0xD0101010);
        drawCenteredString(fontRenderer, I18n.format("hexcasting.gui.staff.title"), centerX, 26, 0xFFFFFF);
        drawCenteredString(fontRenderer, I18n.format("hexcasting.gui.staff.hint"), centerX, 42, 0xB0D0D0D0);

        drawGrid(centerX);
        if (!points.isEmpty()) {
            drawPath(centerX, GRID_TOP);
        }
        if (!status.isEmpty()) {
            drawCenteredString(fontRenderer, status, centerX, height - 55, 0xFFFFA0A0);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawGrid(int centerX) {
        for (int q = -GRID_RADIUS; q <= GRID_RADIUS; q++) {
            for (int r = -GRID_RADIUS; r <= GRID_RADIUS; r++) {
                if (Math.abs(q + r) > GRID_RADIUS) {
                    continue;
                }
                int[] px = toPixel(centerX, GRID_TOP, new GridPoint(q, r));
                drawRect(px[0] - 3, px[1] - 3, px[0] + 4, px[1] + 4, 0xFF707070);
            }
        }
    }

    private void drawPath(int centerX, int top) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GL11.glLineWidth(3.0F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (GridPoint point : points) {
            int[] px = toPixel(centerX, top, point);
            buffer.pos(px[0], px[1], 0).color(0xB0, 0x40, 0xFF, 0xFF).endVertex();
        }
        tessellator.draw();
        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
        for (GridPoint point : points) {
            int[] px = toPixel(centerX, top, point);
            drawRect(px[0] - 4, px[1] - 4, px[0] + 5, px[1] + 5, 0xFFB040FF);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == CLEAR_BUTTON) {
            PaucalAPI.sendToServer(new MsgStaffPatternC2S(hand, null));
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0 || mouseY < GRID_TOP - 18 || mouseY > height - 45) {
            return;
        }
        GridPoint start = nearestPoint(mouseX, mouseY);
        if (start == null) {
            return;
        }
        points.clear();
        points.add(start);
        workingPattern = null;
        drawing = true;
        status = "";
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton,
                                  long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (!drawing || clickedMouseButton != 0 || points.isEmpty()) {
            return;
        }
        GridPoint next = nearestPoint(mouseX, mouseY);
        GridPoint previous = points.get(points.size() - 1);
        if (next == null || next.equals(previous)) {
            return;
        }
        HexDir direction = directionBetween(previous, next);
        if (direction == null) {
            return;
        }
        if (workingPattern == null) {
            workingPattern = new HexPattern(direction);
        } else if (!workingPattern.tryAppendDir(direction)) {
            status = I18n.format("hexcasting.gui.staff.invalid");
            return;
        }
        points.add(next);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state != 0 || !drawing) {
            return;
        }
        drawing = false;
        if (workingPattern == null) {
            return;
        }
        HexActionRegistry.bootstrap();
        Object action = HexActionRegistry.get(workingPattern);
        ResourceLocation id = action == null ? null : HexActionRegistry.idFor((at.petra_k.hexcasting.api.casting.action.HexAction) action);
        if (id == null) {
            status = I18n.format("hexcasting.gui.staff.unknown");
            return;
        }
        PaucalAPI.sendToServer(new MsgStaffPatternC2S(hand, id));
        mc.displayGuiScreen(null);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private GridPoint nearestPoint(int mouseX, int mouseY) {
        GridPoint best = null;
        double bestDistance = 16.0D * 16.0D;
        for (int q = -GRID_RADIUS; q <= GRID_RADIUS; q++) {
            for (int r = -GRID_RADIUS; r <= GRID_RADIUS; r++) {
                if (Math.abs(q + r) > GRID_RADIUS) {
                    continue;
                }
                GridPoint candidate = new GridPoint(q, r);
                int[] px = toPixel(width / 2, GRID_TOP, candidate);
                double dx = mouseX - px[0];
                double dy = mouseY - px[1];
                double distance = dx * dx + dy * dy;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static int[] toPixel(int centerX, int top, GridPoint point) {
        double x = centerX + (point.q + point.r * 0.5D) * CELL;
        double y = top + point.r * CELL * 0.8660254D;
        return new int[] {(int) Math.round(x), (int) Math.round(y)};
    }

    private static HexDir directionBetween(GridPoint from, GridPoint to) {
        int dq = to.q - from.q;
        int dr = to.r - from.r;
        if (dq == 1 && dr == 0) return HexDir.EAST;
        if (dq == 0 && dr == 1) return HexDir.SOUTH_EAST;
        if (dq == -1 && dr == 1) return HexDir.SOUTH_WEST;
        if (dq == -1 && dr == 0) return HexDir.WEST;
        if (dq == 0 && dr == -1) return HexDir.NORTH_WEST;
        if (dq == 1 && dr == -1) return HexDir.NORTH_EAST;
        return null;
    }

    private static final class GridPoint {
        private final int q;
        private final int r;

        private GridPoint(int q, int r) {
            this.q = q;
            this.r = r;
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
