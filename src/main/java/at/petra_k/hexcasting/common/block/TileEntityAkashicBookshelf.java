package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/** Stores one pattern key and one arbitrary Iota in an Akashic bookshelf. */
public final class TileEntityAkashicBookshelf extends TileEntity {
    public static final String TAG_PATTERN = "pattern";
    public static final String TAG_IOTA = "iota";

    private HexPattern pattern;
    private NBTTagCompound iotaTag;

    public HexPattern getPattern() {
        return pattern;
    }

    public NBTTagCompound getIotaTag() {
        return iotaTag == null ? null : iotaTag.copy();
    }

    public Iota getIota() {
        if (iotaTag == null) {
            return null;
        }
        try {
            return HexIotaTypes.deserialize(iotaTag);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public boolean hasMapping() {
        return pattern != null && iotaTag != null;
    }

    public void setMapping(HexPattern pattern, Iota iota) {
        if (pattern == null || iota == null) {
            clearMapping();
            return;
        }
        this.pattern = pattern;
        this.iotaTag = iota.serialize();
        syncState(true);
    }

    public void clearMapping() {
        this.pattern = null;
        this.iotaTag = null;
        syncState(false);
    }

    private void syncState(boolean hasBooks) {
        markDirty();
        if (world != null) {
            IBlockState oldState = world.getBlockState(pos);
            IBlockState newState = oldState;
            if (oldState.getBlock() instanceof BlockAkashicBookshelf) {
                newState = oldState.withProperty(BlockAkashicBookshelf.HAS_BOOKS, hasBooks);
            }
            if (newState != oldState) {
                world.setBlockState(pos, newState, 3);
            }
            world.notifyBlockUpdate(pos, oldState, newState, 3);
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (pattern != null && iotaTag != null) {
            compound.setTag(TAG_PATTERN, pattern.serializeToNBT());
            compound.setTag(TAG_IOTA, iotaTag.copy());
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        pattern = null;
        iotaTag = null;
        if (compound.hasKey(TAG_PATTERN, 10) && compound.hasKey(TAG_IOTA, 10)) {
            try {
                NBTTagCompound patternTag = compound.getCompoundTag(TAG_PATTERN);
                pattern = HexPattern.fromNBT(patternTag);
                iotaTag = compound.getCompoundTag(TAG_IOTA).copy();
            } catch (RuntimeException ignored) {
                pattern = null;
                iotaTag = null;
            }
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }
}
