package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.block.BlockAkashicBookshelf;
import at.petra_k.hexcasting.common.block.TileEntityAkashicBookshelf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Renders the pattern key stored on an Akashic bookshelf. */
@SideOnly(Side.CLIENT)
public final class HexAkashicBookshelfRenderer
    extends TileEntitySpecialRenderer<TileEntityAkashicBookshelf> {
    @Override
    public void render(TileEntityAkashicBookshelf tile, double x, double y,
                       double z, float partialTicks, int destroyStage,
                       float alpha) {
        if (tile == null || tile.getWorld() == null || tile.getPos() == null) {
            return;
        }
        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        if (!(state.getBlock() instanceof BlockAkashicBookshelf)) {
            return;
        }
        EnumFacing facing = state.getValue(BlockAkashicBookshelf.FACING);
        int packedLight = tile.getWorld().getCombinedLight(
            tile.getPos().offset(facing), 0);
        HexWorldPatternRenderer.render(tile.getPattern(), facing, false,
            0.0F, 0.0F, tile.getPos().hashCode(), packedLight);
    }
}
