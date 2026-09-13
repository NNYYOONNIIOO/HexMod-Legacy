package at.petra_k.hexcasting.common.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** A non-stackable knowledge item with an explicit 1.12.2 variant. */
public final class ItemHexKnowledge extends Item {
    private final String variant;

    public ItemHexKnowledge(String variant) {
        this.variant = variant == null ? "" : variant;
        setMaxStackSize(1);
    }

    public String getVariant() {
        return variant;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            if ("creative_unlocker".equals(variant)) {
                player.getEntityData().setBoolean("hexcasting_knowledge_unlocked", true);
            }
            String key = "hexcasting.message." + variant;
            String message = I18n.translateToLocal(key);
            if (!key.equals(message)) {
                player.sendMessage(new TextComponentString(message));
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        String key = "hexcasting.tooltip." + variant;
        String message = I18n.translateToLocal(key);
        if (!key.equals(message)) {
            tooltip.add(message);
        }
    }
}
