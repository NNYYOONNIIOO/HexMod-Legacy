package at.petra_k.hexcasting.interop.inline;

import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import com.samsthenerd.inline.api.InlineMatch;
import com.samsthenerd.inline.api.InlineMatcher;
import com.samsthenerd.inline.api.MatchContext;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 1.12.2 counterpart of Hex Casting's Inline pattern matcher. */
public final class HexPatternInlineMatcher implements InlineMatcher {
    private static final Pattern MODERN_TOKEN = Pattern.compile(
        "(?:HexPattern)?(?:<|\\(|\\[|\\{)\\s*"
            + "([a-zA-Z_-]+)"
            + "(?:\\s*([,!+:; ])\\s*([aqwedsAQWEDS]+)?)?"
            + "\\s*(?:>|\\)|\\]|\\})");
    private static final Pattern EXPLICIT_TOKEN = Pattern.compile(
        "\\[pattern:([a-zA-Z_-]+):([aqwedsAQWEDS]*)\\]");

    @Override
    public InlineMatch find(String text, int fromIndex, MatchContext context) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        InlineMatch modern = findModern(text, fromIndex);
        InlineMatch explicit = findExplicit(text, fromIndex);
        if (modern == null) {
            return explicit;
        }
        if (explicit == null || modern.getStart() <= explicit.getStart()) {
            return modern;
        }
        return explicit;
    }

    private InlineMatch findModern(String text, int fromIndex) {
        Matcher matcher = MODERN_TOKEN.matcher(text);
        matcher.region(Math.max(0, fromIndex), text.length());
        while (matcher.find()) {
            if (isEscaped(text, matcher.start())) {
                continue;
            }
            InlinePatternData data = parse(matcher.group(1), matcher.group(3));
            if (data != null) {
                return new InlineMatch(matcher.start(), matcher.end(), data);
            }
        }
        return null;
    }

    private InlineMatch findExplicit(String text, int fromIndex) {
        Matcher matcher = EXPLICIT_TOKEN.matcher(text);
        matcher.region(Math.max(0, fromIndex), text.length());
        while (matcher.find()) {
            if (isEscaped(text, matcher.start())) {
                continue;
            }
            InlinePatternData data = parse(matcher.group(1), matcher.group(2));
            if (data != null) {
                return new InlineMatch(matcher.start(), matcher.end(), data);
            }
        }
        return null;
    }

    private static InlinePatternData parse(String directionText, String angles) {
        HexDir direction = findDirection(directionText);
        if (direction == null) {
            return null;
        }
        try {
            String angleText = angles == null ? "" : angles.toLowerCase(Locale.ROOT);
            return new InlinePatternData(HexPattern.fromAngles(angleText, direction));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static HexDir findDirection(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT)
            .replace("_", "")
            .replace("-", "");
        if ("nw".equals(normalized)) normalized = "northwest";
        if ("sw".equals(normalized)) normalized = "southwest";
        if ("se".equals(normalized)) normalized = "southeast";
        if ("ne".equals(normalized)) normalized = "northeast";
        if ("w".equals(normalized)) normalized = "west";
        if ("e".equals(normalized)) normalized = "east";
        for (HexDir direction : HexDir.values()) {
            String enumName = direction.name().toLowerCase(Locale.ROOT).replace("_", "");
            if (enumName.equals(normalized)) {
                return direction;
            }
        }
        return null;
    }

    private static boolean isEscaped(String text, int index) {
        return index > 0 && text.charAt(index - 1) == '\\'
            && (index < 2 || text.charAt(index - 2) != '\\');
    }
}

