package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.List;

/** Wooden storage block for the Akashic record system. */
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
        player.sendMessage(new TextComponentTranslation(
            "hexcasting.message.akashic_record_status", record.getEntryCount()));
        return true;
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
