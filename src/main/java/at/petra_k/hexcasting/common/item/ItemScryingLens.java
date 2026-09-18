package at.petra_k.hexcasting.common.item;

import baubles.api.IBauble;
import baubles.api.BaubleType;
import at.petra_k.hexcasting.interop.baubles.BaublesExCompat;
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
    /** The same multiplicative grid modifier as Hex's GRID_ZOOM attribute. */
    public static final double GRID_ZOOM = 0.33D;
    /** Non-zero sight value used by the legacy overlay bridge. */
    public static final double SCRY_SIGHT = 1.0D;
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
        // BaublesEX invokes this hook for the head slot.  The 1.12.2 port
        // computes the effective values from the current equipment instead
        // of mutating a player attribute every tick, so there is no per-stack
        // state to update here.
    }

    /** Return whether a player has a lens in a hand, head slot, or BaublesEX. */
    public static boolean isEquipped(EntityPlayer player) {
        if (player == null) {
            return false;
        }
        if (isLens(player.getHeldItemMainhand())
            || isLens(player.getHeldItemOffhand())) {
            return true;
        }
        for (ItemStack armor : player.inventory.armorInventory) {
            if (isLens(armor)) {
                return true;
            }
        }
        return BaublesExCompat.contains(player, ItemScryingLens::isLens);
    }

    /** Effective legacy equivalent of HexAttributes.GRID_ZOOM. */
    public static double getGridZoom(EntityPlayer player) {
        return isEquipped(player) ? 1.0D + GRID_ZOOM : 1.0D;
    }

    /** Effective legacy equivalent of HexAttributes.SCRY_SIGHT. */
    public static double getScrySight(EntityPlayer player) {
        return isEquipped(player) ? SCRY_SIGHT : 0.0D;
    }

    private static boolean isLens(ItemStack stack) {
        return stack != null && !stack.isEmpty()
            && stack.getItem() instanceof ItemScryingLens;
    }
}
