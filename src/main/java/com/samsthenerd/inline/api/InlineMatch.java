package com.samsthenerd.inline.api;

import java.util.Objects;

/** A single matcher result in a chat message. */
public final class InlineMatch {
    private final int start;
    private final int end;
    private final InlineData<?> data;

    public InlineMatch(int start, int end, InlineData<?> data) {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid Inline match range");
        }
        this.start = start;
        this.end = end;
        this.data = Objects.requireNonNull(data, "data");
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public InlineData<?> getData() {
        return data;
    }
}
