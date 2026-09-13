package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.casting.StaffCastExecutor;
import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.common.lib.HexItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;

import java.util.List;

/** Stores a bound impetus program and its resumable casting state. */
public final class TileEntityImpetus extends TileEntity {
    private static final String KEY_PATTERNS = "patterns";
    private static final String KEY_CASTER = "caster";
    private static final String KEY_POWERED = "powered";

    private NBTTagList patterns = new NBTTagList();
    private ItemStack caster = ItemStack.EMPTY;
    private boolean powered;

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

    public void bindProgram(NBTTagList incoming) {
        patterns = copyPatterns(incoming);
        caster = new ItemStack(HexItems.STAFF);
        StaffCastExecutor.clear(caster);
        markDirty();
    }

    /** Apply a program restored from the item's BlockEntityTag. */
    public void restoreProgram(NBTTagList incoming) {
        patterns = copyPatterns(incoming);
        caster = new ItemStack(HexItems.STAFF);
        StaffCastExecutor.clear(caster);
        markDirty();
    }

    public NBTTagList getProgramSnapshot() {
        return copyPatterns(patterns);
    }

    /** Execute the bound program with the player as the casting environment. */
    public void trigger(EntityPlayer player, EnumHand hand) {
        if (player == null || patterns.tagCount() == 0) {
            return;
        }
        if (caster.isEmpty()) {
            caster = new ItemStack(HexItems.STAFF);
        }
        ItemHexStaff.replaceProgram(caster, patterns);
        List<HexPattern> program = ItemHexStaff.getProgramPatterns(caster);
        for (HexPattern pattern : program) {
            if (!StaffCastExecutor.execute(player, hand, caster, pattern)) {
                break;
            }
        }
        markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag(KEY_PATTERNS, getProgramSnapshot());
        compound.setBoolean(KEY_POWERED, powered);
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
