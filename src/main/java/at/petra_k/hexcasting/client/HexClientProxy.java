package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.CommonProxy;
import at.petra_k.hexcasting.common.config.HexConfig;
import at.petra_k.hexcasting.common.entity.EntityWallScroll;
import at.petra_k.hexcasting.common.block.TileEntityAkashicBookshelf;
import at.petra_k.hexcasting.common.block.TileEntitySlate;
import at.petra_k.hexcasting.common.block.TileEntityQuenchedAllay;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraft.client.Minecraft;

/** Client-side renderer registration for the 1.12.2 port. */
public final class HexClientProxy extends CommonProxy {
    public static String currentLanguage() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft == null || minecraft.gameSettings == null
            ? "" : minecraft.gameSettings.language;
    }

    @Override
    public void registerRenderers() {
        HexConfig.applyClientLanguageDefault(currentLanguage());
        HexShaders.register();
        RenderingRegistry.registerEntityRenderingHandler(
            EntityWallScroll.class, RenderWallScroll::new);
        ClientRegistry.bindTileEntitySpecialRenderer(
            TileEntityQuenchedAllay.class, new HexQuenchedAllayRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(
            TileEntitySlate.class, new HexSlateRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(
            TileEntityAkashicBookshelf.class, new HexAkashicBookshelfRenderer());
    }
}
