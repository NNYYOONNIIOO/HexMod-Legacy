package at.petra_k.hexcasting.common.item;
import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;

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
    public static final String KEY_MAX_MEDIA = "max_media";
    public static final String SOURCE_MAX_MEDIA_KEY = "hexcasting:start_media";
    public static final long DEFAULT_MAX_MEDIA = MediaConstants.CRYSTAL_UNIT * 64L;

    public ItemMediaBattery() { setMaxStackSize(1); }

    @Override
    public long getMaxMedia(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            if (tag.hasKey(KEY_MAX_MEDIA, 4)) {
                return Math.max(1L, tag.getLong(KEY_MAX_MEDIA));
            }
            if (tag.hasKey(SOURCE_MAX_MEDIA_KEY, 4)) {
                return Math.max(1L, tag.getLong(SOURCE_MAX_MEDIA_KEY));
            }
        }
        return DEFAULT_MAX_MEDIA;
    }

    @Override
    public long getMedia(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null || !tag.hasKey(KEY_MEDIA, 4) ? 0L : clamp(stack, tag.getLong(KEY_MEDIA));
    }

    /** Store the capacity on the stack so each phial/battery keeps its own size. */
    public void setMaxMedia(ItemStack stack, long maxMedia) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        long capacity = Math.max(1L, maxMedia);
        tag.setLong(KEY_MAX_MEDIA, capacity);
        tag.setLong(SOURCE_MAX_MEDIA_KEY, capacity);
        if (tag.hasKey(KEY_MEDIA, 4) && tag.getLong(KEY_MEDIA) > capacity) {
            tag.setLong(KEY_MEDIA, capacity);
        }
    }

    @Override
    public void setMedia(ItemStack stack, long media) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) { tag = new NBTTagCompound(); stack.setTagCompound(tag); }
        tag.setLong(KEY_MEDIA, clamp(stack, media));
    }

    @Override public int getConsumptionPriority(ItemStack stack) { return 4000; }

    @Override public boolean canProvide(ItemStack stack) { return true; }

    @Override public boolean canRecharge(ItemStack stack) { return true; }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (tab != getCreativeTab()) {
            return;
        }
        items.add(new ItemStack(this));
        addFilledVariant(items, MediaConstants.DUST_UNIT * 64L);
        addFilledVariant(items, MediaConstants.SHARD_UNIT * 64L);
        addFilledVariant(items, MediaConstants.CRYSTAL_UNIT * 64L);
        addFilledVariant(items, MediaConstants.QUENCHED_SHARD_UNIT * 64L);
        addFilledVariant(items, MediaConstants.QUENCHED_BLOCK_UNIT * 64L);
    }

    private void addFilledVariant(NonNullList<ItemStack> items, long media) {
        ItemStack battery = new ItemStack(this);
        setMaxMedia(battery, media);
        setMedia(battery, media);
        items.add(battery);
    }

    /** Transfer stored battery media into the player's persistent reserve. */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack battery = player.getHeldItem(hand);
        if (!world.isRemote) {
            IHexCastingData data = HexCapabilities.CASTING_DATA == null
                ? null : player.getCapability(HexCapabilities.CASTING_DATA, null);
            if (data == null) {
                player.sendMessage(new TextComponentString(I18n.translateToLocal("hexcasting.message.media_unavailable")));
            } else {
                long stored = getMedia(battery);
                long room = Math.max(0L, data.getMaxMedia() - data.getMedia());
                long transfer = Math.min(stored, room);
                if (transfer > 0L) {
                    setMedia(battery, stored - transfer);
                    data.setMedia(data.getMedia() + transfer);
                    player.sendMessage(new TextComponentString(I18n.translateToLocalFormatted("hexcasting.message.media_recharged", transfer, data.getMedia(), data.getMaxMedia())));
                } else if (stored <= 0L) {
                    player.sendMessage(new TextComponentString(I18n.translateToLocal("hexcasting.message.media_empty")));
                } else {
                    player.sendMessage(new TextComponentString(I18n.translateToLocal("hexcasting.message.media_full")));
                }
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, battery);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        MediaTooltip.add(tooltip, getMedia(stack), getMaxMedia(stack));
    }

    private long clamp(ItemStack stack, long media) {
        return Math.max(0L, Math.min(getMaxMedia(stack), media));
    }
}
