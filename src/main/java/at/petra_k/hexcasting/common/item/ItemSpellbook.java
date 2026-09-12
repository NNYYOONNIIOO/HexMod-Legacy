package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.casting.HexEvaluator;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.common.lib.hex.HexActions;
import at.petra_k.hexcasting.interop.inline.HexInline;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

/** A page-based spellbook for the 1.12.2 port. Pages store registered Hex actions. */
public final class ItemSpellbook extends Item {
    private static final String KEY_PAGES = "pages";
    private static final String KEY_ACTIVE = "active_page";
    public static final int MAX_PAGES = 64;

    public ItemSpellbook() {
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        HexActionRegistry.bootstrap();
        if (player.isSneaking()) {
            ResourceLocation selected = cyclePage(stack);
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.scroll_selected", localizeAction(selected))));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        ItemStack offhand = player.getHeldItemOffhand();
        if (!offhand.isEmpty() && offhand.getItem() instanceof ItemPatternScroll) {
            ResourceLocation action = ItemPatternScroll.getActionId(offhand);
            writeActivePage(stack, action);
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.program_added",
                    localizeAction(action), getPageIndex(stack) + 1, MAX_PAGES)));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        ResourceLocation action = getActionId(stack);
        HexPattern pattern = HexActionRegistry.getPattern(action);
        if (pattern == null) {
            player.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.program_empty")));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        try {
            IHexCastingData data = HexCapabilities.CASTING_DATA == null
                ? null : player.getCapability(HexCapabilities.CASTING_DATA, null);
            CastingStack result;
            if (data == null) {
                result = HexEvaluator.evaluate(Collections.singletonList(pattern));
            } else {
                HexEvaluator.evaluate(Collections.singletonList(pattern), data.getCastingStack(), data, player);
                result = data.getCastingStack();
            }
            String resultText = result.isEmpty()
                ? I18n.translateToLocal("hexcasting.message.empty_stack")
                : result.peek().display();
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.scroll_result",
                    action.getResourcePath(), resultText)));
        } catch (CastingException exception) {
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.scroll_error", exception.getMessage())));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (tab == getCreativeTab()) {
            items.add(new ItemStack(this));
        }
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        HexActionRegistry.bootstrap();
        NBTTagList pages = getPages(stack);
        int active = getPageIndex(stack);
        ResourceLocation action = getActionId(stack);
        tooltip.add(I18n.translateToLocalFormatted("hexcasting.tooltip.staff_program", active + 1, pages.tagCount()));
        tooltip.add(I18n.translateToLocal("hexcasting.tooltip.action") + ": " + localizeAction(action));
        HexPattern pattern = HexActionRegistry.getPattern(action);
        if (pattern != null) {
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.pattern") + ": " + HexInline.formatPattern(pattern));
        }
    }

    public static ResourceLocation getActionId(ItemStack stack) {
        HexActionRegistry.bootstrap();
        NBTTagList pages = getPages(stack);
        int active = getPageIndex(stack);
        if (pages.tagCount() > active) {
            try {
                ResourceLocation stored = new ResourceLocation(pages.getStringTagAt(active));
                if (HexActionRegistry.get(stored) != null) {
                    return stored;
                }
            } catch (RuntimeException ignored) {
                // Invalid page data falls back to a registered action.
            }
        }
        ResourceLocation fallback = HexActions.PUSH_ONE_ID;
        return HexActionRegistry.get(fallback) == null ? HexActionRegistry.firstId() : fallback;
    }

    private static ResourceLocation cyclePage(ItemStack stack) {
        NBTTagCompound tag = getOrCreateTag(stack);
        NBTTagList pages = ensurePages(tag);
        int next = (getPageIndex(stack) + 1) % pages.tagCount();
        tag.setInteger(KEY_ACTIVE, next);
        return getActionId(stack);
    }

    private static void writeActivePage(ItemStack stack, ResourceLocation action) {
        if (action == null || HexActionRegistry.getPattern(action) == null) {
            return;
        }
        NBTTagCompound tag = getOrCreateTag(stack);
        NBTTagList pages = tag.getTagList(KEY_PAGES, 8);
        if (pages.tagCount() == 0) {
            pages.appendTag(new NBTTagString(action.toString()));
            tag.setTag(KEY_PAGES, pages);
            tag.setInteger(KEY_ACTIVE, 0);
            return;
        }

        int active = getPageIndex(stack);
        if (pages.tagCount() >= MAX_PAGES) {
            NBTTagList replacement = new NBTTagList();
            for (int i = 0; i < pages.tagCount(); i++) {
                replacement.appendTag(new NBTTagString(i == active ? action.toString() : pages.getStringTagAt(i)));
            }
            tag.setTag(KEY_PAGES, replacement);
            return;
        }

        NBTTagList replacement = new NBTTagList();
        for (int i = 0; i < pages.tagCount(); i++) {
            replacement.appendTag(new NBTTagString(pages.getStringTagAt(i)));
            if (i == active) {
                replacement.appendTag(new NBTTagString(action.toString()));
            }
        }
        tag.setTag(KEY_PAGES, replacement);
        tag.setInteger(KEY_ACTIVE, Math.min(active + 1, MAX_PAGES - 1));
    }

    private static NBTTagList getPages(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? new NBTTagList() : tag.getTagList(KEY_PAGES, 8);
    }

    private static NBTTagList ensurePages(NBTTagCompound tag) {
        NBTTagList pages = tag.getTagList(KEY_PAGES, 8);
        if (pages.tagCount() == 0) {
            ResourceLocation first = HexActionRegistry.get(HexActions.PUSH_ONE_ID) == null
                ? HexActionRegistry.firstId() : HexActions.PUSH_ONE_ID;
            if (first != null) {
                pages.appendTag(new NBTTagString(first.toString()));
            }
            tag.setTag(KEY_PAGES, pages);
        }
        while (pages.tagCount() > MAX_PAGES) {
            pages.removeTag(pages.tagCount() - 1);
        }
        return pages;
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }

    private static int getPageIndex(ItemStack stack) {
        NBTTagList pages = getPages(stack);
        if (pages.tagCount() == 0) {
            return 0;
        }
        NBTTagCompound tag = stack.getTagCompound();
        int active = tag == null ? 0 : tag.getInteger(KEY_ACTIVE);
        return Math.max(0, Math.min(active, pages.tagCount() - 1));
    }

    private static String localizeAction(ResourceLocation id) {
        String key = "hexcasting.action." + id.getResourcePath();
        String translated = I18n.translateToLocal(key);
        return key.equals(translated) ? id.getResourcePath() : translated;
    }
}
