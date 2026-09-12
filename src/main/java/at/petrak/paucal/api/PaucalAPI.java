package at.petrak.paucal.api;

import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/** Embedded Paucal API entry point for the Hex Casting 1.12.2 port. */
public final class PaucalAPI {
    public static final String MOD_ID = "paucal";

    private PaucalAPI() {
    }

    public static void init() {
        PaucalNetwork.init();
    }

    public static SimpleNetworkWrapper network() {
        return PaucalNetwork.channel();
    }
}
