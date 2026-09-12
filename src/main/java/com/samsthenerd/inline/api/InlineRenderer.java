package com.samsthenerd.inline.api;

/** Converts Inline data to the 1.12.2 text fallback used by chat and tooltips. */
public interface InlineRenderer<D extends InlineData<D>> {
    String render(D data, InlineRenderContext context);
}
