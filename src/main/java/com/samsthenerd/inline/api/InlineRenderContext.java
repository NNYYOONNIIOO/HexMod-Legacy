package com.samsthenerd.inline.api;

import net.minecraft.entity.player.EntityPlayer;

/** Runtime context passed to an Inline renderer. */
public final class InlineRenderContext {
    private final EntityPlayer viewer;
    private final boolean client;

    public InlineRenderContext(EntityPlayer viewer, boolean client) {
        this.viewer = viewer;
        this.client = client;
    }

    public EntityPlayer getViewer() {
        return viewer;
    }

    public boolean isClient() {
        return client;
    }
}
