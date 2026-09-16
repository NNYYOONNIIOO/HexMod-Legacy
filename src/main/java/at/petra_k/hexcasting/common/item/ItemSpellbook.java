package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.item.IotaHolderItem;
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

/** A 64-page spellbook whose pages preserve complete drawable Hex patterns. */
public final class ItemSpellbook extends Item implements IotaHolderItem {
    /** Legacy pages were a list of action-id strings. */
    private static final String KEY_LEGACY_PAGES = "pages";
    /** Current pages are a list of serialized HexPattern compounds. */
    private static final String KEY_PATTERN_PAGES = "pattern_pages";
    /** Generic Iota pages use one-based numeric keys, like modern Hex. */
    private static final String KEY_IOTA_PAGES = "iota_pages";
    private static final String KEY_SEALED_PAGES = "sealed_pages";
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
            HexPattern selectedPattern = cyclePage(stack);
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted(
                    "hexcasting.message.scroll_selected", displayPattern(selectedPattern))));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        ItemStack offhand = player.getHeldItemOffhand();
        if (!offhand.isEmpty() && offhand.getItem() instanceof ItemPatternScroll) {
            HexPattern pattern = ItemPatternScroll.getPattern(offhand);
            if (pattern == null) {
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocal("hexcasting.message.program_empty")));
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
            if (writeActivePage(stack, pattern)) {
                ItemPatternScroll.consumeForWrite(offhand, player);
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted(
                        "hexcasting.message.program_added", displayPattern(pattern),
                        getPageIndex(stack) + 1, MAX_PAGES)));
            } else {
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted(
                        "hexcasting.message.program_full", MAX_PAGES)));
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        HexPattern pattern = getPattern(stack);
        if (pattern == null) {
            player.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.program_empty")));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        try {
            at.petra_k.hexcasting.api.capability.IHexCastingData data =
                HexCapabilities.CASTING_DATA == null
                    ? null : player.getCapability(HexCapabilities.CASTING_DATA, null);
            CastingStack result;
            if (data == null) {
                result = HexEvaluator.evaluate(Collections.singletonList(pattern));
            } else {
                HexEvaluator.evaluate(Collections.singletonList(pattern),
                    data.getCastingStack(), data, player);
                result = data.getCastingStack();
            }
            String resultText = result.isEmpty()
                ? I18n.translateToLocal("hexcasting.message.empty_stack")
                : result.peek().display();
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.scroll_result",
                    displayPattern(pattern), resultText)));
        } catch (CastingException exception) {
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.scroll_error",
                    exception.getMessage())));
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
        int pageCount = getPageCount(stack);
        int active = getPageIndex(stack);
        tooltip.add(I18n.translateToLocalFormatted(
            "hexcasting.tooltip.staff_program", pageCount == 0 ? 0 : active + 1, pageCount));
        HexPattern pattern = getPattern(stack);
        if (pattern != null) {
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.action") + ": "
                + displayPattern(pattern));
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.pattern") + ": "
                + HexInline.formatPattern(pattern, HexInline.DEFAULT_PATTERN_COLOR));
        }
    }

    /** Returns the registered action for the current page, if it has one. */
    public static ResourceLocation getActionId(ItemStack stack) {
        return getActionId(getPattern(stack));
    }

    /** Returns the complete pattern stored on the current page. */
    public static HexPattern getPattern(ItemStack stack) {
        NBTTagCompound storedTag = getStoredIotaTag(stack);
        if (storedTag != null) {
            Iota stored = readStoredIota(stack);
            return stored instanceof PatternIota
                ? ((PatternIota) stored).getPattern() : null;
        }
        return getPatternPage(stack);
    }

    /** Read the current page as a generic Hex Iota, not only as a pattern. */
    @Override
    public NBTTagCompound readIotaTag(ItemStack stack) {
        NBTTagCompound stored = getStoredIotaTag(stack);
        if (stored != null) {
            return stored;
        }
        HexPattern legacyPattern = getPatternPage(stack);
        return legacyPattern == null ? null : new PatternIota(legacyPattern).serialize();
    }

    @Override
    public boolean writeable(ItemStack stack) {
        return !isSealed(stack);
    }

    @Override
    public boolean canWrite(ItemStack stack, Iota datum) {
        return datum == null || !isSealed(stack);
    }

    /** Write or clear the current page while preserving arbitrary Iota types. */
    @Override
    public void writeDatum(ItemStack stack, Iota datum) {
        if (stack == null || stack.isEmpty() || (datum != null && isSealed(stack))) {
            return;
        }
        NBTTagCompound root = getOrCreateTag(stack);
        NBTTagCompound pages = root.hasKey(KEY_IOTA_PAGES, 10)
            ? root.getCompoundTag(KEY_IOTA_PAGES) : new NBTTagCompound();
        String pageKey = String.valueOf(getPageIndex(stack) + 1);
        if (datum == null) {
            pages.removeTag(pageKey);
        } else {
            pages.setTag(pageKey, datum.serialize());
        }
        if (pages.hasNoTags()) {
            root.removeTag(KEY_IOTA_PAGES);
        } else {
            root.setTag(KEY_IOTA_PAGES, pages);
        }
    }

    /** Set the modern per-page sealed flag used by spellbook recipes. */
    public static void setSealed(ItemStack stack, boolean sealed) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NBTTagCompound root = sealed ? getOrCreateTag(stack) : stack.getTagCompound();
        if (root == null) {
            return;
        }
        NBTTagCompound sealedPages = root.hasKey(KEY_SEALED_PAGES, 10)
            ? root.getCompoundTag(KEY_SEALED_PAGES) : new NBTTagCompound();
        String pageKey = String.valueOf(getPageIndex(stack) + 1);
        if (sealed) {
            sealedPages.setBoolean(pageKey, true);
        } else {
            sealedPages.removeTag(pageKey);
        }
        if (sealedPages.hasNoTags()) {
            root.removeTag(KEY_SEALED_PAGES);
        } else {
            root.setTag(KEY_SEALED_PAGES, sealedPages);
        }
    }

    public static boolean isSealed(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getTagCompound() == null) {
            return false;
        }
        NBTTagCompound root = stack.getTagCompound();
        if (!root.hasKey(KEY_SEALED_PAGES, 10)) {
            return false;
        }
        return root.getCompoundTag(KEY_SEALED_PAGES)
            .getBoolean(String.valueOf(getPageIndex(stack) + 1));
    }

    private static ResourceLocation getActionId(HexPattern pattern) {
        if (pattern == null) {
            return null;
        }
        at.petra_k.hexcasting.api.casting.action.HexAction action =
            HexActionRegistry.get(pattern);
        return action == null ? null : HexActionRegistry.idFor(action);
    }

    private static HexPattern cyclePage(ItemStack stack) {
        NBTTagCompound tag = getOrCreateTag(stack);
        int pageCount = getPageCount(stack);
        if (pageCount == 0) {
            tag.setInteger(KEY_ACTIVE, 0);
            return null;
        }
        int next = (getPageIndex(stack, pageCount) + 1) % pageCount;
        tag.setInteger(KEY_ACTIVE, next);
        return getPattern(stack);
    }

    /** Insert after the current page, matching the old book's page workflow. */
    private static boolean writeActivePage(ItemStack stack, HexPattern pattern) {
        if (pattern == null) {
            return false;
        }
        NBTTagCompound tag = getOrCreateTag(stack);
        NBTTagList pages = getOrCreatePatternPages(stack);
        if (pages.tagCount() >= MAX_PAGES) {
            int active = getPageIndex(stack, pages.tagCount());
            NBTTagList replacement = new NBTTagList();
            for (int i = 0; i < pages.tagCount(); i++) {
                replacement.appendTag(i == active
                    ? pattern.serializeToNBT() : pages.getCompoundTagAt(i));
            }
            tag.setTag(KEY_PATTERN_PAGES, replacement);
            return true;
        }

        if (pages.tagCount() == 0) {
            pages.appendTag(pattern.serializeToNBT());
            tag.setTag(KEY_PATTERN_PAGES, pages);
            tag.setInteger(KEY_ACTIVE, 0);
            return true;
        }

        int active = getPageIndex(stack, pages.tagCount());
        NBTTagList replacement = new NBTTagList();
        for (int i = 0; i < pages.tagCount(); i++) {
            replacement.appendTag(pages.getCompoundTagAt(i));
            if (i == active) {
                replacement.appendTag(pattern.serializeToNBT());
            }
        }
        tag.setTag(KEY_PATTERN_PAGES, replacement);
        tag.setInteger(KEY_ACTIVE, Math.min(active + 1, MAX_PAGES - 1));
        return true;
    }

    /**
     * Read current pattern pages and migrate old action-id pages once. The
     * legacy list remains in NBT for old tooling, but the compound list is the
     * authoritative source after migration.
     */
    private static NBTTagList getPatternPages(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getTagCompound() == null) {
            return new NBTTagList();
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey(KEY_PATTERN_PAGES, 9)) {
            return tag.getTagList(KEY_PATTERN_PAGES, 10);
        }

        NBTTagList legacy = tag.getTagList(KEY_LEGACY_PAGES, 8);
        if (legacy.tagCount() == 0) {
            return new NBTTagList();
        }
        HexActionRegistry.bootstrap();
        NBTTagList migrated = new NBTTagList();
        for (int i = 0; i < legacy.tagCount(); i++) {
            try {
                ResourceLocation id = new ResourceLocation(legacy.getStringTagAt(i));
                HexPattern pattern = HexActionRegistry.getPattern(id);
                if (pattern != null) {
                    migrated.appendTag(pattern.serializeToNBT());
                }
            } catch (RuntimeException ignored) {
                // Ignore only the malformed legacy page.
            }
        }
        if (migrated.tagCount() > 0) {
            tag.setTag(KEY_PATTERN_PAGES, migrated);
        }
        return migrated;
    }

    private static NBTTagList getOrCreatePatternPages(ItemStack stack) {
        NBTTagCompound tag = getOrCreateTag(stack);
        NBTTagList pages = getPatternPages(stack);
        if (!tag.hasKey(KEY_PATTERN_PAGES, 9)) {
            tag.setTag(KEY_PATTERN_PAGES, pages);
        }
        while (pages.tagCount() > MAX_PAGES) {
            pages.removeTag(pages.tagCount() - 1);
        }
        return pages;
    }

    private static int getPageCount(ItemStack stack) {
        return Math.min(MAX_PAGES, Math.max(
            getPatternPages(stack).tagCount(), getIotaPageCount(stack)));
    }

    private static int getPageIndex(ItemStack stack) {
        return getPageIndex(stack, getPageCount(stack));
    }

    private static int getPageIndex(ItemStack stack, int pageCount) {
        if (pageCount <= 0 || stack == null || stack.getTagCompound() == null) {
            return 0;
        }
        int active = stack.getTagCompound().getInteger(KEY_ACTIVE);
        return Math.max(0, Math.min(active, pageCount - 1));
    }

    private static int getIotaPageCount(ItemStack stack) {
        NBTTagCompound pages = getIotaPages(stack);
        int highest = 0;
        for (String key : pages.getKeySet()) {
            try {
                highest = Math.max(highest, Integer.parseInt(key));
            } catch (NumberFormatException ignored) {
                // Ignore non-page metadata without affecting valid pages.
            }
        }
        return highest;
    }

    private static NBTTagCompound getIotaPages(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getTagCompound() == null) {
            return new NBTTagCompound();
        }
        NBTTagCompound root = stack.getTagCompound();
        return root.hasKey(KEY_IOTA_PAGES, 10)
            ? root.getCompoundTag(KEY_IOTA_PAGES) : new NBTTagCompound();
    }

    private static NBTTagCompound getStoredIotaTag(ItemStack stack) {
        NBTTagCompound pages = getIotaPages(stack);
        String pageKey = String.valueOf(getPageIndex(stack, getIotaPageCount(stack)) + 1);
        if (!pages.hasKey(pageKey, 10)) {
            return null;
        }
        NBTTagCompound stored = pages.getCompoundTag(pageKey);
        return stored.hasKey("type", 8) && stored.hasKey("data", 10)
            ? stored : null;
    }

    private static Iota readStoredIota(ItemStack stack) {
        try {
            return ((ItemSpellbook) stack.getItem()).readIota(stack);
        } catch (CastingException | RuntimeException ignored) {
            return null;
        }
    }

    private static HexPattern getPatternPage(ItemStack stack) {
        NBTTagList pages = getPatternPages(stack);
        int active = getPageIndex(stack, pages.tagCount());
        if (active >= pages.tagCount()) {
            return null;
        }
        try {
            NBTTagCompound patternTag = pages.getCompoundTagAt(active);
            if (patternTag.hasKey(HexPattern.TAG_START_DIR, 1)
                && patternTag.hasKey(HexPattern.TAG_ANGLES, 7)) {
                return HexPattern.fromNBT(patternTag);
            }
        } catch (RuntimeException ignored) {
            // A malformed page is treated as empty while other pages survive.
        }
        return null;
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }

    private static String displayPattern(HexPattern pattern) {
        if (pattern == null) {
            return I18n.translateToLocal("hexcasting.message.program_empty");
        }
        ResourceLocation id = getActionId(pattern);
        if (id != null) {
            String key = "hexcasting.action." + id.getResourcePath();
            String translated = I18n.translateToLocal(key);
            return key.equals(translated) ? id.getResourcePath() : translated;
        }
        return pattern.signature();
    }
}
