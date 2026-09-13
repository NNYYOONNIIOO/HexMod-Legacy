package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockDoor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Door with vanilla two-block placement and open/hinge/power behavior. */
public final class BlockHexDoor extends BlockDoor {
    public BlockHexDoor() {
        super(Material.WOOD);
        setHardness(3.0F);
        setResistance(3.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
    }
    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(Item.getItemFromBlock(this));
    }
    @Override
    public ItemStack getPickBlock(IBlockState state, net.minecraft.util.math.RayTraceResult target,
                                  World world, BlockPos pos,
                                  net.minecraft.entity.player.EntityPlayer player) {
        return new ItemStack(Item.getItemFromBlock(this));
    }

}
