package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.item.MediaHolderItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.List;

/** A stackable material whose total media can be consumed by Hex actions. */
public final class ItemMediaMaterial extends Item implements MediaHolderItem {
    private static final String KEY_MEDIA = "media";

    private final long mediaPerItem;
    private final String variant;
    private final int priority;

    public ItemMediaMaterial(long mediaPerItem, String variant, int priority) {
        this.mediaPerItem = Math.max(0L, mediaPerItem);
        this.variant = variant;
        this.priority = priority;
        setMaxStackSize(16);
    }

    @Override
    public long getMaxMedia(ItemStack stack) {
        long count = stack == null ? 1L : Math.max(1L, stack.getCount());
        return mediaPerItem > Long.MAX_VALUE / count ? Long.MAX_VALUE : mediaPerItem * count;
    }

    @Override
    public long getMedia(ItemStack stack) {
        long maximum = getMaxMedia(stack);
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null || !tag.hasKey(KEY_MEDIA, 4)) return maximum;
        return Math.max(0L, Math.min(maximum, tag.getLong(KEY_MEDIA)));
    }

    @Override
    public void setMedia(ItemStack stack, long media) {
        if (stack == null || stack.isEmpty()) return;
        long maximum = getMaxMedia(stack);
        long clamped = Math.max(0L, Math.min(maximum, media));
        NBTTagCompound tag = stack.getTagCompound();
        if (clamped == maximum) {
            if (tag != null) tag.removeTag(KEY_MEDIA);
            return;
        }
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setLong(KEY_MEDIA, clamped);
    }

    @Override
    public boolean canRecharge(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canProvide(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canConstructBattery(ItemStack stack) {
        return true;
    }

    @Override
    public int getConsumptionPriority(ItemStack stack) {
        return priority;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        tooltip.add(I18n.translateToLocalFormatted(
            "hexcasting.tooltip.media_material", getMedia(stack), getMaxMedia(stack)));
    }

    public String getVariant() {
        return variant;
    }
}
