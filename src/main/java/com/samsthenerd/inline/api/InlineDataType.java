package com.samsthenerd.inline.api;

import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

/** Stable type registration used by the 1.12.2 Inline port. */
public interface InlineDataType<D extends InlineData<D>> {
    ResourceLocation getId();

    default JsonObject serialize(D data) {
        return new JsonObject();
    }

    default D deserialize(JsonObject json) {
        throw new UnsupportedOperationException("This Inline data type is not deserializable");
    }
}
