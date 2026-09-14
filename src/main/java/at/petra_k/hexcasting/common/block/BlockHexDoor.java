package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.common.lib.HexBlocks;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import java.util.Random;

/** Door using the vanilla 1.12.2 BlockDoor item, pick-block, and drop behavior. */
public final class BlockHexDoor extends BlockDoor {
    public BlockHexDoor() {
        super(Material.WOOD);
        setHardness(3.0F);
        setResistance(3.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
    }

    /** Preserve vanilla door behavior while returning this block's registered item. */
    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        Item item = getDoorItem();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target,
                                  World world, BlockPos pos, EntityPlayer player) {
        return getItem(world, pos, state);
    }

    /** Vanilla drops only from the lower half; the item is this door, not oak. */
    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        if (state.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.UPPER) {
            return Items.AIR;
        }
        Item item = getDoorItem();
        return item == null ? Items.AIR : item;
    }

    private Item getDoorItem() {
        return HexBlocks.getBlockItem("edified_door");
    }
}
