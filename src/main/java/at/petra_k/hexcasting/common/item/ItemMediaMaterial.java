package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.item.MediaHolderItem;
import net.minecraft.entity.item.EntityItem;
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
        setMaxStackSize(64);
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

    /**
     * Static media is consumed in whole source items, just like the modern
     * static-media capability.  The entity-specific extraction method below
     * is deliberately separate because Craft Phial must preserve a partial
     * remainder on a dropped stack instead of writing one total-media tag to
     * every item in the stack.
     */
    @Override
    public long withdrawMedia(ItemStack stack, long amount, boolean simulate) {
        if (stack == null || stack.isEmpty() || mediaPerItem <= 0L) {
            return 0L;
        }
        long available = Math.max(0L, getMedia(stack));
        long requested = amount < 0L ? available : Math.max(0L, amount);
        if (available <= 0L || requested <= 0L) {
            return 0L;
        }
        long items = ceilDivide(requested, mediaPerItem);
        items = Math.min(items, Math.max(0L, (long) stack.getCount()));
        long extracted = items > Long.MAX_VALUE / mediaPerItem
            ? Long.MAX_VALUE : items * mediaPerItem;
        if (!simulate && items > 0L) {
            stack.shrink((int) Math.min(Integer.MAX_VALUE, items));
        }
        return extracted;
    }

    /**
     * Withdraw media from a dropped stack while keeping per-item media
     * semantics.  An ItemStack has one NBT compound for the whole stack, so a
     * partially consumed material must be split into a full stack and one
     * separately tagged item rather than tagging every remaining item.
     */
    public long withdrawMediaFromEntity(EntityItem entity, long amount,
                                        boolean simulate) {
        if (entity == null) {
            return 0L;
        }
        ItemStack stack = entity.getItem();
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }

        long available = Math.max(0L, getMedia(stack));
        long requested = amount < 0L
            ? available : Math.max(0L, amount);
        long extracted = Math.min(available, requested);
        if (simulate || extracted <= 0L) {
            return extracted;
        }

        splitRemainingEntity(entity, stack, available - extracted);
        return extracted;
    }

    private void splitRemainingEntity(EntityItem entity, ItemStack stack,
                                      long remainingMedia) {
        if (remainingMedia <= 0L || mediaPerItem <= 0L) {
            entity.setItem(ItemStack.EMPTY);
            entity.setDead();
            return;
        }

        long fullCount = remainingMedia / mediaPerItem;
        long partialMedia = remainingMedia % mediaPerItem;
        if (partialMedia <= 0L) {
            stack.setCount((int) Math.min(Integer.MAX_VALUE, fullCount));
            // A complete stack needs no media tag.  Calling setMedia also
            // removes a legacy total-stack tag left by older port versions.
            setMedia(stack, remainingMedia);
            entity.setItem(stack);
            return;
        }

        if (fullCount <= 0L) {
            stack.setCount(1);
            setMedia(stack, partialMedia);
            entity.setItem(stack);
            return;
        }

        // Copy before clearing the legacy total-media tag from the full stack
        // so that the partial item keeps all unrelated item NBT.
        ItemStack partialStack = stack.copy();
        partialStack.setCount(1);
        setMedia(partialStack, partialMedia);

        stack.setCount((int) Math.min(Integer.MAX_VALUE, fullCount));
        setMedia(stack, saturatingMultiply(fullCount, mediaPerItem));
        entity.setItem(stack);

        EntityItem partialEntity = new EntityItem(entity.world,
            entity.posX, entity.posY, entity.posZ, partialStack);
        partialEntity.motionX = entity.motionX;
        partialEntity.motionY = entity.motionY;
        partialEntity.motionZ = entity.motionZ;
        entity.world.spawnEntity(partialEntity);
    }

    private static long ceilDivide(long numerator, long denominator) {
        if (numerator <= 0L || denominator <= 0L) {
            return 0L;
        }
        long quotient = numerator / denominator;
        return numerator % denominator == 0L ? quotient : quotient + 1L;
    }

    private static long saturatingMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) {
            return 0L;
        }
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    @Override
    public int getConsumptionPriority(ItemStack stack) {
        return priority;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        MediaTooltip.add(tooltip, getMedia(stack), getMaxMedia(stack));
    }

    public String getVariant() {
        return variant;
    }
}
