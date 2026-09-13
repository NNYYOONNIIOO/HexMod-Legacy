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
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/** Wooden storage anchor for the Akashic record system. */
public final class BlockAkashicRecord extends Block {
    public BlockAkashicRecord() {
        super(Material.WOOD);
        setHardness(2.0F);
        setResistance(5.0F);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    net.minecraft.util.EnumFacing side,
                                    float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntityAkashicRecord)) {
            return false;
        }
        TileEntityAkashicRecord record = (TileEntityAkashicRecord) tileEntity;
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
                    AkashicRecordData.get(world).write(pos, key.signature(),
                        heldIota.serialize());
                    player.sendMessage(new TextComponentTranslation(
                        "hexcasting.message.akashic_record_written", key.signature()));
                    return true;
                }

                NBTTagCompound storedTag = AkashicRecordData.get(world)
                    .read(pos, key.signature());
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
                // Do not mutate the container when the stored Iota is invalid.
            }
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.akashic_record_no_value"));
            return true;
        }

        if (AkashicRecordData.get(world).read(pos, key.signature()) != null) {
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
}
