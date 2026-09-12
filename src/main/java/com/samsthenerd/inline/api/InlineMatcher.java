package com.samsthenerd.inline.api;

/** Finds one Inline payload in a message beginning at a supplied offset. */
public interface InlineMatcher {
    InlineMatch find(String text, int fromIndex, MatchContext context);
}
