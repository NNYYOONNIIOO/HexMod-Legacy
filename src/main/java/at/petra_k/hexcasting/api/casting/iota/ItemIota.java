package at.petra_k.hexcasting.api.casting.iota;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;

/** A serializable 1.12.2 item-stack value used by compare_item. */
public final class ItemIota extends Iota {
    public static final String KEY_STACK = "stack";
    public static final IotaType<ItemIota> TYPE = new IotaType<>("item", data ->
        new ItemIota(new ItemStack(data.getCompoundTag(KEY_STACK))));

    private final ItemStack stack;

    public ItemIota(ItemStack stack) {
        super(TYPE);
        this.stack = Objects.requireNonNull(stack, "stack").copy();
    }

    public ItemStack getStack() {
        return stack.copy();
    }

    @Override
    public ItemStack getPayload() {
        return getStack();
    }

    @Override
    public boolean isTruthy() {
        return !stack.isEmpty();
    }

    @Override
    public String display() {
        return stack.isEmpty() ? "empty" : stack.getDisplayName();
    }

    @Override
    protected void serializePayload(NBTTagCompound data) {
        data.setTag(KEY_STACK, stack.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ItemIota
            && ItemStack.areItemStacksEqual(stack, ((ItemIota) other).stack);
    }

    @Override
    public int hashCode() {
        return stack.getItem().hashCode() * 31 + stack.getMetadata();
    }
}
