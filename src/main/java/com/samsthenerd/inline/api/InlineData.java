package com.samsthenerd.inline.api;

import net.minecraft.util.ResourceLocation;

/** Data attached to an Inline placeholder. */
public interface InlineData<D extends InlineData<D>> {
    InlineDataType<D> getType();

    ResourceLocation getRendererId();

    D copy();
}
