package at.petra_k.hexcasting.common.block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyBool;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.casting.circles.ICircleComponent;
import at.petra_k.hexcasting.common.item.ItemHexStaff;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;

import net.minecraft.util.NonNullList;

/**
 * Impetus trigger for the first 1.12.2 circle slice.
 *
 * <p>The program is bound to the block entity with a sneaking staff use. This
 * keeps the trigger independent from whichever staff the player later holds,
 * while the stored VM state remains persistent with the impetus.</p>
 */
public class BlockImpetus extends BlockCircleComponent {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    public enum TriggerMode {
        EMPTY,
        LOOK,
        REDSTONE,
        RIGHT_CLICK
    }

    private final TriggerMode triggerMode;

    public BlockImpetus() {
        this(TriggerMode.RIGHT_CLICK);
    }

    public BlockImpetus(TriggerMode triggerMode) {
        super(Material.IRON);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.UP).withProperty(ENERGIZED, false));
        setHardness(3.0F);
        setResistance(10.0F);
        this.triggerMode = triggerMode == null ? TriggerMode.RIGHT_CLICK : triggerMode;
    }

    public TriggerMode getTriggerMode() {
        return triggerMode;
    }

    @Override
    public ICircleComponent.ControlFlow acceptControlFlow(
        at.petra_k.hexcasting.api.casting.eval.vm.CastingVM image,
        EnumFacing enterDirection, BlockPos pos, IBlockState state, World world) {
        if (triggerMode == TriggerMode.EMPTY) {
            return new ICircleComponent.Continue(image,
                java.util.Collections.singletonList(
                    exitPositionFromDirection(pos, state.getValue(FACING))));
        }
        // Real impetus blocks are the start/end marker of a circle. The
        // execution state recognizes the return to this block before calling
        // this method; reaching one as an ordinary component stops safely.
        return new ICircleComponent.Stop();
    }

    @Override
    public boolean canEnterFromDirection(EnumFacing enterDirection, BlockPos pos,
                                         IBlockState state, World world) {
        return enterDirection != state.getValue(FACING).getOpposite();
    }

    @Override
    public java.util.EnumSet<EnumFacing> possibleExitDirections(
        BlockPos pos, IBlockState state, World world) {
        return java.util.EnumSet.of(state.getValue(FACING));
    }

    @Override
    public EnumFacing normalDir(BlockPos pos, IBlockState state, World world) {
        return state.getValue(FACING);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityImpetus();
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
        if (tileEntity instanceof TileEntityImpetus) {
            NBTTagList program = ((TileEntityImpetus) tileEntity).getProgramSnapshot();
            if (program.tagCount() > 0) {
                NBTTagCompound blockEntityTag = new NBTTagCompound();
                blockEntityTag.setTag("patterns", program);
                NBTTagCompound tag = new NBTTagCompound();
                tag.setTag("BlockEntityTag", blockEntityTag);
                stack.setTagCompound(tag);
            }
        }
        return stack;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY,
                                    float hitZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntityImpetus)) {
            return false;
        }
        TileEntityImpetus impetus = (TileEntityImpetus) tileEntity;
        ItemStack staff = player.getHeldItem(hand);
        if (player.isSneaking() && ItemHexStaff.isStaff(staff)) {
            impetus.bindProgram(ItemHexStaff.getProgramSnapshot(staff));
            return true;
        }
        if (triggerMode == TriggerMode.RIGHT_CLICK || triggerMode == TriggerMode.LOOK) {
            impetus.trigger(player, hand);
        } else if (impetus.getProgramSize() == 0) {
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.program_empty"));
        }
        return true;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
                                Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, blockIn, fromPos);
        if (world.isRemote || triggerMode != TriggerMode.REDSTONE) {
            return;
        }
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntityImpetus)) {
            return;
        }
        TileEntityImpetus impetus = (TileEntityImpetus) tileEntity;
        boolean powered = world.isBlockPowered(pos);
        if (powered && !impetus.isPowered()) {
            EntityPlayer player = closestPlayer(world, pos);
            if (player != null) {
                impetus.trigger(player, EnumHand.MAIN_HAND);
            }
        }
        impetus.setPowered(powered);
    }

    private static EntityPlayer closestPlayer(World world, BlockPos pos) {
        EntityPlayer closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (EntityPlayer candidate : world.playerEntities) {
            double distance = candidate.getDistanceSqToCenter(pos);
            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        return closest;
    }
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ENERGIZED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        int facingIndex = meta & 7;
        EnumFacing facing = facingIndex < 6 ? EnumFacing.getFront(facingIndex) : EnumFacing.UP;
        return getDefaultState()
            .withProperty(FACING, facing)
            .withProperty(ENERGIZED, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex() | (state.getValue(ENERGIZED) ? 8 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, facing).withProperty(ENERGIZED, false);
    }

}
