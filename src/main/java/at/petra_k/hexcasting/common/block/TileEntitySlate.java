package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/** Stores the pattern written on a slate block. */
public final class TileEntitySlate extends TileEntity {
    public static final String TAG_PATTERN = "pattern";

    private HexPattern pattern;

    public HexPattern getPattern() {
        return pattern;
    }

    public void setPattern(HexPattern pattern) {
        if (pattern == null ? this.pattern == null : pattern.equals(this.pattern)) {
            return;
        }
        this.pattern = pattern;
        markDirty();
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (pattern != null) {
            compound.setTag(TAG_PATTERN, pattern.serializeToNBT());
        } else {
            compound.removeTag(TAG_PATTERN);
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        pattern = null;
        if (compound.hasKey(TAG_PATTERN, 10)) {
            NBTTagCompound patternTag = compound.getCompoundTag(TAG_PATTERN);
            if (patternTag.hasKey(HexPattern.TAG_START_DIR, 1)
                && patternTag.hasKey(HexPattern.TAG_ANGLES, 7)) {
                try {
                    pattern = HexPattern.fromNBT(patternTag);
                } catch (RuntimeException ignored) {
                    pattern = null;
                }
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
