package at.petra_k.hexcasting.common.item;

import baubles.api.IBauble;
import baubles.api.BaubleType;
import at.petra_k.hexcasting.common.lib.HexAttributes;
import at.petra_k.hexcasting.interop.baubles.BaublesExCompat;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** BaublesEX-backed wearable equivalent of Hex Casting's scrying lens. */
public final class ItemScryingLens extends Item implements IBauble {
    /** The same multiplicative grid modifier as Hex's GRID_ZOOM attribute. */
    public static final double GRID_ZOOM = 0.33D;
    /** Non-zero sight value used by the legacy overlay bridge. */
    public static final double SCRY_SIGHT = 1.0D;
    public ItemScryingLens() {
        setMaxStackSize(1);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(this,
            new BehaviorDefaultDispenseItem() {
                @Override
                protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                    ItemStack remaining = ItemArmor.dispenseArmor(source, stack);
                    return remaining.isEmpty()
                        ? super.dispenseStack(source, stack)
                        : remaining;
                }
            });
    }

    @Override
    public EntityEquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EntityEquipmentSlot.HEAD;
    }

    @Override
    public boolean isValidArmor(ItemStack stack, EntityEquipmentSlot armorType, Entity entity) {
        return armorType == EntityEquipmentSlot.HEAD;
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
        if (isLens(player.getItemStackFromSlot(EntityEquipmentSlot.HEAD))) {
            return true;
        }
        return BaublesExCompat.contains(player, ItemScryingLens::isLens);
    }

    /** Effective legacy equivalent of HexAttributes.GRID_ZOOM. */
    public static double getGridZoom(EntityPlayer player) {
        if (isFeebleMind(player)) {
            return 1.0D;
        }
        if (player != null && player.getAttributeMap()
            .getAttributeInstance(HexAttributes.GRID_ZOOM) != null) {
            return player.getEntityAttribute(HexAttributes.GRID_ZOOM).getAttributeValue();
        }
        return isEquipped(player) ? 1.0D + GRID_ZOOM : 1.0D;
    }

    /** Effective legacy equivalent of HexAttributes.SCRY_SIGHT. */
    public static double getScrySight(EntityPlayer player) {
        if (isFeebleMind(player)) {
            return 0.0D;
        }
        if (player != null && player.getAttributeMap()
            .getAttributeInstance(HexAttributes.SCRY_SIGHT) != null) {
            return player.getEntityAttribute(HexAttributes.SCRY_SIGHT).getAttributeValue();
        }
        return isEquipped(player) ? SCRY_SIGHT : 0.0D;
    }

    private static boolean isLens(ItemStack stack) {
        return stack != null && !stack.isEmpty()
            && stack.getItem() instanceof ItemScryingLens;
    }

    private static boolean isFeebleMind(EntityPlayer player) {
        return player != null && player.getAttributeMap()
            .getAttributeInstance(HexAttributes.FEEBLE_MIND) != null
            && player.getEntityAttribute(HexAttributes.FEEBLE_MIND)
                .getAttributeValue() > 0.0D;
    }
}
