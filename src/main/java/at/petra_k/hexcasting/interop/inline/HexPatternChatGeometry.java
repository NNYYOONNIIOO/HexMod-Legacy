package at.petra_k.hexcasting.interop.inline;

import at.petra_k.hexcasting.api.casting.math.HexCoord;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The small pattern glyph used by Inline in chat.
 *
 * <p>The modern renderer lays the pattern out in the same flat-top axial
 * coordinate system used by {@code HexPattern}: an east step is sqrt(3)
 * units wide and a north/south step is 1.5 units tall. The old port used a
 * rectangular six-direction approximation and filled squares at every point;
 * that is why its glyphs were both too large and visibly blocky. This class
 * keeps the 1.20.1 inline dimensions (9 px high, 1 px stroke) while using
 * 1.12.2's immediate-mode GUI primitives.</p>
 */
final class HexPatternChatGeometry {
    private static final double SQRT_3 = Math.sqrt(3.0D);
    private static final double BASE_SCALE = 4.0D / 1.5D;
    private static final double TARGET_HEIGHT = 9.0D;
    private static final double VERTICAL_PADDING = 0.5D;
    private static final double STROKE_WIDTH = 1.0D;
    private static final int STROKE_RED = 245;
    private static final int STROKE_GREEN = 245;
    private static final int STROKE_BLUE = 245;
    // PatternTooltipComponent uses WorldlyPatternRenderHelpers.READABLE_SCROLL_SETTINGS.
    // Keep these values in pose-space units and convert them to the 128 px tooltip
    // space only after the pattern has been fitted, just like PatternRenderer.
    private static final double READABLE_PADDING = 2.0D / 16.0D;
    private static final double READABLE_BASE_SCALE = 0.25D / 1.5D;
    private static final double READABLE_OUTER_WIDTH = 0.8D / 16.0D;
    private static final double READABLE_INNER_WIDTH = READABLE_OUTER_WIDTH * 2.0D / 5.0D;
    private static final double READABLE_START_DOT_RADIUS =
        0.8D * READABLE_OUTER_WIDTH * 2.0D / 5.0D;
    private static final double READABLE_GRID_DOT_RADIUS =
        0.4D * READABLE_OUTER_WIDTH * 2.0D / 5.0D;
    private static final int READABLE_HOPS = 10;
    private static final double READABLE_VARIANCE = 0.5D;
    private static final double READABLE_FLOW_IRREGULAR = 0.2D;
    private static final double READABLE_OFFSET = 0.2D;
    private static final double READABLE_LAST_SEGMENT = 0.8D;
    private static final SimplexNoise READABLE_NOISE = new SimplexNoise(9001L);
    // The previous two alignment corrections had already moved the glyph
    // down by eight pixels.  Keep the chat text origin as the final origin so
    // the pattern sits on the same baseline as 1.20.1.
    private static final int CHAT_VERTICAL_OFFSET = 0;

    private HexPatternChatGeometry() {
    }

    /** Return the width reserved by the modern inline renderer, in GUI pixels. */
    static int width(String signature) {
        Layout layout = layout(signature);
        if (layout == null) {
            return 4;
        }
        if (layout.rangeY <= 0.000001D) {
            return Math.max(1, (int) Math.ceil(layout.rangeX * BASE_SCALE) + 1);
        }
        double baseHeight = layout.rangeY * BASE_SCALE;
        return Math.max(1, (int) Math.ceil(
            Math.min(baseHeight, 8.0D) * layout.rangeX / layout.rangeY) + 1);
    }

    /** Draw one glyph at the same top-left origin used by vanilla chat text. */
    static void draw(String signature, int x, int y) {
        draw(signature, x, y, 255);
    }

    /** Draw one glyph with the same alpha fade as its vanilla chat line. */
    static void draw(String signature, int x, int y, int alpha) {
        drawInternal(signature, x + 2, y + CHAT_VERTICAL_OFFSET, alpha,
            STROKE_RED, STROKE_GREEN, STROKE_BLUE);
    }

