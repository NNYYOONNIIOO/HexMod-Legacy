package at.petra_k.hexcasting.common;

/** Common-side proxy; client-only render registration lives in the client proxy. */
public class CommonProxy {
    public void registerRenderers() {
        // No client classes may be loaded on a dedicated server.
    }
}
