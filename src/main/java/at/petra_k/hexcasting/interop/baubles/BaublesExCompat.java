package at.petra_k.hexcasting.interop.baubles;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** Direct BaublesEX 1.12.2 integration shared by gameplay and client code. */
public final class BaublesExCompat {
    private BaublesExCompat() {
    }

    public static void forEach(EntityLivingBase entity, Consumer<ItemStack> consumer) {
        if (entity == null || consumer == null) {
            return;
        }
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(entity);
        if (handler == null) {
            return;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            consumer.accept(handler.getStackInSlot(slot));
        }
    }

    public static boolean contains(EntityLivingBase entity, Predicate<ItemStack> predicate) {
        if (entity == null || predicate == null) {
            return false;
        }
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(entity);
        if (handler == null) {
            return false;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (predicate.test(handler.getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    public static long getAvailableMedia(EntityPlayer player,
                                         Predicate<ItemStack> mediaPredicate,
                                         MediaAmount mediaAmount) {
        if (player == null || mediaPredicate == null || mediaAmount == null) {
            return 0L;
        }
        final long[] total = {0L};
        forEach(player, stack -> {
            if (mediaPredicate.test(stack)) {
                long amount = mediaAmount.get(stack);
                if (amount > 0L) {
                    total[0] = saturatingAdd(total[0], amount);
                }
            }
        });
        return total[0];
    }

    private static long saturatingAdd(long left, long right) {
        return right > 0L && Long.MAX_VALUE - left < right
            ? Long.MAX_VALUE : left + Math.max(0L, right);
    }

    @FunctionalInterface
    public interface MediaAmount {
        long get(ItemStack stack);
    }
}

