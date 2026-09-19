package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.block.TileEntityQuenchedAllay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Renders the invisible Quenched Allay block using its item model. */
@SideOnly(Side.CLIENT)
public final class HexQuenchedAllayRenderer
    extends TileEntitySpecialRenderer<TileEntityQuenchedAllay> {

    @Override
    public void render(TileEntityQuenchedAllay tile, double x, double y,
                       double z, float partialTicks, int destroyStage,
                       float alpha) {
        if (tile == null || tile.getWorld() == null
            || tile.getBlockType() == null
            || tile.getBlockType().getRegistryName() == null) {
            return;
        }

        Item item = Item.getItemFromBlock(tile.getBlockType());
        if (item == null) {
            return;
        }

        // The inventory model is the authoritative resource path for these
        // blocks: it already contains the four gaslighting overrides and is
        // known to be baked by 1.12.2.  Resolving the override here avoids
        // depending on the ModelResourceLocation spelling used internally by
        // ModelBakery for models that are only referenced from an item JSON.
        ItemStack stack = new ItemStack(item);
        RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
        IBakedModel model = renderItem.getItemModelWithOverrides(
            stack, tile.getWorld(), null);
        if (model == null) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha <= 0.0F ? 1.0F : alpha);
        Minecraft.getMinecraft().getTextureManager().bindTexture(
            TextureMap.LOCATION_BLOCKS_TEXTURE);

        renderItem.renderItem(stack, model);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
