package at.petra_k.hexcasting.common.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.List;

/** Knowledge-bearing item used by the lore fragment and creative unlocker. */
public final class ItemKnowledgeFragment extends Item {
    private static final String KEY_UNLOCKED = "hexcasting_knowledge_unlocked";
    private final String variant;

    public ItemKnowledgeFragment(String variant) {
        this.variant = variant;
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            if ("creative_unlocker".equals(variant)) {
                player.getEntityData().setBoolean(KEY_UNLOCKED, true);
            }
            player.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message." + variant)));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        tooltip.add(I18n.translateToLocal("hexcasting.tooltip." + variant));
    }
}
