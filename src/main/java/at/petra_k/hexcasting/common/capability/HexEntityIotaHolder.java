package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.addldata.ADIotaHolder;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.common.casting.IotaDataHolder;
import at.petra_k.hexcasting.common.entity.EntityWallScroll;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Delegates entity Iota storage to the entity's actual held ItemStack. */
public final class HexEntityIotaHolder implements ADIotaHolder {
    private static final String TAG_IOTA = "hexcasting_iota";
    private final Entity entity;

    public HexEntityIotaHolder(Entity entity) {
        this.entity = entity;
    }

    @Override
    public NBTTagCompound readIotaTag() {
        ItemStack stack = getStack();
        if (stack.isEmpty()) {
            return null;
        }
        if (HexCapabilities.IOTA != null) {
            ADIotaHolder capability = stack.getCapability(HexCapabilities.IOTA, null);
            if (capability != null) {
                NBTTagCompound tag = capability.readIotaTag();
                if (tag != null) {
                    return tag;
                }
            }
        }
        if (stack.getItem() instanceof at.petra_k.hexcasting.api.item.IotaHolderItem) {
            return ((at.petra_k.hexcasting.api.item.IotaHolderItem) stack.getItem())
                .readIotaTag(stack);
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(TAG_IOTA, 10)
            ? tag.getCompoundTag(TAG_IOTA) : null;
    }

    @Override
    public boolean writeIota(Iota iota, boolean simulate) {
        if (entity instanceof EntityWallScroll) {
            return false;
        }
        ItemStack stack = getStack();
        if (stack.isEmpty() || iota == null || !IotaDataHolder.canWrite(stack, iota)) {
            return false;
        }
        if (!simulate) {
            try {
                IotaDataHolder.write(stack, iota);
            } catch (CastingException ignored) {
                return false;
            }
            setStack(stack);
        }
        return true;
    }

    @Override
    public boolean writeable() {
        if (entity instanceof EntityWallScroll) {
            return false;
        }
        ItemStack stack = getStack();
        return !stack.isEmpty() && IotaDataHolder.canWrite(stack);
    }

    private ItemStack getStack() {
        if (entity instanceof EntityItem) {
            ItemStack stack = ((EntityItem) entity).getItem();
            return stack == null ? ItemStack.EMPTY : stack;
        }
        if (entity instanceof EntityItemFrame) {
            ItemStack stack = ((EntityItemFrame) entity).getDisplayedItem();
            return stack == null ? ItemStack.EMPTY : stack;
        }
        if (entity instanceof EntityWallScroll) {
            return ((EntityWallScroll) entity).getScroll();
        }
        return ItemStack.EMPTY;
    }

    private void setStack(ItemStack stack) {
        if (entity instanceof EntityItem) {
            ((EntityItem) entity).setItem(stack);
        } else if (entity instanceof EntityItemFrame) {
            ((EntityItemFrame) entity).setDisplayedItem(stack);
        } else if (entity instanceof EntityWallScroll) {
            ((EntityWallScroll) entity).setScroll(stack);
        }
        if (entity.world != null) {
            entity.world.notifyNeighborsOfStateChange(entity.getPosition(),
                entity.world.getBlockState(entity.getPosition()).getBlock(),
                false);
        }
    }
}
