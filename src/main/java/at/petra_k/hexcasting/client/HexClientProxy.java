package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.CommonProxy;
import at.petra_k.hexcasting.common.block.TileEntityQuenchedAllay;
import net.minecraftforge.fml.client.registry.ClientRegistry;

/** Client-side renderer registration for the 1.12.2 port. */
public final class HexClientProxy extends CommonProxy {
    @Override
    public void registerRenderers() {
        ClientRegistry.bindTileEntitySpecialRenderer(
            TileEntityQuenchedAllay.class, new HexQuenchedAllayRenderer());
    }
}
