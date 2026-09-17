package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.casting.circles.ICircleComponent;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import at.petra_k.hexcasting.common.item.ItemSlate;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * A thin, face-attached slate used by Hex's circle structures.
 *
 * <p>FACING is the outward normal of the slate. The block is only valid while
 * the opposite face has a solid support block, matching the modern slate's
 * floor, ceiling, and wall placement behavior.</p>
 */
public final class BlockSlate extends BlockCircleComponent {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    private static final double THICKNESS = 1.0D / 16.0D;

    public BlockSlate() {
        super(Material.ROCK);
        setHardness(1.5F);
        setResistance(6.0F);
        setDefaultState(blockState.getBaseState()
            .withProperty(FACING, EnumFacing.UP)
            .withProperty(ENERGIZED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ENERGIZED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING,
            EnumFacing.getFront(meta & 7))
            .withProperty(ENERGIZED, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex()
            | (state.getValue(ENERGIZED) ? 8 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, facing)
            .withProperty(ENERGIZED, false);
    }

    @Override
    public ICircleComponent.ControlFlow acceptControlFlow(CastingVM image,
        EnumFacing enterDirection, BlockPos pos, IBlockState state, World world) {
        EnumSet<EnumFacing> exits = possibleExitDirections(pos, state, world);
        exits.remove(enterDirection.getOpposite());
        List<ICircleComponent.Exit> output = new ArrayList<>(exits.size());
        for (EnumFacing direction : exits) {
            output.add(exitPositionFromDirection(pos, direction));
        }

        TileEntity tileEntity = world.getTileEntity(pos);
        HexPattern pattern = tileEntity instanceof TileEntitySlate
            ? ((TileEntitySlate) tileEntity).getPattern() : null;
        if (pattern == null) {
            return new ICircleComponent.Continue(image, output);
        }
        try {
            image.resetOperationCounter();
            image.enqueue(pattern);
            image.run(CastingVM.DEFAULT_MAX_OPERATIONS);
            return new ICircleComponent.Continue(image, output);
        } catch (Exception ignored) {
            return new ICircleComponent.Stop();
        }
    }

    @Override
    public boolean canEnterFromDirection(EnumFacing enterDirection, BlockPos pos,
                                         IBlockState state, World world) {
        return enterDirection != normalDir(pos, state, world).getOpposite();
    }

    @Override
    public EnumSet<EnumFacing> possibleExitDirections(BlockPos pos,
                                                       IBlockState state,
                                                       World world) {
        EnumSet<EnumFacing> exits = EnumSet.allOf(EnumFacing.class);
        exits.remove(normalDir(pos, state, world));
        return exits;
    }

    @Override
    public EnumFacing normalDir(BlockPos pos, IBlockState state, World world) {
        return state.getValue(FACING);
    }

    @Override
    public float particleHeight(BlockPos pos, IBlockState state, World world) {
        return 0.5F - 15.0F / 16.0F;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        for (EnumFacing facing : EnumFacing.values()) {
            if (hasSupport(world, pos, facing)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
                                Block blockIn, BlockPos fromPos) {
        if (!canStay(world, pos, state)) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
        super.neighborChanged(state, world, pos, blockIn, fromPos);
    }

    private static boolean canStay(World world, BlockPos pos, IBlockState state) {
        return hasSupport(world, pos, state.getValue(FACING));
    }

    private static boolean hasSupport(World world, BlockPos pos, EnumFacing normal) {
        BlockPos supportPos = pos.offset(normal.getOpposite());
        return world.getBlockState(supportPos).isSideSolid(world, supportPos, normal);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case DOWN:
                return new AxisAlignedBB(0.0D, 1.0D - THICKNESS, 0.0D, 1.0D, 1.0D, 1.0D);
            case NORTH:
                return new AxisAlignedBB(0.0D, 0.0D, 1.0D - THICKNESS, 1.0D, 1.0D, 1.0D);
            case SOUTH:
                return new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, THICKNESS);
            case WEST:
                return new AxisAlignedBB(1.0D - THICKNESS, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
            case EAST:
                return new AxisAlignedBB(0.0D, 0.0D, 0.0D, THICKNESS, 1.0D, 1.0D);
            case UP:
            default:
                return new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, THICKNESS, 1.0D);
        }
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntitySlate();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY,
                                    float hitZ) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntitySlate)) {
            return false;
        }
        TileEntitySlate slate = (TileEntitySlate) tileEntity;
        if (world.isRemote) {
            return true;
        }

        if (player.isSneaking()) {
            if (slate.getPattern() != null) {
                slate.setPattern(null);
                player.sendMessage(new TextComponentTranslation(
                    "hexcasting.message.slate_cleared"));
            } else {
                player.sendMessage(new TextComponentTranslation(
                    "hexcasting.message.slate_empty"));
            }
            return true;
        }

        EnumHand scrollHand = null;
        ItemStack held = player.getHeldItem(hand);
        if (isPatternScroll(held)) {
            scrollHand = hand;
        } else if (isPatternScroll(player.getHeldItemOffhand())) {
            scrollHand = EnumHand.OFF_HAND;
        }
        if (scrollHand != null) {
            ItemStack scroll = player.getHeldItem(scrollHand);
            HexPattern pattern = ItemPatternScroll.getPattern(scroll);
            if (pattern != null) {
                slate.setPattern(pattern);
                ItemPatternScroll.consumeForWrite(scroll, player);
                player.sendMessage(new TextComponentTranslation(
                    "hexcasting.message.slate_written", pattern.signature()));
            } else {
                player.sendMessage(new TextComponentTranslation(
                    "hexcasting.message.slate_invalid_scroll"));
            }
            return true;
        }

        HexPattern pattern = slate.getPattern();
        if (pattern == null) {
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.slate_empty"));
        } else {
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.slate_pattern", pattern.signature()));
        }
        return true;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return createItemStack(world, pos);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world,
                         BlockPos pos, IBlockState state, int fortune) {
        ItemStack stack = createItemStack(world, pos);
        if (!stack.isEmpty()) {
            drops.add(stack);
        }
    }

    private ItemStack createItemStack(IBlockAccess world, BlockPos pos) {
        Item item = Item.getItemFromBlock(this);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntitySlate) {
            ItemSlate.writePattern(stack, ((TileEntitySlate) tileEntity).getPattern());
        }
        return stack;
    }

    private static boolean isPatternScroll(ItemStack stack) {
        return stack != null && !stack.isEmpty()
            && stack.getItem() instanceof ItemPatternScroll;
    }
}
