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
     * Return the pattern's inline marker. The client-side Inline event handler
     * consumes this marker and draws the geometry with the same line/dot
     * renderer used by the staff GUI. Returning a Unicode/box-drawing diagram
     * here is deliberately avoided: that is merely character art and is the
     * bug visible in the error message.
     */
    public static String render(HexPattern pattern) {
        if (pattern == null) {
            return "";
        }
        return "\\uE000hexcasting:pattern:" + pattern.signature() + "\\uE001";
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
