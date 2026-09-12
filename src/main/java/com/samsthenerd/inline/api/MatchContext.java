package com.samsthenerd.inline.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/** Context available to server and client chat matchers. */
public final class MatchContext {
    private final EntityPlayer viewer;
    private final World world;

    public MatchContext(EntityPlayer viewer, World world) {
        this.viewer = viewer;
        this.world = world;
    }

    public EntityPlayer getViewer() {
        return viewer;
    }

    public World getWorld() {
        return world;
    }
}
