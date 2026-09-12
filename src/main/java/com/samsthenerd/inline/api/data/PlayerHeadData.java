package com.samsthenerd.inline.api.data;

import com.mojang.authlib.GameProfile;
import com.samsthenerd.inline.api.InlineData;
import com.samsthenerd.inline.api.InlineDataType;
import net.minecraft.util.ResourceLocation;

/** Inline data for a player profile. */
public final class PlayerHeadData implements InlineData<PlayerHeadData> {
    public static final ResourceLocation RENDERER_ID =
        new ResourceLocation("inline", "playerhead");
    public static final InlineDataType<PlayerHeadData> TYPE =
        () -> new ResourceLocation("inline", "playerhead");

    private final GameProfile profile;

    public PlayerHeadData(GameProfile profile) {
        this.profile = profile;
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
