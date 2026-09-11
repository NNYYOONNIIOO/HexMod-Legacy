package at.petra_k.hexcasting.interop.inline;

import at.petra_k.hexcasting.api.casting.math.HexAngle;
import at.petra_k.hexcasting.api.casting.math.HexPattern;

/** Deterministic fallback renderer for Inline-style pattern payloads. */
public final class InlinePatternRenderer {
    private InlinePatternRenderer() {
    }

    /** Render a pattern without requiring a custom 1.20.1 font or renderer. */
    public static String render(HexPattern pattern) {
        if (pattern == null) {
            return "<null pattern>";
        }
        StringBuilder result = new StringBuilder(pattern.getAngles().size() * 4 + 8);
        result.append('[').append(pattern.getStartDir().name()).append(']');
        for (HexAngle angle : pattern.getAngles()) {
            result.append('/').append(angle.name());
        }
        return result.toString();
    }

    public static String render(InlinePatternData data) {
        return data == null ? "<null pattern>" : render(data.getPattern());
    }
}