    /** Draw one glyph in another GUI using the supplied text origin. */
    static void drawAt(String signature, int x, int y, int alpha, int argb) {
        drawInternal(signature, x, y, alpha,
            (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
    }

    /**
     * Draw the large pattern image used by a scroll tooltip.  The 1.20.1
     * renderer fits the complete path into the 128 px scroll image; reusing
     * the same axial layout here keeps the tooltip preview geometrically
     * identical to the inline glyph instead of falling back to text art.
     */
    static void drawPreview(HexPattern pattern, int x, int y, int size, int alpha,
                            int outerArgb, int innerArgb) {
        if (pattern == null || size <= 0 || alpha <= 3) {
            return;
        }
        PreviewLayout preview = readablePreviewLayout(pattern, x, y, size);
        if (preview == null || preview.linePoints.size() < 2) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(
            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        double outerWidth = size * READABLE_OUTER_WIDTH;
        double innerWidth = size * READABLE_INNER_WIDTH;
        drawLineSequence(buffer, preview.linePoints, outerWidth, alpha,
            (outerArgb >> 16) & 0xFF, (outerArgb >> 8) & 0xFF, outerArgb & 0xFF);
        drawLineSequence(buffer, preview.linePoints, innerWidth, alpha,
            (innerArgb >> 16) & 0xFF, (innerArgb >> 8) & 0xFF, innerArgb & 0xFF);
        tessellator.draw();

        // Match PatternColors.DEFAULT_PATTERN_COLOR.withDots(true, true).
        // The radii are deliberately small: they are 0.016 and 0.008 pose
        // units at the 128 px render size, not a fraction of each grid step.
        drawPreviewDot(preview.dots.get(0), size * READABLE_START_DOT_RADIUS,
            alpha,
            0x5B, 0x7B, 0xD7);
        int gridAlpha = alpha * 0x80 / 0xFF;
        for (int i = 1; i < preview.dots.size(); i++) {
            drawPreviewDot(preview.dots.get(i), size * READABLE_GRID_DOT_RADIUS,
                gridAlpha, (outerArgb >> 16) & 0xFF,
                (outerArgb >> 8) & 0xFF, outerArgb & 0xFF);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawInternal(String signature, int x, int y, int alpha,
                                     int red, int green, int blue) {
        Layout layout = layout(signature);
        if (layout == null || layout.points.size() < 2 || alpha <= 3) {
            return;
        }

        GlStateManager.pushMatrix();
        // The chat overload applies the eight-pixel correction requested for
        // the lower-left chat glyph. Other GUI surfaces use their text origin
        // directly so tooltips and the stack preview stay aligned.
        GlStateManager.translate(x, y, 0.0D);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(
            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha / 255.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        drawLineSequence(buffer, layout.points, STROKE_WIDTH, alpha,
            red, green, blue);
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static Layout layout(String signature) {
        if (signature == null || signature.isEmpty()) {
            return null;
        }

        final HexPattern pattern;
        try {
            pattern = HexPattern.fromSignature(signature);
        } catch (RuntimeException ignored) {
            return null;
        }

        List<HexCoord> positions = pattern.positions();
        if (positions.isEmpty()) {
            return null;
        }

        List<Point> rawPoints = new ArrayList<>(positions.size());
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (HexCoord position : positions) {
            // This is HexUtils.coordToPx(coord, 1, Vec2.ZERO) from Hex.
            double rawX = SQRT_3 * position.getQ()
                + (SQRT_3 * 0.5D) * position.getR();
            double rawY = 1.5D * position.getR();
            Point point = new Point(rawX, rawY);
            rawPoints.add(point);
            minX = Math.min(minX, rawX);
            minY = Math.min(minY, rawY);
            maxX = Math.max(maxX, rawX);
            maxY = Math.max(maxY, rawY);
        }

        double rangeX = maxX - minX;
        double rangeY = maxY - minY;
        double baseHeight = rangeY * BASE_SCALE;
        double fitScale = baseHeight <= 0.000001D
            ? 1.0D
            : Math.min(1.0D,
                (TARGET_HEIGHT - (2.0D * VERTICAL_PADDING) - STROKE_WIDTH)
                    / baseHeight);
        double finalScale = BASE_SCALE * fitScale;
        double scaledWidth = rangeX * finalScale;
        double scaledHeight = rangeY * finalScale;
        double inherentHeight = scaledHeight
            + (2.0D * VERTICAL_PADDING) + STROKE_WIDTH;
        double heightDiff = Math.max(TARGET_HEIGHT - inherentHeight, 0.0D);
        double offsetY = ((inherentHeight - scaledHeight) * 0.5D)
            + (heightDiff * 0.5D);

        // Inline's horizontal axis is CENTER (not CENTER_FIT). It only has
        // one pixel of stroke space and may grow to the pattern's width.
        double inherentWidth = scaledWidth + STROKE_WIDTH;
        double widthDiff = Math.max(1.0D - inherentWidth, 0.0D);
        double offsetX = STROKE_WIDTH * 0.5D + widthDiff * 0.5D;

        List<Point> points = new ArrayList<>(rawPoints.size());
        for (Point raw : rawPoints) {
            points.add(new Point(
                (raw.x - minX) * finalScale + offsetX,
                (raw.y - minY) * finalScale + offsetY));
        }
        return new Layout(points, rangeX, rangeY);
    }

    /** Draw a line ribbon with RenderLib's mitered segments and round caps. */
    private static void drawLineSequence(BufferBuilder buffer,
                                         List<Point> points,
                                         double width,
                                         int alpha,
                                         int red,
                                         int green,
                                         int blue) {
        if (points.size() <= 1 || width <= 0.0D) {
            return;
        }
        double radius = width * 0.5D;
        int count = points.size();
        double[] joinAngles = new double[count];
        double[] joinOffsets = new double[count];
        for (int i = 2; i < count; i++) {
            Point p0 = points.get(i - 2);
            Point p1 = points.get(i - 1);
            Point p2 = points.get(i);
            double prevX = p1.x - p0.x;
            double prevY = p1.y - p0.y;
            double nextX = p2.x - p1.x;
            double nextY = p2.y - p1.y;
            double prevLength = length(prevX, prevY);
            double nextLength = length(nextX, nextY);
            if (prevLength <= 0.000001D || nextLength <= 0.000001D) {
                continue;
            }
            double angle = Math.atan2(prevX * nextY - prevY * nextX,
                prevX * nextX + prevY * nextY);
            joinAngles[i - 1] = angle;
            double clamp = Math.min(prevLength, nextLength) / radius;
            double denominator = 1.0D + Math.cos(angle);
            joinOffsets[i - 1] = denominator <= 0.000001D ? 0.0D
                : Math.max(-clamp, Math.min(clamp, Math.sin(angle) / denominator));
        }

        for (int i = 0; i < count - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            double dx = p2.x - p1.x;
            double dy = p2.y - p1.y;
            double segmentLength = length(dx, dy);
            if (segmentLength <= 0.000001D) {
                continue;
            }
            double tangentX = dx / segmentLength * radius;
            double tangentY = dy / segmentLength * radius;
            double normalX = -tangentY;
            double normalY = tangentX;
            double low = joinOffsets[i];
            double high = joinOffsets[i + 1];

            Point p1Down = new Point(
                p1.x + tangentX * Math.max(0.0D, low) + normalX,
                p1.y + tangentY * Math.max(0.0D, low) + normalY);
            Point p1Up = new Point(
                p1.x + tangentX * Math.max(0.0D, -low) - normalX,
                p1.y + tangentY * Math.max(0.0D, -low) - normalY);
            Point p2Down = new Point(
                p2.x - tangentX * Math.max(0.0D, high) + normalX,
                p2.y - tangentY * Math.max(0.0D, high) + normalY);
            Point p2Up = new Point(
                p2.x - tangentX * Math.max(0.0D, -high) - normalX,
                p2.y - tangentY * Math.max(0.0D, -high) - normalY);

            vertex(buffer, p1Down, alpha, red, green, blue);
            vertex(buffer, p1, alpha, red, green, blue);
            vertex(buffer, p1Up, alpha, red, green, blue);
            vertex(buffer, p1Down, alpha, red, green, blue);
            vertex(buffer, p1Up, alpha, red, green, blue);
            vertex(buffer, p2Up, alpha, red, green, blue);
            vertex(buffer, p1Down, alpha, red, green, blue);
            vertex(buffer, p2Up, alpha, red, green, blue);
            vertex(buffer, p2, alpha, red, green, blue);
            vertex(buffer, p1Down, alpha, red, green, blue);
            vertex(buffer, p2, alpha, red, green, blue);
            vertex(buffer, p2Down, alpha, red, green, blue);

            if (i > 0) {
                drawJoin(buffer, p1, normalX, normalY, joinAngles[i],
                    alpha, red, green, blue);
            }
        }
        drawCapFan(buffer, points.get(0), points.get(1), radius,
            alpha, red, green, blue);
        drawCapFan(buffer, points.get(count - 1), points.get(count - 2), radius,
            alpha, red, green, blue);
    }

    private static void vertex(BufferBuilder buffer, double x, double y, int alpha,
                               int red, int green, int blue) {
        buffer.pos(x, y, 0.0D)
            .color(red, green, blue, alpha)
            .endVertex();
    }

    private static void vertex(BufferBuilder buffer, Point point, int alpha,
                               int red, int green, int blue) {
        vertex(buffer, point.x, point.y, alpha, red, green, blue);
    }

    private static void drawPreviewDot(Point point, double radius, int alpha,
                                       int red, int green, int blue) {
        if (point == null || radius <= 0.0D || alpha <= 0) {
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(point.x, point.y, 0.0D).color(red, green, blue, alpha).endVertex();
        for (int i = 0; i <= 6; i++) {
            double angle = Math.PI * 2.0D * i / 6.0D;
            buffer.pos(point.x + Math.cos(angle) * radius,
                point.y + Math.sin(angle) * radius, 0.0D)
                .color(red, green, blue, alpha).endVertex();
        }
        tessellator.draw();
    }

    private static void drawJoin(BufferBuilder buffer, Point point,
                                 double normalX, double normalY, double signedAngle,
                                 int alpha, int red, int green, int blue) {
        double angle = Math.abs(signedAngle);
        if (angle <= 0.000001D) {
            return;
        }
        int joinSteps = Math.max(1, (int) Math.ceil(
            angle * 180.0D / (18.0D * Math.PI)));
        double rnormalX = -normalX;
        double rnormalY = -normalY;
        if (signedAngle < 0.0D) {
            double[] previous = new double[] {
                point.x - rnormalX, point.y - rnormalY};
            for (int j = 1; j <= joinSteps; j++) {
                double[] fan = rotate(rnormalX, rnormalY,
                    -signedAngle * j / joinSteps);
                Point current = new Point(point.x - fan[0], point.y - fan[1]);
                vertex(buffer, point, alpha, red, green, blue);
                vertex(buffer, previous[0], previous[1], alpha, red, green, blue);
                vertex(buffer, current, alpha, red, green, blue);
                previous = new double[] {current.x, current.y};
            }
        } else {
            double[] start = rotate(normalX, normalY, -signedAngle);
            double[] previous = new double[] {
                point.x - start[0], point.y - start[1]};
            for (int j = joinSteps - 1; j >= 0; j--) {
                double[] fan = rotate(normalX, normalY,
                    -signedAngle * j / joinSteps);
                Point current = new Point(point.x - fan[0], point.y - fan[1]);
                vertex(buffer, point, alpha, red, green, blue);
                vertex(buffer, previous[0], previous[1], alpha, red, green, blue);
                vertex(buffer, current, alpha, red, green, blue);
                previous = new double[] {current.x, current.y};
            }
        }
    }

    private static void drawCapFan(BufferBuilder buffer, Point point, Point previous,
                                   double radius, int alpha, int red, int green,
                                   int blue) {
        double dx = point.x - previous.x;
        double dy = point.y - previous.y;
        double segmentLength = length(dx, dy);
        if (segmentLength <= 0.000001D) {
            return;
        }
        double tangentX = dx / segmentLength * radius;
        double tangentY = dy / segmentLength * radius;
        double normalX = -tangentY;
        double normalY = tangentX;
        Point first = rotatedPoint(point, normalX, normalY, -Math.PI);
        Point last = first;
        for (int j = 1; j <= 10; j++) {
            Point current = rotatedPoint(point, normalX, normalY,
                -Math.PI + Math.PI * j / 10.0D);
            vertex(buffer, point, alpha, red, green, blue);
            vertex(buffer, last, alpha, red, green, blue);
            vertex(buffer, current, alpha, red, green, blue);
            last = current;
        }
    }

    private static Point rotatedPoint(Point center, double x, double y, double theta) {
        double[] rotated = rotate(x, y, theta);
        return new Point(center.x + rotated[0], center.y + rotated[1]);
    }

    private static double[] rotate(double x, double y, double theta) {
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        return new double[] {x * cos - y * sin, y * cos + x * sin};
    }

    private static double length(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    private static PreviewLayout readablePreviewLayout(HexPattern pattern,
                                                        int originX, int originY,
                                                        int size) {
        List<Point> dots = new ArrayList<>();
        for (HexCoord position : pattern.positions()) {
            dots.add(new Point(
                SQRT_3 * position.getQ() + SQRT_3 * 0.5D * position.getR(),
                1.5D * position.getR()));
        }
        if (dots.size() < 2) {
            return null;
        }

        Set<Integer> duplicates = new HashSet<>();
        for (int i = 0; i < dots.size(); i++) {
            for (int previous = 0; previous < i; previous++) {
                if (samePoint(dots.get(i), dots.get(previous))) {
                    duplicates.add(i);
                    duplicates.add(previous);
                }
            }
        }
        List<Point> zappy = makeReadableZappy(dots, duplicates, 0.0D);
        if (zappy.size() < 2) {
            return null;
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Point point : zappy) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }
        double rangeX = Math.max(0.000001D, maxX - minX);
        double rangeY = Math.max(0.000001D, maxY - minY);
        double baseWidth = rangeX * READABLE_BASE_SCALE;
        double baseHeight = rangeY * READABLE_BASE_SCALE;
        double scale = 1.0D;
        if (baseHeight > 0.000001D) {
            scale = Math.min(scale,
                (1.0D - 2.0D * READABLE_PADDING - READABLE_OUTER_WIDTH)
                    / baseHeight);
        }
        if (baseWidth > 0.000001D) {
            scale = Math.min(scale,
                (1.0D - 2.0D * READABLE_PADDING - READABLE_OUTER_WIDTH)
                    / baseWidth);
        }
        scale = Math.max(0.000001D, scale);
        double finalScale = READABLE_BASE_SCALE * scale;
        double inherentWidth = baseWidth * scale
            + 2.0D * READABLE_PADDING + READABLE_OUTER_WIDTH;
        double inherentHeight = baseHeight * scale
            + 2.0D * READABLE_PADDING + READABLE_OUTER_WIDTH;
        double widthDiff = Math.max(1.0D - inherentWidth, 0.0D);
        double heightDiff = Math.max(1.0D - inherentHeight, 0.0D);
        double offsetX = (inherentWidth - baseWidth * scale) * 0.5D
            + widthDiff * 0.5D;
        double offsetY = (inherentHeight - baseHeight * scale) * 0.5D
            + heightDiff * 0.5D;

        List<Point> scaledLines = new ArrayList<>(zappy.size());
        for (Point point : zappy) {
            scaledLines.add(new Point(xForPreview(point.x, minX, finalScale,
                    offsetX, originX, size),
                xForPreview(point.y, minY, finalScale, offsetY, originY, size)));
        }
        List<Point> scaledDots = new ArrayList<>(dots.size());
        for (Point point : dots) {
            scaledDots.add(new Point(xForPreview(point.x, minX, finalScale,
                    offsetX, originX, size),
                xForPreview(point.y, minY, finalScale, offsetY, originY, size)));
        }
        return new PreviewLayout(scaledLines, scaledDots);
    }

    private static double xForPreview(double value, double min,
                                      double scale, double offset, int origin,
                                      int size) {
        return origin + (value - min) * scale * size + offset * size;
    }

    private static boolean samePoint(Point first, Point second) {
        return first.x == second.x && first.y == second.y;
    }

    private static List<Point> makeReadableZappy(List<Point> barePoints,
                                                  Set<Integer> duplicateIndices,
                                                  double seed) {
        if (duplicateIndices == null || duplicateIndices.isEmpty()) {
            return zappify(barePoints, true, seed, 0);
        }
        List<Point> output = new ArrayList<>(barePoints.size() * READABLE_HOPS);
        List<Point> chain = new ArrayList<>();
        int chainStart = 0;
        for (int i = 0; i + 1 < barePoints.size(); i++) {
            Point head = barePoints.get(i);
            Point tail = barePoints.get(i + 1);
            double tangentX = (tail.x - head.x) * READABLE_OFFSET;
            double tangentY = (tail.y - head.y) * READABLE_OFFSET;
            if (i != 0 && duplicateIndices.contains(i)) {
                chain.add(new Point(head.x + tangentX, head.y + tangentY));
            } else {
                chain.add(new Point(head.x, head.y));
            }

            if (i == barePoints.size() - 2) {
                chain.add(new Point(tail.x, tail.y));
                output.addAll(zappify(chain, true, seed, chainStart));
            } else if (duplicateIndices.contains(i + 1)) {
                chain.add(new Point(tail.x - tangentX, tail.y - tangentY));
                output.addAll(zappify(chain, false, seed, chainStart));
                chain.clear();
                chainStart = i + 1;
            }
        }
        return output;
    }

    private static List<Point> zappify(List<Point> points, boolean truncateLast,
                                       double seed, int segmentOffset) {
        List<Point> output = new ArrayList<>(points.size() * READABLE_HOPS);
        if (points.isEmpty()) {
            return output;
        }
        output.add(points.get(0));
        for (int i = 0; i + 1 < points.size(); i++) {
            Point source = points.get(i);
            Point target = points.get(i + 1);
            double dx = target.x - source.x;
            double dy = target.y - source.y;
            double distance = length(dx, dy);
            double hopDistance = distance / READABLE_HOPS;
            double maxVariance = hopDistance * READABLE_VARIANCE;
            int maxJ = truncateLast && i == points.size() - 2
                ? (int) Math.round(READABLE_LAST_SEGMENT * READABLE_HOPS)
                : READABLE_HOPS;
            for (int j = 1; j <= maxJ; j++) {
                double progress = j / (double) (READABLE_HOPS + 1);
                double px = source.x + dx * progress;
                double py = source.y + dy * progress;
                double minorPerturb = readableNoise(i, j, Math.sin(0.0D))
                    * READABLE_FLOW_IRREGULAR;
                double theta = 3.0D * readableNoise(
                    i + progress + minorPerturb, 1337.0D, seed)
                    * Math.PI * 2.0D;
                double scaleVariance = Math.min(1.0D,
                    8.0D * (0.5D - Math.abs(0.5D - progress)));
                double radius = readableNoise(
                    i + progress, 69420.0D, seed)
                    * maxVariance * scaleVariance;
                output.add(new Point(px + radius * Math.cos(theta),
                    py + radius * Math.sin(theta)));
                if (j == READABLE_HOPS) {
                    output.add(new Point(target.x, target.y));
                }
            }
        }
        return output;
    }

    private static double readableNoise(double x, double y, double z) {
        return READABLE_NOISE.value(x * 0.6D, y * 0.6D, z * 0.6D) / 2.0D;
    }

    /** The same seeded 3D simplex source used by Hex's RenderLib. */
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
                if (y0 >= z0) {
                    i1 = 1; j1 = 0; k1 = 0;
                    i2 = 1; j2 = 1; k2 = 0;
                } else if (x0 >= z0) {
                    i1 = 1; j1 = 0; k1 = 0;
                    i2 = 1; j2 = 0; k2 = 1;
                } else {
                    i1 = 0; j1 = 0; k1 = 1;
                    i2 = 1; j2 = 0; k2 = 1;
                }
            } else if (y0 < z0) {
                i1 = 0; j1 = 0; k1 = 1;
                i2 = 0; j2 = 1; k2 = 1;
            } else if (x0 < z0) {
                i1 = 0; j1 = 1; k1 = 0;
                i2 = 0; j2 = 1; k2 = 1;
            } else {
                i1 = 0; j1 = 1; k1 = 0;
                i2 = 1; j2 = 1; k2 = 0;
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
            double n1 = contribution(i + i1, j + j1, k + k1,
                x1, y1, z1);
            double n2 = contribution(i + i2, j + j2, k + k2,
                x2, y2, z2);
            double n3 = contribution(i + 1, j + 1, k + 1,
                x3, y3, z3);
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

    private static final class PreviewLayout {
        private final List<Point> linePoints;
        private final List<Point> dots;

        private PreviewLayout(List<Point> linePoints, List<Point> dots) {
            this.linePoints = linePoints;
            this.dots = dots;
        }
    }

    private static final class Layout {
        private final List<Point> points;
        private final double rangeX;
        private final double rangeY;

        private Layout(List<Point> points, double rangeX, double rangeY) {
            this.points = points;
            this.rangeX = rangeX;
            this.rangeY = rangeY;
        }
    }

    private static final class Point {
        private final double x;
        private final double y;

        private Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
