package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.block.BlockCircleComponent;
import at.petra_k.hexcasting.common.block.BlockSlate;
import at.petra_k.hexcasting.common.block.TileEntitySlate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Renders the pattern stored on a world slate. */
@SideOnly(Side.CLIENT)
public final class HexSlateRenderer extends TileEntitySpecialRenderer<TileEntitySlate> {
    @Override
    public void render(TileEntitySlate tile, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        if (tile == null || tile.getWorld() == null || tile.getPos() == null) {
            return;
        }
        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        if (!(state.getBlock() instanceof BlockSlate)) {
            return;
        }
        EnumFacing facing = state.getValue(BlockSlate.FACING);
        boolean energized = state.getValue(BlockCircleComponent.ENERGIZED);
        long worldTime = tile.getWorld().getTotalWorldTime();
        HexWorldPatternRenderer.render(tile.getPattern(), facing, energized,
            partialTicks, worldTime, tile.getPos().hashCode());
    }
}
