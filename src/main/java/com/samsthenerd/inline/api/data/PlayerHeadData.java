package com.samsthenerd.inline.api.data;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.samsthenerd.inline.api.InlineData;
import com.samsthenerd.inline.api.InlineDataType;
import net.minecraft.util.ResourceLocation;
import java.util.UUID;

/** Inline data for a player profile. */
public final class PlayerHeadData implements InlineData<PlayerHeadData> {
    public static final ResourceLocation RENDERER_ID =
        new ResourceLocation("inline", "playerhead");
    public static final InlineDataType<PlayerHeadData> TYPE =
        new InlineDataType<PlayerHeadData>() {
            @Override
            public ResourceLocation getId() {
                return new ResourceLocation("inline", "playerhead");
            }

            @Override
            public JsonObject serialize(PlayerHeadData data) {
                JsonObject result = new JsonObject();
                if (data.getProfile() != null && data.getProfile().getId() != null) {
                    result.addProperty("uuid", data.getProfile().getId().toString());
                }
                if (data.getProfile() != null && data.getProfile().getName() != null) {
                    result.addProperty("name", data.getProfile().getName());
                }
                return result;
            }

            @Override
            public PlayerHeadData deserialize(JsonObject data) {
                String name = data.has("name") ? data.get("name").getAsString() : null;
                UUID id = data.has("uuid")
                    ? UUID.fromString(data.get("uuid").getAsString())
                    : (name == null ? null : UUID.nameUUIDFromBytes(
                        name.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                return new PlayerHeadData(new GameProfile(id, name));
            }
        };

    private final GameProfile profile;

    public PlayerHeadData(GameProfile profile) {
        this.profile = profile;
    }

    public PlayerHeadData(String name) {
        this(new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name));
    }

    public GameProfile getProfile() {
        return profile;
    }

    @Override
    public InlineDataType<PlayerHeadData> getType() {
        return TYPE;
    }

    @Override
    public ResourceLocation getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public PlayerHeadData copy() {
        return new PlayerHeadData(profile);
    }
}
