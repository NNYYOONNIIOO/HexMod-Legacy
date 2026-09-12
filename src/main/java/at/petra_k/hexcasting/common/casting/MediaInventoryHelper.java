package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.item.MediaHolderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;

/**
 * 1.12.2 media inventory bridge.  It deliberately scans only player-owned
 * stacks and keeps the persistent player capability as the final source.
 */
public final class MediaInventoryHelper {
    private MediaInventoryHelper() {
    }

    public static long getAvailableMedia(EntityPlayer player, IHexCastingData data) {
        long total = data == null ? 0L : Math.max(0L, data.getMedia());
        if (player == null) {
            return total;
        }
        for (ItemStack stack : player.inventory.mainInventory) {
            total = saturatingAdd(total, getAvailable(stack));
        }
        for (ItemStack stack : player.inventory.armorInventory) {
            total = saturatingAdd(total, getAvailable(stack));
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            total = saturatingAdd(total, getAvailable(stack));
        }
        // BaublesEX exposes an item-handler inventory rather than a vanilla
        // player list. Reflecting the small API keeps this bridge compatible
        // with both Baubles and BaublesEX without making the casting core
        // depend on a client-only implementation detail.
        total = saturatingAdd(total, getBaublesMedia(player));
        return total;
    }

    private static long getBaublesMedia(EntityPlayer player) {
        try {
            Class<?> api = Class.forName("baubles.api.BaublesApi");
            for (Method getBaubles : api.getMethods()) {
                if (!"getBaubles".equals(getBaubles.getName())
                    || getBaubles.getParameterTypes().length != 1
                    || !getBaubles.getParameterTypes()[0].isAssignableFrom(player.getClass())) {
                    continue;
                }
                Object handler = getBaubles.invoke(null, player);
                if (handler == null) {
                    return 0L;
                }
                Method getSlots = handler.getClass().getMethod("getSlots");
                Method getStackInSlot = handler.getClass().getMethod("getStackInSlot", int.class);
                int slots = Math.max(0, ((Number) getSlots.invoke(handler)).intValue());
                long total = 0L;
                for (int slot = 0; slot < slots; slot++) {
                    total = saturatingAdd(total,
                        getAvailable((ItemStack) getStackInSlot.invoke(handler, slot)));
                }
                return total;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Baubles is optional for the media bridge; vanilla inventories
            // remain fully functional if the handler API is unavailable.
        }
        return 0L;
    }

    private static long getAvailable(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof MediaHolderItem)) {
            return 0L;
        }
        MediaHolderItem holder = (MediaHolderItem) stack.getItem();
        return holder.canProvide(stack) ? holder.withdrawMedia(stack, -1L, true) : 0L;
    }

    private static long saturatingAdd(long left, long right) {
        if (right <= 0L || Long.MAX_VALUE - left < right) {
            return right <= 0L ? left : Long.MAX_VALUE;
        }
        return left + right;
    }
}
