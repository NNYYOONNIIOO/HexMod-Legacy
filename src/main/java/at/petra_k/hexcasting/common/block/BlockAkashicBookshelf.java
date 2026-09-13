package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.item.IotaHolderItem;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** A single Akashic mapping cell: pattern key to arbitrary stored Iota. */
public final class BlockAkashicBookshelf extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    public static final PropertyBool HAS_BOOKS = PropertyBool.create("has_books");

    public BlockAkashicBookshelf() {
        super(Material.WOOD);
        setHardness(2.0F);
        setResistance(5.0F);
        setDefaultState(blockState.getBaseState()
            .withProperty(FACING, EnumFacing.NORTH)
            .withProperty(HAS_BOOKS, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, HAS_BOOKS);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
            .withProperty(FACING, EnumFacing.getHorizontal(meta & 3).getOpposite())
            .withProperty(HAS_BOOKS, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex()
            | (state.getValue(HAS_BOOKS) ? 4 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        EnumFacing horizontal = placer == null ? EnumFacing.NORTH
            : placer.getHorizontalFacing().getOpposite();
        return getDefaultState().withProperty(FACING, horizontal);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityAkashicBookshelf();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY,
                                    float hitZ) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntityAkashicBookshelf)) {
            return false;
        }
        TileEntityAkashicBookshelf shelf = (TileEntityAkashicBookshelf) tileEntity;
        if (world.isRemote) {
            return true;
        }

        ItemStack held = player.getHeldItem(hand);
        if (shelf.hasMapping() && held.getItem() instanceof IotaHolderItem) {
            IotaHolderItem holder = (IotaHolderItem) held.getItem();
            Iota stored = shelf.getIota();
            if (stored != null) {
                try {
                    if (holder.canWrite(held, stored)) {
                        holder.writeDatum(held, stored);
                        player.sendMessage(new TextComponentTranslation(
                            "hexcasting.message.akashic_shelf_read",
                            shelf.getPattern().signature()));
                        return true;
                    }
                } catch (RuntimeException ignored) {
                    // Leave the container unchanged when it cannot receive this Iota.
                }
            }
        }

        EnumHand otherHand = hand == EnumHand.MAIN_HAND
            ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        ItemStack other = player.getHeldItem(otherHand);
        if (!shelf.hasMapping() && held.getItem() instanceof ItemPatternScroll
            && other.getItem() instanceof IotaHolderItem) {
            HexPattern pattern = ItemPatternScroll.getPattern(held);
            IotaHolderItem holder = (IotaHolderItem) other.getItem();
            try {
                Iota datum = holder.readIota(other);
                if (pattern != null && datum != null) {
                    shelf.setMapping(pattern, datum);
                    player.sendMessage(new TextComponentTranslation(
                        "hexcasting.message.akashic_shelf_written", pattern.signature()));
                    return true;
                }
            } catch (CastingException | RuntimeException ignored) {
                // Do not consume either item when the Iota is malformed.
            }
        }

        if (held.getItem() instanceof ItemPatternScroll && shelf.getPattern() != null) {
            ItemPatternScroll.setPattern(held, shelf.getPattern());
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.akashic_scroll_written", shelf.getPattern().signature()));
            return true;
        }
        if (player.isSneaking() && held.isEmpty()) {
            shelf.clearMapping();
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.akashic_cleared"));
            return true;
        }
        if (shelf.hasMapping()) {
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.akashic_mapping", shelf.getPattern().signature(),
                shelf.getIota() == null ? "?" : shelf.getIota().display()));
        } else {
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.akashic_empty"));
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
        if (tileEntity instanceof TileEntityAkashicBookshelf) {
            TileEntityAkashicBookshelf shelf = (TileEntityAkashicBookshelf) tileEntity;
            if (shelf.hasMapping()) {
                NBTTagCompound blockEntityTag = new NBTTagCompound();
                NBTTagCompound stored = shelf.writeToNBT(new NBTTagCompound());
                stored.removeTag("x");
                stored.removeTag("y");
                stored.removeTag("z");
                blockEntityTag.merge(stored);
                NBTTagCompound tag = new NBTTagCompound();
                tag.setTag("BlockEntityTag", blockEntityTag);
                stack.setTagCompound(tag);
            }
        }
        return stack;
    }
}
