package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.CommonProxy;
import at.petra_k.hexcasting.common.block.TileEntityQuenchedAllay;
import at.petra_k.hexcasting.common.entity.EntityWallScroll;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** Client-side renderer registration for the 1.12.2 port. */
public final class HexClientProxy extends CommonProxy {
    @Override
    public void registerRenderers() {
        ClientRegistry.bindTileEntitySpecialRenderer(
            TileEntityQuenchedAllay.class, new HexQuenchedAllayRenderer());
        RenderingRegistry.registerEntityRenderingHandler(
            EntityWallScroll.class, RenderWallScroll::new);
    }
}
