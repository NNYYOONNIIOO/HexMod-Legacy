package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.item.MediaHolderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** Creative/debug item with the upstream infinite-media behavior. */
public final class ItemCreativeUnlocker extends Item implements MediaHolderItem {
    private static final String KEY_UNLOCKED = "hexcasting_knowledge_unlocked";

    public ItemCreativeUnlocker() {
        setMaxStackSize(1);
    }

    @Override
    public long getMaxMedia(ItemStack stack) {
        return Long.MAX_VALUE;
    }

    @Override
    public long getMedia(ItemStack stack) {
        return Long.MAX_VALUE;
    }

    @Override
    public void setMedia(ItemStack stack, long media) {
        // Infinite media is intentionally not stored in NBT.
    }

    @Override
    public boolean canProvide(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canRecharge(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canConstructBattery(ItemStack stack) {
        return false;
    }

    @Override
    public int getConsumptionPriority(ItemStack stack) {
        return 10000;
    }

    @Override
    public long withdrawMedia(ItemStack stack, long amount, boolean simulate) {
        return amount < 0L ? Long.MAX_VALUE : Math.max(0L, amount);
    }

    @Override
    public long insertMedia(ItemStack stack, long amount, boolean simulate) {
        return amount < 0L ? Long.MAX_VALUE : Math.max(0L, amount);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            player.getEntityData().setBoolean(KEY_UNLOCKED, true);
            player.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.creative_unlocker")));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        String key = "hexcasting.tooltip.creative_unlocker";
        String message = I18n.translateToLocal(key);
        if (!key.equals(message)) {
            tooltip.add(message);
        }
    }
}
