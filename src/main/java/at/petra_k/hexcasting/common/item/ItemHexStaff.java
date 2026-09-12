package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.casting.HexEvaluator;
import at.petra_k.hexcasting.common.lib.HexItems;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A programmable Hex Casting staff for the first 1.12.2 casting slice. */
public final class ItemHexStaff extends Item {
    public static final int MAX_PROGRAM_SIZE = 64;
    private static final String KEY_PROGRAM = "program";

    public ItemHexStaff() {
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack staff = player.getHeldItem(hand);
        if (!world.isRemote) {
            HexActionRegistry.bootstrap();
            ItemStack scroll = player.getHeldItemOffhand();
            if (isPatternScroll(scroll)) {
                if (player.isSneaking()) {
                    clearProgram(staff);
                    player.sendMessage(new TextComponentString(
                        I18n.translateToLocal("hexcasting.message.program_cleared")));
                } else {
                    ResourceLocation action = ItemPatternScroll.getActionId(scroll);
                    if (appendAction(staff, action)) {
                        player.sendMessage(new TextComponentString(
                            I18n.translateToLocalFormatted(
                                "hexcasting.message.program_added",
                                localizeAction(action), getProgramSize(staff), MAX_PROGRAM_SIZE)));
                    } else {
                        player.sendMessage(new TextComponentString(
                            I18n.translateToLocalFormatted(
                                "hexcasting.message.program_full", MAX_PROGRAM_SIZE)));
                    }
                }
                return new ActionResult<>(EnumActionResult.SUCCESS, staff);
            }

            List<HexPattern> program = getProgramPatterns(staff);
            if (program.isEmpty()) {
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocal("hexcasting.message.program_empty")));
                return new ActionResult<>(EnumActionResult.SUCCESS, staff);
            }

            try {
                CastingStack result = new CastingStack();
                IHexCastingData data = HexCapabilities.CASTING_DATA == null
                    ? null
                    : player.getCapability(HexCapabilities.CASTING_DATA, null);
                HexEvaluator.evaluate(program, result, data, player);
                String resultText = result.isEmpty()
                    ? I18n.translateToLocal("hexcasting.message.empty_stack")
                    : result.peek().display();
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted(
                        "hexcasting.message.program_result", resultText)));
            } catch (CastingException exception) {
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted(
                        "hexcasting.message.staff_error", localizeError(exception.getMessage()))));
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, staff);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        HexActionRegistry.bootstrap();
        List<ResourceLocation> actions = getProgramIds(stack);
        tooltip.add(I18n.translateToLocalFormatted(
            "hexcasting.tooltip.staff_program", actions.size(), MAX_PROGRAM_SIZE));
        int shown = Math.min(actions.size(), 8);
        for (int i = 0; i < shown; i++) {
            tooltip.add(I18n.translateToLocalFormatted(
                "hexcasting.tooltip.staff_entry", i + 1, localizeAction(actions.get(i))));
        }
        if (actions.size() > shown) {
            tooltip.add(I18n.translateToLocalFormatted(
                "hexcasting.tooltip.staff_more", actions.size() - shown));
        }
    }

    public static boolean isStaff(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == HexItems.STAFF;
    }

    public static boolean appendAction(ItemStack staff, ResourceLocation action) {
        if (!isStaff(staff) || action == null) {
            return false;
        }
        HexActionRegistry.bootstrap();
        if (HexActionRegistry.getPattern(action) == null || getProgramSize(staff) >= MAX_PROGRAM_SIZE) {
            return false;
        }
        NBTTagCompound tag = getOrCreateTag(staff);
        NBTTagList program = tag.getTagList(KEY_PROGRAM, 8);
        program.appendTag(new NBTTagString(action.toString()));
        tag.setTag(KEY_PROGRAM, program);
        return true;
    }

    public static void clearProgram(ItemStack staff) {
        if (!isStaff(staff)) {
            return;
        }
        NBTTagCompound tag = staff.getTagCompound();
        if (tag != null) {
            tag.removeTag(KEY_PROGRAM);
        }
    }

    public static int getProgramSize(ItemStack staff) {
        if (!isStaff(staff) || staff.getTagCompound() == null) {
            return 0;
        }
        return staff.getTagCompound().getTagList(KEY_PROGRAM, 8).tagCount();
    }

    public static List<ResourceLocation> getProgramIds(ItemStack staff) {
        if (!isStaff(staff) || staff.getTagCompound() == null) {
            return Collections.emptyList();
        }
        NBTTagList program = staff.getTagCompound().getTagList(KEY_PROGRAM, 8);
        List<ResourceLocation> result = new ArrayList<>(program.tagCount());
        for (int i = 0; i < program.tagCount(); i++) {
            String value = program.getStringTagAt(i);
            try {
                result.add(new ResourceLocation(value));
            } catch (RuntimeException ignored) {
                // Invalid old data is ignored while the remaining program is preserved.
            }
        }
        return result;
    }

    public static List<HexPattern> getProgramPatterns(ItemStack staff) {
        HexActionRegistry.bootstrap();
        List<HexPattern> result = new ArrayList<>();
        for (ResourceLocation id : getProgramIds(staff)) {
            HexPattern pattern = HexActionRegistry.getPattern(id);
            if (pattern != null) {
                result.add(pattern);
            }
        }
        return result;
    }

    private static boolean isPatternScroll(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == HexItems.PATTERN_SCROLL;
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }

    private static String localizeError(String message) {
        if (message == null || message.isEmpty()) {
            return I18n.translateToLocal("hexcasting.error.unknown");
        }
        String translated = I18n.translateToLocal(message);
        return message.equals(translated) ? message : translated;
    }

    private static String localizeAction(ResourceLocation id) {
        String key = "hexcasting.action." + id.getResourcePath();
        String translated = I18n.translateToLocal(key);
        return key.equals(translated) ? id.getResourcePath() : translated;
    }
}
