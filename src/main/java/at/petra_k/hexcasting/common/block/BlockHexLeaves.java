package at.petra_k.hexcasting.common.block;

import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.common.IShearable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Hex's Akashic foliage, backed by the vanilla 1.12.2 leaves implementation.
 *
 * <p>The modern block has the same important distinction as vanilla leaves:
 * leaves produced by a tree are decayable, while leaves placed from the item
 * form are persistent.  Using {@link BlockLeaves} here also gives us the
 * vanilla log-distance decay scan and makes shears/silk touch the only ways
 * to obtain the leaf item, matching Hex's 1.20.1 loot tables.</p>
 */
public final class BlockHexLeaves extends BlockLeaves {
    public BlockHexLeaves() {
        super();
        setHardness(0.2F);
        setResistance(1.0F);
        setSoundType(SoundType.PLANT);
        setLightOpacity(1);
        setDefaultState(blockState.getBaseState()
            .withProperty(DECAYABLE, true)
            .withProperty(CHECK_DECAY, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, DECAYABLE, CHECK_DECAY);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
            .withProperty(DECAYABLE, (meta & 4) == 0)
            .withProperty(CHECK_DECAY, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = 0;
        if (!state.getValue(DECAYABLE)) {
            meta |= 4;
        }
        if (state.getValue(CHECK_DECAY)) {
            meta |= 8;
        }
        return meta;
    }

    /** A sapling item is not part of Hex's Akashic tree; leaves decay empty. */
    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Items.AIR;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    /** Shears and Silk Touch both give the exact leaf block item. */
    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        Item item = Item.getItemFromBlock(this);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos,
                              IBlockState state, TileEntity tileEntity,
                              ItemStack heldItem) {
        if (!world.isRemote && heldItem != null
            && heldItem.getItem() == Items.SHEARS) {
            player.addStat(StatList.getBlockStats(this));
            Item item = Item.getItemFromBlock(this);
            if (item != null) {
                spawnAsEntity(world, pos, new ItemStack(item));
            }
        } else {
            super.harvestBlock(world, player, pos, state, tileEntity, heldItem);
        }
    }

    /** Item placement creates persistent leaves; tree generation uses defaultState(). */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        return getDefaultState()
            .withProperty(DECAYABLE, false)
            .withProperty(CHECK_DECAY, false);
    }

    /** The wood type is only used by vanilla bookkeeping and rendering hooks. */
    @Override
    public BlockPlanks.EnumType getWoodType(int meta) {
        return BlockPlanks.EnumType.OAK;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    public boolean isLeaves(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockAccess world,
                                     BlockPos pos, int fortune) {
        Item leafItem = Item.getItemFromBlock(this);
        return leafItem == null ? Collections.<ItemStack>emptyList()
            : Collections.singletonList(new ItemStack(leafItem));
    }

    @Override
    public boolean isFlammable(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return true;
    }

    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 30;
    }

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 60;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }
}
