package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.item.IotaHolderItem;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import at.petra_k.hexcasting.common.world.AkashicRecordData;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.util.BlockRenderLayer;

/** Wooden storage anchor for the Akashic record system. */
public final class BlockAkashicRecord extends Block {
    private static final int MAX_NETWORK_BLOCKS = 128;

    public BlockAkashicRecord() {
        super(Material.WOOD);
        setHardness(2.0F);
        setResistance(5.0F);
    }

    /** Look up a key in connected bookshelves, with the old world-data bridge as fallback. */
    public NBTTagCompound lookupPattern(World world, BlockPos pos, HexPattern key) {
        if (world == null || pos == null || key == null
            || !(world.getBlockState(pos).getBlock() instanceof BlockAkashicRecord)) {
            return null;
        }
        for (TileEntityAkashicBookshelf shelf : connectedBookshelves(world, pos)) {
            HexPattern storedPattern = shelf.getPattern();
            if (storedPattern != null && storedPattern.signature().equals(key.signature())
                && shelf.getIotaTag() != null) {
                return shelf.getIotaTag();
            }
        }
        return AkashicRecordData.get(world).read(pos, key.signature());
    }

    /** Add a new key without clobbering an existing mapping. */
    public boolean addNewDatum(World world, BlockPos pos, HexPattern key, Iota datum) {
        if (world == null || pos == null || key == null || datum == null
            || !(world.getBlockState(pos).getBlock() instanceof BlockAkashicRecord)
            || lookupPattern(world, pos, key) != null) {
            return false;
        }
        for (TileEntityAkashicBookshelf shelf : connectedBookshelves(world, pos)) {
            if (!shelf.hasMapping()) {
                shelf.setMapping(key, datum);
                return true;
            }
        }
        // Keep worlds created by the earlier bridge usable even when no
        // bookshelf network has been built yet.
        AkashicRecordData.get(world).write(pos, key.signature(), datum.serialize());
        return true;
    }

    /** Clear both connected bookshelf mappings and the legacy per-record data. */
    public void clearMappings(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return;
        }
        for (TileEntityAkashicBookshelf shelf : connectedBookshelves(world, pos)) {
            shelf.clearMapping();
        }
        AkashicRecordData.get(world).clearAt(pos);
    }

    private static List<TileEntityAkashicBookshelf> connectedBookshelves(
            World world, BlockPos origin) {
        List<TileEntityAkashicBookshelf> result = new ArrayList<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(origin);
        while (!pending.isEmpty() && visited.size() < MAX_NETWORK_BLOCKS) {
            BlockPos current = pending.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            Block block = world.getBlockState(current).getBlock();
            boolean traversable = current.equals(origin)
                || block instanceof BlockAkashicBookshelf || isConnector(block);
            if (!traversable) {
                continue;
            }
            if (block instanceof BlockAkashicBookshelf) {
                TileEntity tileEntity = world.getTileEntity(current);
                if (tileEntity instanceof TileEntityAkashicBookshelf) {
                    result.add((TileEntityAkashicBookshelf) tileEntity);
                }
            }
            for (EnumFacing facing : EnumFacing.values()) {
                pending.addLast(current.offset(facing));
            }
        }
        return result;
    }

    private static boolean isConnector(Block block) {
        if (block instanceof BlockAkashicConnector) {
            return true;
        }
        net.minecraft.util.ResourceLocation id = block.getRegistryName();
        return id != null && "hexcasting".equals(id.getResourceDomain())
            && "akashic_connector".equals(id.getResourcePath());
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntityAkashicRecord)) {
            return false;
        }
        TileEntityAkashicRecord record = (TileEntityAkashicRecord) tileEntity;
        BlockAkashicRecord recordBlock = this;
        ItemStack held = player.getHeldItem(hand);
        if (player.isSneaking() && held.isEmpty()) {
            record.clearAll();
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.akashic_record_cleared"));
            return true;
        }

        EnumHand scrollHand = findScrollHand(player, hand);
        if (scrollHand == null) {
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.akashic_record_requires_scroll"));
            return true;
        }
        ItemStack scroll = player.getHeldItem(scrollHand);
        HexPattern key = ItemPatternScroll.getPattern(scroll);
        if (key == null) {
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.akashic_record_no_pattern"));
            return true;
        }

        EnumHand valueHand = scrollHand == EnumHand.MAIN_HAND
            ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        ItemStack valueStack = player.getHeldItem(valueHand);
        if (valueStack.getItem() instanceof IotaHolderItem) {
            IotaHolderItem holder = (IotaHolderItem) valueStack.getItem();
            try {
                Iota heldIota = holder.readIota(valueStack);
                if (heldIota != null) {
                    if (recordBlock.addNewDatum(world, pos, key, heldIota)) {
                        player.sendMessage(new TextComponentTranslation(
                            "hexcasting.message.akashic_record_written", key.signature()));
                    } else {
                        player.sendMessage(new TextComponentTranslation(
                            "hexcasting.error.akashic_duplicate"));
                    }
                    return true;
                }

                NBTTagCompound storedTag = recordBlock.lookupPattern(world, pos, key);
                if (storedTag != null) {
                    Iota storedIota = HexIotaTypes.deserialize(storedTag);
                    if (holder.canWrite(valueStack, storedIota)) {
                        holder.writeDatum(valueStack, storedIota);
                        player.sendMessage(new TextComponentTranslation(
                            "hexcasting.message.akashic_record_read", key.signature()));
                        return true;
                    }
                }
            } catch (CastingException | RuntimeException ignored) {
                // Do not mutate the container when the Iota cannot be decoded.
            }
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.akashic_record_no_value"));
            return true;
        }

        if (recordBlock.lookupPattern(world, pos, key) != null) {
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.akashic_record_exists", key.signature()));
        } else {
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.akashic_record_requires_iota"));
        }
        return true;
    }

    private static EnumHand findScrollHand(EntityPlayer player, EnumHand preferred) {
        if (player.getHeldItem(preferred).getItem() instanceof ItemPatternScroll) {
            return preferred;
        }
        EnumHand other = preferred == EnumHand.MAIN_HAND
            ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        return player.getHeldItem(other).getItem() instanceof ItemPatternScroll
            ? other : null;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityAkashicRecord();
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

}
