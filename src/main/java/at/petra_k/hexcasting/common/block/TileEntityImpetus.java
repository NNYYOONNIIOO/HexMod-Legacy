package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.casting.circles.CircleExecutionState;
import at.petra_k.hexcasting.api.addldata.ADMediaHolder;
import at.petra_k.hexcasting.common.casting.StaffCastExecutor;
import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.common.lib.HexItems;
import net.minecraft.util.EnumFacing;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;

import java.util.ArrayList;
import java.util.List;

/** Stores a bound impetus program and its resumable circle execution state. */
public final class TileEntityImpetus extends TileEntity
    implements ITickable, ADMediaHolder {
    private static final String KEY_PATTERNS = "patterns";
    private static final String KEY_CASTER = "caster";
    private static final String KEY_POWERED = "powered";
    private static final String KEY_MEDIA = "media";
    private static final long MAX_MEDIA = 9_000_000_000_000_000_000L;

    private NBTTagList patterns = new NBTTagList();
    private ItemStack caster = ItemStack.EMPTY;
    private boolean powered;
    private long media;
    private CircleExecutionState executionState;
    private NBTTagCompound lazyExecutionState;

    public int getProgramSize() {
        return patterns.tagCount();
    }

    public boolean isPowered() {
        return powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
        markDirty();
    }

    @Override
    public long getMedia() {
        return media;
    }

    @Override
    public long getMaxMedia() {
        return MAX_MEDIA;
    }

    @Override
    public void setMedia(long media) {
        this.media = Math.max(0L, Math.min(MAX_MEDIA, media));
        markDirty();
    }

    @Override
    public boolean canRecharge() {
        return true;
    }

    @Override
    public boolean canProvide() {
        return true;
    }

    @Override
    public int getConsumptionPriority() {
        return 0;
    }

    @Override
    public boolean canConstructBattery() {
        return false;
    }

    public void bindProgram(NBTTagList incoming) {
        endExecution();
        patterns = copyPatterns(incoming);
        caster = new ItemStack(HexItems.STAFF);
        StaffCastExecutor.clear(caster);
        markDirty();
    }

    /** Apply a program restored from the item's BlockEntityTag. */
    public void restoreProgram(NBTTagList incoming) {
        endExecution();
        patterns = copyPatterns(incoming);
        caster = new ItemStack(HexItems.STAFF);
        StaffCastExecutor.clear(caster);
        markDirty();
    }

    public NBTTagList getProgramSnapshot() {
        return copyPatterns(patterns);
    }

    public List<HexPattern> getBoundPatterns() {
        if (caster.isEmpty()) {
            caster = new ItemStack(HexItems.STAFF);
        }
        ItemHexStaff.replaceProgram(caster, patterns);
        return new ArrayList<>(ItemHexStaff.getProgramPatterns(caster));
    }

    public BlockImpetus.TriggerMode getTriggerMode() {
        IBlockState state = getWorld() == null ? null : getWorld().getBlockState(pos);
        return state != null && state.getBlock() instanceof BlockImpetus
            ? ((BlockImpetus) state.getBlock()).getTriggerMode()
            : BlockImpetus.TriggerMode.RIGHT_CLICK;
    }

    public EnumFacing getStartDirection() {
        IBlockState state = getWorld() == null ? null : getWorld().getBlockState(pos);
        return state != null && state.getBlock() instanceof BlockImpetus
            ? state.getValue(BlockImpetus.FACING) : EnumFacing.UP;
    }

    public boolean isExecuting() {
        return executionState != null || lazyExecutionState != null;
    }

    public CircleExecutionState getExecutionState() {
        if (executionState == null && lazyExecutionState != null && getWorld() != null) {
            try {
                executionState = CircleExecutionState.deserialize(lazyExecutionState);
            } catch (Exception ignored) {
                lazyExecutionState = null;
            }
        }
        return executionState;
    }

    public void trigger(EntityPlayer player, EnumHand hand) {
        startExecution(player);
    }

    public void startExecution(EntityPlayer player) {
        if (getWorld() == null || getWorld().isRemote || isExecuting()) {
            return;
        }
        CircleExecutionState.CreationResult result =
            CircleExecutionState.createNew(this, player);
        if (!result.isSuccess()) {
            return;
        }
        executionState = result.getState();
        lazyExecutionState = null;
        setEnergized(true);
        markDirty();
    }

    public void endExecution() {
        CircleExecutionState current = getExecutionState();
        if (current != null) {
            current.endExecution(this);
        }
        executionState = null;
        lazyExecutionState = null;
        setEnergized(false);
        markDirty();
    }

    private void setEnergized(boolean energized) {
        if (getWorld() == null) {
            return;
        }
        IBlockState state = getWorld().getBlockState(pos);
        if (state.getBlock() instanceof BlockCircleComponent) {
            IBlockState updated = state.withProperty(BlockCircleComponent.ENERGIZED,
                energized);
            if (!updated.equals(state)) {
                getWorld().setBlockState(pos, updated, 3);
            }
        }
    }

    @Override
    public void update() {
        if (getWorld() == null || getWorld().isRemote) {
            return;
        }
        CircleExecutionState current = getExecutionState();
        if (current == null) {
            return;
        }
        if (!current.tick(this)) {
            current.endExecution(this);
            executionState = null;
            lazyExecutionState = null;
            setEnergized(false);
        }
        markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag(KEY_PATTERNS, getProgramSnapshot());
        compound.setBoolean(KEY_POWERED, powered);
        compound.setLong(KEY_MEDIA, media);
        CircleExecutionState current = getExecutionState();
        if (current != null) {
            compound.setTag("execution", current.serialize());
        } else if (lazyExecutionState != null) {
            compound.setTag("execution", lazyExecutionState.copy());
        }
        if (!caster.isEmpty()) {
            compound.setTag(KEY_CASTER, caster.writeToNBT(new NBTTagCompound()));
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        patterns = compound.hasKey(KEY_PATTERNS, 9)
            ? copyPatterns(compound.getTagList(KEY_PATTERNS, 10)) : new NBTTagList();
        powered = compound.getBoolean(KEY_POWERED);
        media = Math.max(0L, Math.min(MAX_MEDIA, compound.getLong(KEY_MEDIA)));
        executionState = null;
        lazyExecutionState = compound.hasKey("execution", 10)
            ? compound.getCompoundTag("execution").copy() : null;
        caster = compound.hasKey(KEY_CASTER, 10)
            ? new ItemStack(compound.getCompoundTag(KEY_CASTER)) : ItemStack.EMPTY;
    }

    private static NBTTagList copyPatterns(NBTTagList source) {
        NBTTagList copy = new NBTTagList();
        if (source == null) {
            return copy;
        }
        for (int i = 0; i < source.tagCount(); i++) {
            copy.appendTag(source.getCompoundTagAt(i).copy());
        }
        return copy;
    }
}
