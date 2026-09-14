package at.petra_k.hexcasting.interop.inline;

import at.petra_k.hexcasting.api.casting.math.HexAngle;
import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexPattern;

import java.util.ArrayList;
import java.util.List;
import com.samsthenerd.inline.api.InlineRenderContext;
import com.samsthenerd.inline.api.InlineRenderer;

/** Deterministic fallback renderer for Inline-style pattern payloads. */
public final class InlinePatternRenderer implements InlineRenderer<InlinePatternData> {
    private InlinePatternRenderer() {
    }

    public static final InlinePatternRenderer INSTANCE = new InlinePatternRenderer();

    @Override
    public String render(InlinePatternData data, InlineRenderContext context) {
        return render(data == null ? null : data.getPattern());
    }

    /**
     * Render the actual hex geometry as a compact Unicode diagram. This keeps
     * the 1.20.1 inline-pattern behavior meaningful in 1.12.2 chat, where the
     * modern font renderer is unavailable.
     */
    public static String render(HexPattern pattern) {
        if (pattern == null) {
            return "<null pattern>";
        }
        List<GridPoint> points = new ArrayList<>();
        GridPoint cursor = new GridPoint(0, 0);
        points.add(cursor);
        for (HexDir direction : pattern.directions()) {
            cursor = cursor.add(direction);
            points.add(cursor);
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (GridPoint point : points) {
            int x = point.q * 2;
            int y = point.r * 2;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        if (width > 96 || height > 48) {
            return pattern.signature();
        }
        char[][] canvas = new char[height][width];
        for (int y = 0; y < height; y++) {
            java.util.Arrays.fill(canvas[y], ' ');
        }
        for (int i = 0; i + 1 < points.size(); i++) {
            GridPoint from = points.get(i);
            GridPoint to = points.get(i + 1);
            int x1 = from.q * 2 - minX;
            int y1 = from.r * 2 - minY;
            int x2 = to.q * 2 - minX;
            int y2 = to.r * 2 - minY;
            char segment = segmentChar(x2 - x1, y2 - y1);
            put(canvas, (x1 + x2) / 2, (y1 + y2) / 2, segment);
        }
        for (int i = 0; i < points.size(); i++) {
            GridPoint point = points.get(i);
            int x = point.q * 2 - minX;
            int y = point.r * 2 - minY;
            put(canvas, x, y, i == 0 ? '\u25C6' : '\u25CF');
        }
        StringBuilder result = new StringBuilder(width * height + height);
        for (int y = 0; y < height; y++) {
            int last = width - 1;
            while (last >= 0 && canvas[y][last] == ' ') {
                last--;
            }
            if (last < 0) {
                continue;
            }
            if (result.length() > 0) {
                result.append('\n');
            }
            int first = 0;
            while (first < last && canvas[y][first] == ' ') {
                first++;
            }
            result.append(canvas[y], first, last - first + 1);
        }
        return result.toString();
    }

    private static char segmentChar(int dx, int dy) {
        if (dy == 0) {
            return '\u2500';
        }
        if (dx == 0) {
            return '\u2502';
        }
        return dx * dy < 0 ? '/' : '\\';
    }

    private static void put(char[][] canvas, int x, int y, char value) {
        if (y < 0 || y >= canvas.length || x < 0 || x >= canvas[y].length) {
            return;
        }
        char old = canvas[y][x];
        if (old == ' ' || old == value || value == '\u25C6' || value == '\u25CF') {
            canvas[y][x] = value;
        } else if (old != '\u25C6' && old != '\u25CF') {
            canvas[y][x] = '\u253C';
        }
    }

    private static final class GridPoint {
        private final int q;
        private final int r;

        private GridPoint(int q, int r) {
            this.q = q;
            this.r = r;
        }

        private GridPoint add(HexDir direction) {
            switch (direction) {
                case NORTH_EAST: return new GridPoint(q + 1, r - 1);
                case EAST: return new GridPoint(q + 1, r);
                case SOUTH_EAST: return new GridPoint(q, r + 1);
                case SOUTH_WEST: return new GridPoint(q - 1, r + 1);
                case WEST: return new GridPoint(q - 1, r);
                case NORTH_WEST: return new GridPoint(q, r - 1);
                default: throw new AssertionError(direction);
            }
        }
    }

    public static String render(InlinePatternData data) {
        return data == null ? "<null pattern>" : render(data.getPattern());
    }
}
