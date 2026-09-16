package at.petra_k.hexcasting.client;

import net.minecraft.item.ItemStack;
import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.math.HexAngle;
import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.interop.inline.HexInline;
import at.petra_k.hexcasting.interop.inline.InlinePatternChatRenderer;
import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.common.casting.SpecialPatternResolver;
import at.petra_k.hexcasting.common.casting.StaffCastExecutor;
import at.petra_k.hexcasting.common.lib.hex.HexActions;
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
import net.minecraft.util.text.TextComponentString;
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
    private static final int GUIDE_RADIUS = 3;
    private static final double SNAP_DISTANCE_FACTOR = 2.0D;
    private static final int RESOLUTION_UNRESOLVED = 0;
    private static final int RESOLUTION_EVALUATED = 1;
    private static final int RESOLUTION_ESCAPED = 2;
    private static final int RESOLUTION_UNDONE = 3;
    private static final int RESOLUTION_ERRORED = 4;
    private static final int RESOLUTION_INVALID = 5;
    private static final int ZAPPY_HOPS = 10;

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
    private List<String> stackPreview = new ArrayList<>();
    private int parenDepth;
    private boolean escapeNext;

    public GuiHexStaff(EnumHand hand) {
        this.hand = hand == null ? EnumHand.MAIN_HAND : hand;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
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

        // Hex keeps the live evaluation stack in a translucent panel at the
        // upper-left. This is also where an open Introspection is made
        // visible, even though the value stack is currently empty.
        drawStackPreview();
    }

    private void drawStackPreview() {
        List<String> lines = new ArrayList<>();
        if (stackPreview != null) {
            lines.addAll(stackPreview);
        }
        if (lines.isEmpty() && parenDepth > 0) {
            lines.add(I18n.format("hexcasting.gui.staff.introspecting"));
        }
        if (escapeNext) {
            lines.add(I18n.format("hexcasting.gui.staff.escape_ready"));
        }
        if (lines.isEmpty()) {
            return;
        }

        int measuredWidth = 0;
        for (String line : lines) {
            measuredWidth = Math.max(measuredWidth,
                InlinePatternChatRenderer.stringWidth(fontRenderer, line));
        }
        int maxWidth = Math.max(90, Math.min(
            Math.max(measuredWidth + 10, (int) (width * 0.35F)), 260));
        int panelHeight = 5 + lines.size() * 10;
        drawRect(10, 10, 10 + maxWidth, 10 + panelHeight, 0x50303030);
        drawRect(13, 13, 8 + maxWidth, 7 + panelHeight, 0x50303030);

        int y = 15;
        for (String line : lines) {
            String safe = line == null ? "?" : line;
            // PatternIota.display() contains the same private Inline token as
            // chat and tooltips. Draw its width-preserving text first, then
            // the actual glyph at the token's measured position.
            InlinePatternChatRenderer.drawInlineText(
                fontRenderer, safe, 15, y, 0xFFEFEFEF);
            y += 10;
        }
    }

    private void postChatMessage(String message) {
        if (message == null || message.isEmpty() || mc == null
            || mc.ingameGUI == null || mc.ingameGUI.getChatGUI() == null) {
            return;
        }
        TextComponentString component = new TextComponentString(message);
        // printChatMessage bypasses ClientChatReceivedEvent, so perform the
        // same capture/blank replacement that the normal chat event uses.
        InlinePatternChatRenderer.capture(component);
        mc.ingameGUI.getChatGUI().printChatMessage(
            InlinePatternChatRenderer.stripTokens(component));
    }

    private void drawGuideSpots(int mouseX, int mouseY) {
        GridPoint mouseCoord = pxToCoord(mouseX, mouseY);
        double hexSize = hexSize();
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
                    1.0D - ((distance - hexSize) / (GUIDE_RADIUS * hexSize)),
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

    /** Match Hex's ResolvedPatternType palette for completed paths. */
    private static ResolutionColors resolutionColors(int resolutionOrdinal) {
        int start;
        int end;
        switch (resolutionOrdinal) {
            case RESOLUTION_EVALUATED:
                start = 0xFF7385DE;
                end = 0xFFFECBE6;
                break;
            case RESOLUTION_ESCAPED:
                start = 0xFFDDCC73;
                end = 0xFFFFFAE5;
                break;
            case RESOLUTION_UNDONE:
                start = 0xFFB26B6B;
                end = 0xFFCCA88E;
                break;
            case RESOLUTION_ERRORED:
                start = 0xFFDE6262;
                end = 0xFFFFC7A0;
                break;
            case RESOLUTION_INVALID:
                start = 0xFFB26B6B;
                end = 0xFFCCA88E;
                break;
            case RESOLUTION_UNRESOLVED:
            default:
                start = 0xFF7F7F7F;
                end = 0xFFCCCCCC;
                break;
        }
        // The state palette changes between patterns, but each individual
        // stroke is rendered as a solid color instead of a tail-to-head
        // gradient.
        return new ResolutionColors(start, start, screenColor(start),
            screenColor(start), start);
    }

    private static int screenColor(int argb) {
        int alpha = channel(argb, 24);
        int red = (channel(argb, 16) + 255) / 2;
        int green = (channel(argb, 8) + 255) / 2;
        int blue = (channel(argb, 0) + 255) / 2;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static boolean isUnstableResolution(int resolutionOrdinal) {
        return resolutionOrdinal == RESOLUTION_ERRORED
            || resolutionOrdinal == RESOLUTION_INVALID;
    }

    private void drawExistingPaths() {
        for (DrawnPath path : drawnPaths) {
            ResolutionColors colors = resolutionColors(path.resolutionOrdinal);
            drawPath(path.points, colors.outerStart, colors.outerEnd,
                colors.innerStart, colors.innerEnd, colors.node,
                isUnstableResolution(path.resolutionOrdinal));
        }
    }

    private void drawWorkingPath(int mouseX, int mouseY) {
        if (currentPoints.isEmpty()) {
            return;
        }
        // Hex renders the snapped path and the unsnapped cursor as one
        // continuous preview. The cursor segment is important both for
        // readable connections and for seeing the next snapped direction.
        drawPath(currentPoints, 0xFF64C8FF, 0xFF64C8FF,
            screenColor(0xFF64C8FF), screenColor(0xFF64C8FF), 0xFFFECBE6,
            false,
            mouseX, mouseY, drawing && current != null);
    }

    private void drawPath(List<GridPoint> points, int outerStart, int outerEnd,
                          int innerStart, int innerEnd, int nodeColor,
                          boolean unstable) {
        drawPath(points, outerStart, outerEnd, innerStart, innerEnd, nodeColor,
            unstable, -1, -1, false);
    }

    private void drawPath(List<GridPoint> points, int outerStart, int outerEnd,
                          int innerStart, int innerEnd, int nodeColor,
                          boolean unstable,
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
        Set<GridPoint> duplicateSpots = new HashSet<>();
        Set<Integer> duplicateIndices = new HashSet<>();
        Set<GridPoint> visitedSpots = new HashSet<>();
        for (int i = 0; i < nodeCount; i++) {
            GridPoint point = points.get(i);
            if (!visitedSpots.add(point)) {
                duplicateSpots.add(point);
                duplicateIndices.add(i);
                for (int previous = 0; previous < i; previous++) {
                    if (point.equals(points.get(previous))) {
                        duplicateIndices.add(previous);
                    }
                }
            }
        }
        if (includeCursor) {
            pixelPoints.add(new float[] {cursorX, cursorY});
        }
        if (pixelPoints.size() > 1) {
            // RenderLib.drawPatternFromPoints expands segments with makeZappy
            // before the 5 px outer and 2 px inner drawLineSeq passes.
            ZappyPath zappyPath = makeZappyPoints(pixelPoints, duplicateIndices,
                unstable ? 0.9F : 0.2F, 0.8F, points.size());
            // Match Hex RenderLib.drawPatternFromPoints: one 5 px pattern
            // ribbon followed by the 2 px readability pass. The old extra
            // 8 px halo made connections visibly thicker than upstream.
            drawLineSequence(zappyPath, 5.0F, outerStart, outerEnd);
            drawLineSequence(zappyPath, 2.0F, innerStart, innerEnd);
        }
        for (int i = 0; i < nodeCount; i++) {
            float[] pixel = pixelPoints.get(i);
            if (i == nodeCount - 1 && nodeCount > 1
                && samePixel(pixel, pixelPoints.get(0))) {
                continue;
            }
            drawConnectionSpot(pixel[0], pixel[1], outerStart, nodeColor,
                duplicateSpots.contains(points.get(i)));
        }
    }

    /**
     * Direct Java port of Hex's RenderLib.makeZappy. The old port used five
     * random hops and a per-segment hash, which made the lightning noticeably
     * different from 1.20.1 and broke closed/duplicate strokes.
     */
    private static ZappyPath makeZappyPoints(List<float[]> barePoints,
                                             Set<Integer> duplicateIndices,
                                             float flowIrregular,
                                             float lastSegmentLenProportion,
                                             int seedSalt) {
        if (barePoints == null || barePoints.isEmpty()) {
            return new ZappyPath(new ArrayList<float[]>(), new ArrayList<Integer>(), 0);
        }
        final float variance = 2.5F;
        final double speed = 0.1D;
        final double zSeed = (System.currentTimeMillis() / 50.0D) * speed;
        final double seed = seedSalt;
        final int sourceSegmentCount = Math.max(1, barePoints.size() - 1);

        if (duplicateIndices == null) {
            return zappify(barePoints, false, ZAPPY_HOPS, variance, zSeed,
                flowIrregular, lastSegmentLenProportion, seed, 0,
                sourceSegmentCount);
        }

        List<float[]> zappy = new ArrayList<>(barePoints.size() * ZAPPY_HOPS);
        List<Integer> segmentIndices = new ArrayList<>(barePoints.size() * ZAPPY_HOPS);
        List<float[]> daisyChain = new ArrayList<>();
        int chainStartSegment = 0;
        for (int i = 0; i + 1 < barePoints.size(); i++) {
            float[] head = barePoints.get(i);
            float[] tail = barePoints.get(i + 1);
            float tangentX = (tail[0] - head[0]) * 0.2F;
            float tangentY = (tail[1] - head[1]) * 0.2F;
            if (i != 0 && duplicateIndices.contains(i)) {
                daisyChain.add(new float[] {head[0] + tangentX, head[1] + tangentY});
            } else {
                daisyChain.add(new float[] {head[0], head[1]});
            }

            if (i == barePoints.size() - 2) {
                daisyChain.add(new float[] {tail[0], tail[1]});
                ZappyPath chain = zappify(daisyChain, true, ZAPPY_HOPS, variance,
                    zSeed, flowIrregular, lastSegmentLenProportion, seed,
                    chainStartSegment, sourceSegmentCount);
                if (!zappy.isEmpty() && !chain.points.isEmpty()) {
                    segmentIndices.add(chain.segmentIndices.isEmpty()
                        ? chainStartSegment : chain.segmentIndices.get(0));
                }
                zappy.addAll(chain.points);
                segmentIndices.addAll(chain.segmentIndices);
            } else if (duplicateIndices.contains(i + 1)) {
                daisyChain.add(new float[] {tail[0] - tangentX, tail[1] - tangentY});
                ZappyPath chain = zappify(daisyChain, false, ZAPPY_HOPS, variance,
                    zSeed, flowIrregular, lastSegmentLenProportion, seed,
                    chainStartSegment, sourceSegmentCount);
                if (!zappy.isEmpty() && !chain.points.isEmpty()) {
                    segmentIndices.add(chain.segmentIndices.isEmpty()
                        ? chainStartSegment : chain.segmentIndices.get(0));
                }
                zappy.addAll(chain.points);
                segmentIndices.addAll(chain.segmentIndices);
                daisyChain.clear();
                chainStartSegment = i + 1;
            }
        }
        return new ZappyPath(zappy, segmentIndices, sourceSegmentCount);
    }

    private static ZappyPath zappify(List<float[]> points, boolean truncateLast,
                                     int hops, float variance, double zSeed,
                                     float flowIrregular,
                                     float lastSegmentLenProportion,
                                     double seed, int sourceSegmentOffset,
                                     int sourceSegmentCount) {
        List<float[]> result = new ArrayList<>(points.size() * hops);
        List<Integer> segmentIndices = new ArrayList<>(points.size() * hops);
        result.add(new float[] {points.get(0)[0], points.get(0)[1]});
        for (int i = 0; i + 1 < points.size(); i++) {
            float[] source = points.get(i);
            float[] target = points.get(i + 1);
            double dx = target[0] - source[0];
            double dy = target[1] - source[1];
            double distance = Math.sqrt(dx * dx + dy * dy);
            double hopDistance = distance / hops;
            double maxVariance = hopDistance * variance;
            int maxJ = truncateLast && i == points.size() - 2
                ? Math.round(lastSegmentLenProportion * hops) : hops;
            for (int j = 1; j <= maxJ; j++) {
                double progress = j / (double) (hops + 1);
                float px = (float) (source[0] + dx * progress);
                float py = (float) (source[1] + dy * progress);
                double minorPerturb = getZappyNoise(i, j, Math.sin(zSeed))
                    * flowIrregular;
                double theta = 3.0D * getZappyNoise(
                    i + progress + minorPerturb - zSeed, 1337.0D, seed)
                    * Math.PI * 2.0D;
                double scaleVariance = Math.min(1.0D,
                    8.0D * (0.5D - Math.abs(0.5D - progress)));
                double radius = getZappyNoise(
                    i + progress - zSeed, 69420.0D, seed)
                    * maxVariance * scaleVariance;
                result.add(new float[] {
                    (float) (px + radius * Math.cos(theta)),
                    (float) (py + radius * Math.sin(theta))
                });
                segmentIndices.add(Math.max(0, Math.min(sourceSegmentCount - 1,
                    sourceSegmentOffset + i)));
                if (j == hops) {
                    result.add(new float[] {target[0], target[1]});
                    segmentIndices.add(Math.max(0, Math.min(sourceSegmentCount - 1,
                        sourceSegmentOffset + i)));
                }
            }
        }
        return new ZappyPath(result, segmentIndices, sourceSegmentCount);
    }

    private static double getZappyNoise(double x, double y, double z) {
        // Keep the seeded simplex source used by RenderLib. The 1.12.2
        // screen uses a wall-clock animation seed rather than the modern
        // client tick counter; on some frames that lands several samples
        // close to a zero contour and makes all ten hops look straight. A
        // small smooth fallback wave preserves the animated feel and keeps
        // the perturbation visible without turning the stroke into noise.
        double simplex = ZAPPY_NOISE.value(x * 0.6D, y * 0.6D, z * 0.6D)
            / 2.0D;
        double wave = Math.sin(x * 2.17D + y * 0.013D + z * 0.071D) * 0.18D
            + Math.cos(x * 0.71D - y * 0.009D + z * 0.043D) * 0.12D;
        return Math.max(-0.5D, Math.min(0.5D, simplex + wave));
    }

    /**
     * RenderLib's line join is four triangles per segment plus a 10-step fan
     * at every turn. Keeping that topology is what gives Hex its characteristic
     * soft, slightly pointed connections instead of a generic mitered line.
     */
    private void drawLineSequence(ZappyPath path, float width,
                                  int tailColor, int headColor) {
        List<float[]> points = path == null ? null : path.points;
        if (points == null || points.size() <= 1 || width <= 0.0F) {
            return;
        }
        int n = points.size();
        float halfWidth = width * 0.5F;
        float[] joinAngles = new float[n];
        float[] joinOffsets = new float[n];
        for (int i = 2; i < n; i++) {
            float[] p0 = points.get(i - 2);
            float[] p1 = points.get(i - 1);
            float[] p2 = points.get(i);
            float prevX = p1[0] - p0[0];
            float prevY = p1[1] - p0[1];
            float nextX = p2[0] - p1[0];
            float nextY = p2[1] - p1[1];
            float prevLength = length(prevX, prevY);
            float nextLength = length(nextX, nextY);
            if (prevLength < 0.0001F || nextLength < 0.0001F) {
                continue;
            }
            float angle = (float) Math.atan2(
                prevX * nextY - prevY * nextX,
                prevX * nextX + prevY * nextY);
            joinAngles[i - 1] = angle;
            float clamp = Math.min(prevLength, nextLength) / halfWidth;
            float denominator = 1.0F + (float) Math.cos(angle);
            float offset = denominator < 0.0001F ? 0.0F
                : (float) Math.sin(angle) / denominator;
            joinOffsets[i - 1] = Math.max(-clamp, Math.min(clamp, offset));
        }

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
        for (int i = 0; i < n - 1; i++) {
            float[] p1 = points.get(i);
            float[] p2 = points.get(i + 1);
            float dx = p2[0] - p1[0];
            float dy = p2[1] - p1[1];
            float segmentLength = length(dx, dy);
            if (segmentLength < 0.0001F) {
                continue;
            }
            float tangentX = dx / segmentLength * halfWidth;
            float tangentY = dy / segmentLength * halfWidth;
            float normalX = -tangentY;
            float normalY = tangentX;
            // Keep one complete drawn stroke in one color. The modern
            // RenderLib interpolates its endpoint colors, but that creates a
            // visible blue-to-pink gradient on long staff patterns. The
            // state color is intentionally constant for this 1.12.2 screen.
            int color1 = tailColor;
            int color2 = tailColor;
            float low = joinOffsets[i];
            float high = joinOffsets[i + 1];

            float[] p1Down = new float[] {
                p1[0] + tangentX * Math.max(0.0F, low) + normalX,
                p1[1] + tangentY * Math.max(0.0F, low) + normalY};
            float[] p1Up = new float[] {
                p1[0] + tangentX * Math.max(0.0F, -low) - normalX,
                p1[1] + tangentY * Math.max(0.0F, -low) - normalY};
            float[] p2Down = new float[] {
                p2[0] - tangentX * Math.max(0.0F, high) + normalX,
                p2[1] - tangentY * Math.max(0.0F, high) + normalY};
            float[] p2Up = new float[] {
                p2[0] - tangentX * Math.max(0.0F, -high) - normalX,
                p2[1] - tangentY * Math.max(0.0F, -high) - normalY};

            putColorVertex(buffer, p1Down, color1);
            putColorVertex(buffer, p1, color1);
            putColorVertex(buffer, p1Up, color1);
            putColorVertex(buffer, p1Down, color1);
            putColorVertex(buffer, p1Up, color1);
            putColorVertex(buffer, p2Up, color2);
            putColorVertex(buffer, p1Down, color1);
            putColorVertex(buffer, p2Up, color2);
            putColorVertex(buffer, p2, color2);
            putColorVertex(buffer, p1Down, color1);
            putColorVertex(buffer, p2, color2);
            putColorVertex(buffer, p2Down, color2);

            if (i > 0) {
                float signedAngle = joinAngles[i];
                float angle = Math.abs(signedAngle);
                int joinSteps = Math.max(1,
                    (int) Math.ceil(angle * 180.0D
                        / (18.0D * Math.PI)));
                if (angle > 0.0001F) {
                    float rnormalX = -normalX;
                    float rnormalY = -normalY;
                    if (signedAngle < 0.0F) {
                        float previousX = p1[0] - rnormalX;
                        float previousY = p1[1] - rnormalY;
                        for (int j = 1; j <= joinSteps; j++) {
                            float[] fan = rotate(rnormalX, rnormalY,
                                -signedAngle * j / joinSteps);
                            float fanX = p1[0] - fan[0];
                            float fanY = p1[1] - fan[1];
                            putColorVertex(buffer, p1, color1);
                            putColorVertex(buffer, new float[] {previousX, previousY}, color1);
                            putColorVertex(buffer, new float[] {fanX, fanY}, color1);
                            previousX = fanX;
                            previousY = fanY;
                        }
                    } else {
                        float[] startFan = rotate(normalX, normalY, -signedAngle);
                        float previousX = p1[0] - startFan[0];
                        float previousY = p1[1] - startFan[1];
                        for (int j = joinSteps - 1; j >= 0; j--) {
                            float[] fan = rotate(normalX, normalY,
                                -signedAngle * j / joinSteps);
                            float fanX = p1[0] - fan[0];
                            float fanY = p1[1] - fan[1];
                            putColorVertex(buffer, p1, color1);
                            putColorVertex(buffer, new float[] {previousX, previousY}, color1);
                            putColorVertex(buffer, new float[] {fanX, fanY}, color1);
                            previousX = fanX;
                            previousY = fanY;
                        }
                    }
                }
            }
        }
        tessellator.draw();

        drawCapFan(points.get(0), points.get(1), halfWidth, tailColor);
        drawCapFan(points.get(n - 1), points.get(n - 2), halfWidth, tailColor);

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static float length(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    private static float[] rotate(float x, float y, double theta) {
        float cos = (float) Math.cos(theta);
        float sin = (float) Math.sin(theta);
        return new float[] {x * cos - y * sin, y * cos + x * sin};
    }

    private static void putColorVertex(BufferBuilder buffer, float[] point, int color) {
        buffer.pos(point[0], point[1], 0.0D)
            .color(channel(color, 16), channel(color, 8), channel(color, 0),
                channel(color, 24)).endVertex();
    }

    private static void drawCapFan(float[] point, float[] previous, float radius,
                                   int color) {
        float dx = point[0] - previous[0];
        float dy = point[1] - previous[1];
        float segmentLength = length(dx, dy);
        if (segmentLength < 0.0001F) {
            return;
        }
        float tangentX = dx / segmentLength * radius;
        float tangentY = dy / segmentLength * radius;
        float normalX = -tangentY;
        float normalY = tangentX;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        putColorVertex(buffer, point, color);
        for (int j = 10; j >= 0; j--) {
            float[] fan = rotate(normalX, normalY, -Math.PI * j / 10.0D);
            putColorVertex(buffer, new float[] {
                point[0] + fan[0], point[1] + fan[1]}, color);
        }
        Tessellator.getInstance().draw();
    }

    /** Hex's compact seeded simplex noise, used by makeZappy. */
    private static final SimplexNoise ZAPPY_NOISE = new SimplexNoise(9001L);

    private static final class SimplexNoise {
        private static final int[][] GRADIENTS = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}};
        private final short[] permutation = new short[512];

        private SimplexNoise(long seed) {
            java.util.Random random = new java.util.Random(seed);
            int[] source = new int[256];
            for (int i = 0; i < source.length; i++) {
                source[i] = i;
            }
            for (int i = source.length - 1; i > 0; i--) {
                int swap = random.nextInt(i + 1);
                int value = source[i];
                source[i] = source[swap];
                source[swap] = value;
            }
            for (int i = 0; i < permutation.length; i++) {
                permutation[i] = (short) source[i & 255];
            }
        }

        private double value(double x, double y, double z) {
            final double skew = 1.0D / 3.0D;
            final double unskew = 1.0D / 6.0D;
            double s = (x + y + z) * skew;
            int i = fastFloor(x + s);
            int j = fastFloor(y + s);
            int k = fastFloor(z + s);
            double t = (i + j + k) * unskew;
            double x0 = x - (i - t);
            double y0 = y - (j - t);
            double z0 = z - (k - t);

            int i1;
            int j1;
            int k1;
            int i2;
            int j2;
            int k2;
            if (x0 >= y0) {
                if (y0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0; }
                else if (x0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1; }
                else { i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1; }
            } else if (y0 < z0) {
                i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1;
            } else if (x0 < z0) {
                i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1;
            } else {
                i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0;
            }

            double x1 = x0 - i1 + unskew;
            double y1 = y0 - j1 + unskew;
            double z1 = z0 - k1 + unskew;
            double x2 = x0 - i2 + 2.0D * unskew;
            double y2 = y0 - j2 + 2.0D * unskew;
            double z2 = z0 - k2 + 2.0D * unskew;
            double x3 = x0 - 1.0D + 3.0D * unskew;
            double y3 = y0 - 1.0D + 3.0D * unskew;
            double z3 = z0 - 1.0D + 3.0D * unskew;

            double n0 = contribution(i, j, k, x0, y0, z0);
            double n1 = contribution(i + i1, j + j1, k + k1, x1, y1, z1);
            double n2 = contribution(i + i2, j + j2, k + k2, x2, y2, z2);
            double n3 = contribution(i + 1, j + 1, k + 1, x3, y3, z3);
            return 32.0D * (n0 + n1 + n2 + n3);
        }

        private double contribution(int x, int y, int z,
                                    double dx, double dy, double dz) {
            double radius = 0.6D - dx * dx - dy * dy - dz * dz;
            if (radius <= 0.0D) {
                return 0.0D;
            }
            int hash = permutation[(x + permutation[(y + permutation[z & 255]) & 255]) & 255] % 12;
            int[] gradient = GRADIENTS[hash];
            radius *= radius;
            return radius * radius
                * (gradient[0] * dx + gradient[1] * dy + gradient[2] * dz);
        }

        private static int fastFloor(double value) {
            int floor = (int) value;
            return value < floor ? floor - 1 : floor;
        }
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

    /** Hex's connection points use the dodged head color in 1.20.1. */
    private void drawConnectionSpot(float x, float y, int glowColor, int coreColor) {
        drawConnectionSpot(x, y, glowColor, coreColor, false);
    }

    private void drawConnectionSpot(float x, float y, int glowColor, int coreColor,
                                    boolean duplicate) {
        // The duplicate point is drawn again by the upstream renderer; it is
        // not a separate white marker.  Using the same radius and color also
        // prevents valid blue/purple/yellow paths from acquiring a red-white
        // node tint.
        drawHexSpot(x, y, 2.0F, dodgeColor(coreColor));
    }

    private static int dodgeColor(int argb) {
        int alpha = channel(argb, 24);
        int red = Math.round(channel(argb, 16) * 0.9F);
        int green = Math.round(channel(argb, 8) * 0.9F);
        int blue = Math.round(channel(argb, 0) * 0.9F);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
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
        // Match Hex's drawStart: clamp the click before converting it to a
        // grid coordinate so clicks at the screen edge cannot start outside
        // the visible casting area.
        int clampedMouseX = Math.max(0, Math.min(width, mouseX));
        int clampedMouseY = Math.max(0, Math.min(height, mouseY));
        GridPoint start = pxToCoord(clampedMouseX, clampedMouseY);
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
        int clampedMouseX = Math.max(0, Math.min(width, mouseX));
        int clampedMouseY = Math.max(0, Math.min(height, mouseY));
        int[] anchorPixel = coordToPx(current);
        double dx = clampedMouseX - anchorPixel[0];
        double dy = clampedMouseY - anchorPixel[1];
        double hexSize = hexSize();
        double snapDistance = hexSize * hexSize * SNAP_DISTANCE_FACTOR;
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
        boolean backtracking = currentPoints.size() > 1
            && next.equals(currentPoints.get(currentPoints.size() - 2));
        if (!backtracking && usedSpots.contains(next)) {
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
            postChatMessage(status);
            resetWorkingPath();
            return;
        }

        GridPoint origin = currentPoints.get(0);
        HexPattern submittedPattern = workingPattern;
        HexActionRegistry.bootstrap();
        HexAction action = HexActionRegistry.get(
            submittedPattern, mc == null ? null : mc.world);
        ResourceLocation id = action == null ? null : HexActionRegistry.idFor(action);

        // Install the local path before sending the snapshot.  Integrated
        // server packets can complete during this call, so the result handler
        // must already be able to find the new path by its index.
        savedPatterns.add(submittedPattern);
        savedOrigins.add(origin);
        if (id != null) {
            programIds.add(id);
        }
        programCount++;
        drawnPaths.add(new DrawnPath(submittedPattern,
            new ArrayList<>(currentPoints), id, RESOLUTION_UNRESOLVED));
        usedSpots.addAll(currentPoints);
        sendProgramSnapshot();
        currentPoints.clear();
        current = null;
        workingPattern = null;
        if (id == null && !SpecialPatternResolver.isSpecial(submittedPattern)) {
            status = I18n.format("hexcasting.message.pattern_unregistered",
                patternDescription(submittedPattern));
        } else if (id == null) {
            SpecialPatternResolver.Match special = SpecialPatternResolver.match(submittedPattern);
            String description = special.getKind() == SpecialPatternResolver.Kind.NUMBER
                ? I18n.format("hexcasting.special.number", special.getNumber())
                : I18n.format("hexcasting.special.mask", special.maskSignature());
            status = I18n.format("hexcasting.message.program_added", description,
                programCount, ItemHexStaff.MAX_PROGRAM_SIZE);
        } else {
            status = I18n.format("hexcasting.message.program_added", localizeAction(id),
                programCount, ItemHexStaff.MAX_PROGRAM_SIZE);
        }
        // GuiNewChat is the lower-left feedback surface used by Hex. Keep the
        // drawing screen itself unobstructed so the message scrolls and fades
        // exactly like normal chat.
        // An unregistered pattern is reported authoritatively by the server
        // while it evaluates the newly appended program.  Posting it here as
        // well makes the same error appear twice in the lower-left chat.
        if (id != null || SpecialPatternResolver.isSpecial(submittedPattern)) {
            postChatMessage(status);
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
        boolean backtracking = currentPoints.size() > 1
            && next.equals(currentPoints.get(currentPoints.size() - 2));
        if (!closing && !backtracking && usedSpots.contains(next)) {
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
        if (direction == last.rotatedBy(HexAngle.BACK)) {
            // The first segment has no turn angle to remove. Treat returning
            // to the start as a real undo of that segment; otherwise the
            // first line remains in the working path and cannot be withdrawn.
            if (currentPoints.size() <= 2 || workingPattern.getAngles().isEmpty()) {
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
        return pattern == null ? I18n.format("hexcasting.tooltip.pattern")
            : HexInline.formatPattern(pattern);
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
                ItemHexStaff.clearProgram(mc.player, hand, mc.player.getHeldItem(hand));
                HexClientEffects.clearSpiralPatterns(mc.player.getUniqueID());
            }
            PaucalAPI.sendToServer(new MsgStaffPatternC2S(
                hand,
                mc == null || mc.player == null ? ""
                    : ItemHexStaff.getInstanceId(mc.player.getHeldItem(hand)),
                (ResourceLocation) null));
            programIds.clear();
            programCount = 0;
            drawnPaths.clear();
            savedPatterns.clear();
            savedOrigins.clear();
            usedSpots.clear();
            resetWorkingPath();
            drawing = false;
            status = I18n.format("hexcasting.message.program_cleared");
            stackPreview.clear();
            parenDepth = 0;
            escapeNext = false;
            postChatMessage(status);
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
            net.minecraft.item.ItemStack staff = mc.player.getHeldItem(hand);
            NBTTagList snapshot = new NBTTagList();
            for (int i = 0; i < savedPatterns.size(); i++) {
                HexPattern pattern = savedPatterns.get(i);
                GridPoint origin = i < savedOrigins.size()
                    ? savedOrigins.get(i) : new GridPoint(0, 0);
                NBTTagCompound entry = pattern.serializeToNBT();
                entry.setInteger("origin_q", origin.q);
                entry.setInteger("origin_r", origin.r);
                entry.setInteger(ItemHexStaff.KEY_RESOLUTION,
                    i < drawnPaths.size()
                        ? drawnPaths.get(i).resolutionOrdinal
                        : RESOLUTION_UNRESOLVED);
                snapshot.appendTag(entry);
            }
            ItemHexStaff.replaceProgram(mc.player, hand, staff, snapshot);
            String staffInstanceId = ItemHexStaff.getInstanceId(staff);
            PaucalAPI.sendToServer(new MsgStaffPatternC2S(
                hand, staffInstanceId, snapshot));
            return;
        }
        PaucalAPI.sendToServer(new MsgStaffPatternC2S(
            hand, "", new ArrayList<>(savedPatterns), originQ, originR));
    }

    public void refreshProgram() {
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
            mc.player, hand, mc.player.getHeldItem(hand))) {
            programCount++;
            ResourceLocation id = entry.getActionId();
            if (id != null) {
                programIds.add(id);
            }
            GridPoint origin = new GridPoint(entry.getOriginQ(), entry.getOriginR());
            savedPatterns.add(entry.getPattern());
            savedOrigins.add(origin);
            List<GridPoint> points = patternPoints(entry.getPattern(), origin);
            int resolution = entry.getResolutionOrdinal();
            drawnPaths.add(new DrawnPath(entry.getPattern(), points, id, resolution));
            usedSpots.addAll(points);
        }
    }

    public void refreshFromServer() {
        if (!drawing) {
            refreshProgram();
        }
    }

    public void showCastResult(String message) {
        if (message != null && !message.isEmpty()) {
            status = message;
        }
    }

    public void showCastResult(String message, int patternIndex,
                               int resolutionOrdinal, List<String> preview,
                               int newParenDepth, boolean newEscapeNext) {
        if (message != null && !message.isEmpty()) {
            status = message;
        }
        stackPreview = preview == null ? new ArrayList<>()
            : new ArrayList<>(preview);
        parenDepth = Math.max(0, newParenDepth);
        escapeNext = newEscapeNext;
        if (resolutionOrdinal == RESOLUTION_UNDONE) {
            // UNDO is represented by the Evanition pattern that was just
            // drawn, but the line being visually undone is the nearest older
            // ESCAPED/open-parenthesis pattern. This is the same small client
            // side remapping used by 1.20.1 GuiSpellcasting.
            int searchFrom = patternIndex >= 0
                ? Math.min(patternIndex - 1, drawnPaths.size() - 1)
                : drawnPaths.size() - 1;
            for (int index = searchFrom; index >= 0; index--) {
                if (canBeUndone(drawnPaths.get(index))) {
                    drawnPaths.get(index).resolutionOrdinal = RESOLUTION_UNDONE;
                    persistResolution(index, RESOLUTION_UNDONE);
                    break;
                }
            }
            if (patternIndex >= 0 && patternIndex < drawnPaths.size()) {
                drawnPaths.get(patternIndex).resolutionOrdinal = RESOLUTION_EVALUATED;
                persistResolution(patternIndex, RESOLUTION_EVALUATED);
            }
        } else if (patternIndex >= 0 && patternIndex < drawnPaths.size()) {
            drawnPaths.get(patternIndex).resolutionOrdinal = resolutionOrdinal;
            persistResolution(patternIndex, resolutionOrdinal);
        }

        boolean errored = resolutionOrdinal == RESOLUTION_ERRORED
            || resolutionOrdinal == RESOLUTION_INVALID;
        if (!errored && stackPreview.isEmpty() && parenDepth == 0 && !escapeNext) {
            // The modern screen closes as soon as the stack/local state is
            // completely clear. Introspection deliberately does not satisfy
            // this because its parenthesis depth is still non-zero.
            if (mc != null) {
                mc.displayGuiScreen(null);
            }
        }
    }

    /** Keep the client-held stack in step with the status just received from the server. */
    private void persistResolution(int patternIndex, int resolutionOrdinal) {
        if (mc == null || mc.player == null || patternIndex < 0) {
            return;
        }
        StaffCastExecutor.Resolution[] values = StaffCastExecutor.Resolution.values();
        if (resolutionOrdinal < 0 || resolutionOrdinal >= values.length) {
            return;
        }
        ItemHexStaff.setProgramResolution(
            mc.player.getHeldItem(hand), patternIndex, values[resolutionOrdinal]);
    }

    private static boolean canBeUndone(DrawnPath path) {
        if (path == null) {
            return false;
        }
        if (path.resolutionOrdinal == RESOLUTION_ESCAPED) {
            return true;
        }
        if (path.resolutionOrdinal != RESOLUTION_EVALUATED
            || path.pattern == null) {
            return false;
        }
        List<HexAngle> angles = path.pattern.getAngles();
        return angles.equals(HexActions.OPEN_PAREN_PATTERN.getAngles())
            || angles.equals(HexActions.OPEN_N_PARENS_PATTERN.getAngles())
            || angles.equals(HexActions.READ_INTO_PARENS_PATTERN.getAngles());
    }

    private static final class ResolutionColors {
        private final int outerStart;
        private final int outerEnd;
        private final int innerStart;
        private final int innerEnd;
        private final int node;

        private ResolutionColors(int outerStart, int outerEnd,
                                 int innerStart, int innerEnd, int node) {
            this.outerStart = outerStart;
            this.outerEnd = outerEnd;
            this.innerStart = innerStart;
            this.innerEnd = innerEnd;
            this.node = node;
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

    /**
     * Match GuiSpellcasting.hexSize(): the grid is sized from the available
     * screen area instead of using a fixed pixel distance. This keeps the
     * angular snap sectors, connection nodes, and preview path proportional on
     * both small and large 1.12.2 windows.
     */
    private float hexSize() {
        if (width <= 0 || height <= 0) {
            return 42.0F;
        }
        return (float) Math.sqrt(width * (double) height / 512.0D);
    }

    /** Exact axial-to-cube rounding, matching Hex's pixel-to-coordinate snap. */
    private GridPoint pxToCoord(int mouseX, int mouseY) {
        double hexSize = hexSize();
        double x = (mouseX - width / 2.0D) / hexSize;
        double y = (mouseY - height / 2.0D) / hexSize;
        double qf = (1.7320508D / 3.0D * x) - (0.33333D * y);
        double rf = (0.66666D * y);
        int q = (int) Math.round(qf);
        int r = (int) Math.round(rf);
        qf -= q;
        rf -= r;
        if (Math.abs(q) >= Math.abs(r)) {
            return new GridPoint(q + (int) Math.round(qf + 0.5D * rf), r);
        }
        return new GridPoint(q, r + (int) Math.round(rf + 0.5D * qf));
    }

    private int[] coordToPx(GridPoint point) {
        double hexSize = hexSize();
        double x = width / 2.0D
            + (1.7320508D * point.q + 0.8660254D * point.r) * hexSize;
        double y = height / 2.0D + 1.5D * point.r * hexSize;
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

    private static final class ZappyPath {
        private final List<float[]> points;
        /** One source-stroke index for each line from points[i] to points[i+1]. */
        private final List<Integer> segmentIndices;
        private final int sourceSegmentCount;

        private ZappyPath(List<float[]> points, List<Integer> segmentIndices,
                          int sourceSegmentCount) {
            this.points = points;
            this.segmentIndices = segmentIndices;
            this.sourceSegmentCount = Math.max(1, sourceSegmentCount);
        }
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
        private int resolutionOrdinal;

        private DrawnPath(HexPattern pattern, List<GridPoint> points,
                          ResourceLocation id, int resolutionOrdinal) {
            this.pattern = pattern;
            this.points = points;
            this.id = id;
            this.resolutionOrdinal = resolutionOrdinal;
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
