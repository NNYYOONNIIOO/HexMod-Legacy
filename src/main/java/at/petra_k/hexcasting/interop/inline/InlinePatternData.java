package at.petra_k.hexcasting.interop.inline;

import at.petra_k.hexcasting.api.casting.math.HexPattern;

import java.util.Objects;

/** Inline-style typed pattern payload for the 1.12.2 port. */
public final class InlinePatternData {
    private final HexPattern pattern;

    public InlinePatternData(HexPattern pattern) {
        this.pattern = Objects.requireNonNull(pattern, "pattern");
    }

    public HexPattern getPattern() {
        return pattern;
    }

    public String asText() {
        return InlinePatternRenderer.render(pattern);
    }

    public String asPlainText() {
        return pattern.signature();
    }

    @Override
    public String toString() {
        return asText();
    }
}
