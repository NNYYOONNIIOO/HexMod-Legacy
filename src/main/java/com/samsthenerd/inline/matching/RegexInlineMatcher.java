package com.samsthenerd.inline.matching;

import com.samsthenerd.inline.api.InlineData;
import com.samsthenerd.inline.api.InlineMatch;
import com.samsthenerd.inline.api.InlineMatcher;
import com.samsthenerd.inline.api.MatchContext;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Regex-backed matcher equivalent to Inline's common matcher primitive. */
public final class RegexInlineMatcher implements InlineMatcher {
    private final Pattern pattern;
    private final BiFunction<Matcher, MatchContext, InlineData<?>> factory;

    public RegexInlineMatcher(Pattern pattern,
                              BiFunction<Matcher, MatchContext, InlineData<?>> factory) {
        this.pattern = Objects.requireNonNull(pattern, "pattern");
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    @Override
    public InlineMatch find(String text, int fromIndex, MatchContext context) {
        Matcher matcher = pattern.matcher(text);
        matcher.region(Math.max(0, fromIndex), text.length());
        if (!matcher.find()) {
            return null;
        }
        InlineData<?> data = factory.apply(matcher, context);
        return data == null ? null : new InlineMatch(matcher.start(), matcher.end(), data);
    }
}
