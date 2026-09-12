package at.petra_k.hexcasting.api.casting.iota;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;

/** A serializable 1.12.2 block-state value used by compare_block. */
public final class BlockIota extends Iota {
    public static final String KEY_STATE_ID = "state_id";
    public static final IotaType<BlockIota> TYPE = new IotaType<>("block", data ->
        new BlockIota(Block.getStateById(data.getInteger(KEY_STATE_ID))));

    private final IBlockState state;

    public BlockIota(IBlockState state) {
        super(TYPE);
        this.state = Objects.requireNonNull(state, "state");
    }

    public IBlockState getState() {
        return state;
    }

    public Block getBlock() {
        return state.getBlock();
    }

    @Override
    public IBlockState getPayload() {
        return state;
    }

    @Override
    public boolean isTruthy() {
        return getBlock() != net.minecraft.init.Blocks.AIR;
    }

    @Override
    public String display() {
        return getBlock().getLocalizedName();
    }

    @Override
    protected void serializePayload(NBTTagCompound data) {
        data.setInteger(KEY_STATE_ID, Block.getStateId(state));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BlockIota && state.equals(((BlockIota) other).state);
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }
}
