package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import net.minecraft.item.Item;

/** A single-use or reusable packaged spell container for the 1.12.2 port. */
public final class ItemPackagedSpell extends Item {
    private static final String KEY_PACKAGED_ACTION = "packaged_action";

    /** Returns the action stored in this packaged spell, or null for an empty item. */
    public static ResourceLocation getPackagedAction(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null || !tag.hasKey(KEY_PACKAGED_ACTION, 8)) {
            return null;
        }
        try {
            ResourceLocation id = new ResourceLocation(tag.getString(KEY_PACKAGED_ACTION));
            return HexActionRegistry.getPattern(id) == null ? null : id;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public ItemPackagedSpell() {
        setMaxStackSize(1);
    }
}
