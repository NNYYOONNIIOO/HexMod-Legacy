package at.petra_k.hexcasting.interop.inline;

import at.petra_k.hexcasting.api.casting.math.HexPattern;

/**
 * Small Inline-compatible facade embedded for Minecraft 1.12.2.
 * It intentionally has no dependency on the 1.20.1 Inline, Pehkui, or Cloth
 * Config APIs; callers still get typed pattern data and stable text output.
 */
public final class HexInline {
    private static boolean initialized;

    private HexInline() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static InlinePatternData pattern(HexPattern pattern) {
        return new InlinePatternData(pattern);
    }

    public static String formatPattern(HexPattern pattern) {
        return pattern(pattern).asText();
    }
}
