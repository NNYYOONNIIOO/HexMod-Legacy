package at.petra_k.hexcasting.common.item;

import baubles.api.IBauble;
import baubles.api.BaubleType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/** BaublesEX-backed wearable equivalent of Hex Casting's scrying lens. */
public final class ItemScryingLens extends Item implements IBauble {
    private static final String KEY_LENS_ENABLED = "lens_enabled";
    public ItemScryingLens() {
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }
            boolean enabled = !tag.getBoolean(KEY_LENS_ENABLED);
            tag.setBoolean(KEY_LENS_ENABLED, enabled);
            player.sendMessage(new TextComponentString(I18n.translateToLocal(enabled
                ? "hexcasting.message.lens_enabled"
                : "hexcasting.message.lens_disabled")));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.HEAD;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        // Pattern overlays and entity inspection will be connected here after
        // the 1.12.2 client rendering layer is migrated.
    }
}
