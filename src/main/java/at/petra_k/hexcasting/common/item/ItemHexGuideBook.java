package at.petra_k.hexcasting.common.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import vazkii.patchouli.common.item.ItemModBook;

/** Patchouli 1.12.2 guide-book item for the Hex Casting port. */
public final class ItemHexGuideBook extends ItemModBook {
    private static final String BOOK_TAG = "patchouli:book";
    private static final String BOOK_ID = "hexcasting:hexcasting";

    public ItemHexGuideBook() {
        super();
        setRegistryName("hexcasting", "guide_book");
        setUnlocalizedName("hexcasting.guide_book");
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        ensureBookTag(stack);
        return super.onItemRightClick(world, player, hand);
    }

    private static void ensureBookTag(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setString(BOOK_TAG, BOOK_ID);
        stack.setTagCompound(tag);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return "Hex Casting Guide";
    }
}
