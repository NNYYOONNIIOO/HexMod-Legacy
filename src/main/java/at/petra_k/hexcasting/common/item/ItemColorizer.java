package at.petra_k.hexcasting.common.item;

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

import java.util.List;
import java.util.Locale;

/** A pigment item that stores a color on a focus or staff in 1.12.2. */
public final class ItemColorizer extends Item {
    private static final String KEY_COLOR = "hexcasting_color";

    private final String variant;
    private final int color;

    public ItemColorizer(String variant) {
        this.variant = variant;
        this.color = colorFor(variant);
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack colorizer = player.getHeldItem(hand);
        EnumHand otherHand = hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        ItemStack target = player.getHeldItem(otherHand);
        if (!world.isRemote && isColorable(target)) {
            NBTTagCompound tag = target.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                target.setTagCompound(tag);
            }
            tag.setInteger(KEY_COLOR, color);
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.colorized", target.getDisplayName())));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, colorizer);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        String name = I18n.translateToLocal("item.hexcasting." + variant + ".name");
        tooltip.add(I18n.translateToLocalFormatted("hexcasting.tooltip.colorizer", name));
        tooltip.add(I18n.translateToLocalFormatted(
            "hexcasting.tooltip.colorizer_hex", String.format(Locale.ROOT, "#%06X", color)));
    }

    public static boolean isColorable(ItemStack stack) {
        return stack != null && !stack.isEmpty()
            && (stack.getItem() instanceof ItemHexFocus || stack.getItem() instanceof ItemHexStaff);
    }

    public static int getColor(ItemStack stack) {
        if (stack == null || stack.getTagCompound() == null
            || !stack.getTagCompound().hasKey(KEY_COLOR, 3)) {
            return -1;
        }
        return stack.getTagCompound().getInteger(KEY_COLOR);
    }

    private static int colorFor(String id) {
        if (id.contains("white")) return 0xF9FFFE;
        if (id.contains("orange")) return 0xF9801D;
        if (id.contains("magenta")) return 0xC74EBD;
        if (id.contains("light_blue")) return 0x3AB3DA;
        if (id.contains("yellow")) return 0xFED83D;
        if (id.contains("lime")) return 0x80C71F;
        if (id.contains("pink")) return 0xF38BAA;
        if (id.contains("light_gray")) return 0x9D9D97;
        if (id.contains("gray")) return 0x474F52;
        if (id.contains("cyan")) return 0x169C9C;
        if (id.contains("purple")) return 0x8932B8;
        if (id.contains("blue")) return 0x3C44AA;
        if (id.contains("brown")) return 0x835432;
        if (id.contains("green")) return 0x5E7C16;
        if (id.contains("red")) return 0xB02E26;
        if (id.contains("black")) return 0x1D1D21;
        if (id.contains("ancient")) return 0xE6B84A;
        if (id.contains("uuid")) return 0xB78CFF;
        return 0xB58CFF;
    }
}
