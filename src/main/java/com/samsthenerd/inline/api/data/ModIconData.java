package com.samsthenerd.inline.api.data;

import com.samsthenerd.inline.api.InlineData;
import com.samsthenerd.inline.api.InlineDataType;
import net.minecraft.util.ResourceLocation;

/** Inline data for a Forge mod icon. */
public final class ModIconData implements InlineData<ModIconData> {
    public static final ResourceLocation RENDERER_ID =
        new ResourceLocation("inline", "modicon");
    public static final InlineDataType<ModIconData> TYPE =
        () -> new ResourceLocation("inline", "modicon");

    private final String modId;

    public ModIconData(String modId) {
        this.modId = modId;
    }

    public String getModId() {
        return modId;
    }

    @Override
    public InlineDataType<ModIconData> getType() {
        return TYPE;
    }

    @Override
    public ResourceLocation getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public ModIconData copy() {
        return new ModIconData(modId);
    }
}
