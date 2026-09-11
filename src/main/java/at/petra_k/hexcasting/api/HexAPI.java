package at.petra_k.hexcasting.api;

import net.minecraft.util.ResourceLocation;

/** Shared public constants for the 1.12.2 Hex Casting port. */
public final class HexAPI {
    public static final String MOD_ID = "hexcasting";
    public static final String MOD_NAME = "Hex Casting";
    public static final String MOD_VERSION = "0.1.0-1.12.2";

    private HexAPI() {
    }

    public static ResourceLocation modLoc(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
