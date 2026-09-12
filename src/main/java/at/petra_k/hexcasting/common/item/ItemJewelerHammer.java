package at.petra_k.hexcasting.common.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;

/** A durable 1.12.2 tool item used by the Hex Casting crafting flow. */
public final class ItemJewelerHammer extends Item {
    public ItemJewelerHammer() {
        setMaxStackSize(1);
        setMaxDamage(128);
        setNoRepair();
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        tooltip.add(I18n.translateToLocal("hexcasting.tooltip.jeweler_hammer"));
    }
}
