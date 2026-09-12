package com.samsthenerd.inline.api.data;

import com.google.gson.JsonObject;
import com.samsthenerd.inline.api.InlineData;
import com.samsthenerd.inline.api.InlineDataType;
import net.minecraft.util.ResourceLocation;

/** Inline data for a Forge mod icon. */
public final class ModIconData implements InlineData<ModIconData> {
    public static final ResourceLocation RENDERER_ID =
        new ResourceLocation("inline", "modicon");
    public static final InlineDataType<ModIconData> TYPE =
        new InlineDataType<ModIconData>() {
            @Override
            public ResourceLocation getId() {
                return new ResourceLocation("inline", "modicon");
            }

            @Override
            public JsonObject serialize(ModIconData data) {
                JsonObject result = new JsonObject();
                result.addProperty("mod_id", data.getModId());
                return result;
            }

            @Override
            public ModIconData deserialize(JsonObject data) {
                if (!data.has("mod_id")) {
                    throw new IllegalArgumentException("Mod icon Inline payload has no mod_id");
                }
                return new ModIconData(data.get("mod_id").getAsString());
            }
        };

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
