package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.item.MediaHolderItem;
import at.petra_k.hexcasting.api.misc.MediaConstants;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.List;

/** Portable NBT-backed media storage for the 1.12.2 port. */
public final class ItemMediaBattery extends Item implements MediaHolderItem {
    public static final String KEY_MEDIA = "media";
    public static final long DEFAULT_MAX_MEDIA = MediaConstants.CRYSTAL_UNIT * 64L;

    public ItemMediaBattery() { setMaxStackSize(1); }

    @Override public long getMaxMedia(ItemStack stack) { return DEFAULT_MAX_MEDIA; }

    @Override
    public long getMedia(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null || !tag.hasKey(KEY_MEDIA, 4) ? 0L : clamp(tag.getLong(KEY_MEDIA));
    }

    @Override
    public void setMedia(ItemStack stack, long media) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) { tag = new NBTTagCompound(); stack.setTagCompound(tag); }
        tag.setLong(KEY_MEDIA, clamp(media));
    }

    @Override public int getConsumptionPriority(ItemStack stack) { return 4000; }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (tab != getCreativeTab()) {
            return;
        }
        items.add(new ItemStack(this));
        addFilledVariant(items, MediaConstants.DUST_UNIT * 64L);
        addFilledVariant(items, MediaConstants.SHARD_UNIT * 64L);
        addFilledVariant(items, MediaConstants.CRYSTAL_UNIT * 64L);
    }

    private void addFilledVariant(NonNullList<ItemStack> items, long media) {
        ItemStack battery = new ItemStack(this);
        setMedia(battery, media);
        items.add(battery);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.AQUA + I18n.translateToLocalFormatted(
            "hexcasting.tooltip.media", getMedia(stack), getMaxMedia(stack)));
    }

    private static long clamp(long media) {
        return Math.max(0L, Math.min(DEFAULT_MAX_MEDIA, media));
    }
}
