package at.petra_k.hexcasting.interop.inline;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import com.samsthenerd.inline.api.InlineData;
import com.samsthenerd.inline.api.InlineDataType;
import net.minecraft.util.ResourceLocation;

import java.util.Objects;

/** Inline-style typed pattern payload for the 1.12.2 port. */
public final class InlinePatternData implements InlineData<InlinePatternData> {
    public static final InlineDataType<InlinePatternData> TYPE =
        () -> new ResourceLocation("hexcasting", "pattern");
    private final HexPattern pattern;

    public InlinePatternData(HexPattern pattern) {
        this.pattern = Objects.requireNonNull(pattern, "pattern");
    }

    public HexPattern getPattern() {
        return pattern;
    }

    @Override
    public InlineDataType<InlinePatternData> getType() {
        return TYPE;
    }

    @Override
    public ResourceLocation getRendererId() {
        return new ResourceLocation("hexcasting", "pattern");
    }

    @Override
    public InlinePatternData copy() {
        return new InlinePatternData(pattern);
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
