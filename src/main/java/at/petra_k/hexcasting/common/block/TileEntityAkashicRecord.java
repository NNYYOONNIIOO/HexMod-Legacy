package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.world.AkashicRecordData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/** Persistent record anchor; its actual mappings live in AkashicRecordData. */
public final class TileEntityAkashicRecord extends TileEntity {
    public int getEntryCount() {
        return AkashicRecordData.get(world).countAt(pos);
    }

    public void clearAll() {
        if (world != null && world.getBlockState(pos).getBlock() instanceof BlockAkashicRecord) {
            ((BlockAkashicRecord) world.getBlockState(pos).getBlock())
                .clearMappings(world, pos);
        }
        markDirty();
    }
}
