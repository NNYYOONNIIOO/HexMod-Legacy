package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.item.IotaHolderItem;
import at.petra_k.hexcasting.api.item.VariantItem;
import at.petra_k.hexcasting.common.casting.HexEvaluator;
import at.petra_k.hexcasting.common.casting.MishapFeedback;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.interop.inline.HexInline;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
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
public final class ItemSpellbook extends Item implements IotaHolderItem, VariantItem {
    /** One-based selected page; zero means that the book has no pages. */
    public static final String TAG_SELECTED_PAGE = "page_idx";
    /** A compound of one-based page numbers to serialized Iotas. */
    public static final String TAG_PAGES = "pages";
    /** A compound of one-based page numbers to the page's custom name. */
    public static final String TAG_PAGE_NAMES = "page_names";
    /** A compound of one-based page numbers to sealed flags. */
    public static final String TAG_SEALED = "sealed_pages";
    /** The visual spellbook variant, clamped to the eight supplied models. */
    public static final String TAG_VARIANT = "variant";

    /** Data keys used by earlier 1.12.2 snapshots of this port. */
    private static final String KEY_PATTERN_PAGES = "pattern_pages";
    private static final String KEY_IOTA_PAGES = "iota_pages";
    private static final String KEY_ACTIVE = "active_page";
    private static final String KEY_LEGACY_SEALED = "sealed";
    public static final int MAX_PAGES = 64;
    public static final int VARIANT_COUNT = 8;

    public ItemSpellbook() {
        setMaxStackSize(1);
    }

    @Override
    public int numVariants() {
        return VARIANT_COUNT;
    }

    @Override
    public int getVariantValue(ItemStack stack) {
        return getVariant(stack);
    }

    @Override
    public void setVariantValue(ItemStack stack, int variant) {
        setVariant(stack, variant);
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
                        getPage(stack, 1), MAX_PAGES)));
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
                    data.getCastingStack(), data, player, hand);
                result = data.getCastingStack();
            }
            String resultText = result.isEmpty()
                ? I18n.translateToLocal("hexcasting.message.empty_stack")
                : result.peek().display();
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.scroll_result",
                    displayPattern(pattern), resultText)));
        } catch (CastingException exception) {
            MishapFeedback.send(player, exception);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.onUpdate(stack, world, entity, slot, selected);
        if (world == null || world.isRemote || !(entity instanceof EntityPlayer)
            || stack == null || stack.isEmpty()) {
            return;
        }
        migrateLegacyData(stack);
        int page = getPage(stack, 0);
        if (page <= 0) {
            return;
        }
        NBTTagCompound root = getOrCreateTag(stack);
        NBTTagCompound names = root.hasKey(TAG_PAGE_NAMES, 10)
            ? root.getCompoundTag(TAG_PAGE_NAMES) : new NBTTagCompound();
        String key = String.valueOf(page);
        if (stack.hasDisplayName()) {
            names.setString(key, stack.getDisplayName());
        } else {
            names.removeTag(key);
        }
        if (names.hasNoTags()) {
            root.removeTag(TAG_PAGE_NAMES);
        } else {
            root.setTag(TAG_PAGE_NAMES, names);
        }
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
        int pageCount = highestPage(stack);
        int active = getPage(stack, 0);
        tooltip.add(I18n.translateToLocalFormatted(
            "hexcasting.tooltip.staff_program", active, pageCount));
        Iota stored = readStoredIota(stack);
        if (stored instanceof PatternIota) {
            HexPattern pattern = ((PatternIota) stored).getPattern();
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.action") + ": "
                + displayPattern(pattern));
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.pattern") + ": "
                + HexInline.formatPattern(pattern, HexInline.DEFAULT_PATTERN_COLOR));
        } else if (stored != null) {
            tooltip.add(stored.getType().getId() + ": " + stored.display());
        }
    }

    /** Returns the registered action for the current page, if it has one. */
    public static ResourceLocation getActionId(ItemStack stack) {
        return getActionId(getPattern(stack));
    }

    /** Returns the complete pattern stored on the current page. */
    public static HexPattern getPattern(ItemStack stack) {
        Iota stored = readStoredIota(stack);
        return stored instanceof PatternIota
            ? ((PatternIota) stored).getPattern() : null;
    }

    public static boolean hasIota(ItemStack stack) {
        return stack != null && !stack.isEmpty()
            && stack.getItem() instanceof ItemSpellbook
            && ((ItemSpellbook) stack.getItem()).readIotaTag(stack) != null;
    }

    /** Read the current page as a generic Hex Iota, not only as a pattern. */
    @Override
    public NBTTagCompound readIotaTag(ItemStack stack) {
        migrateLegacyData(stack);
        NBTTagCompound pages = getIotaPages(stack);
        int page = getPage(stack, 1);
        if (page <= 0 || !pages.hasKey(String.valueOf(page), 10)) {
            return null;
        }
        NBTTagCompound stored = pages.getCompoundTag(String.valueOf(page));
        return stored.hasKey("type", 8) && stored.hasKey("data", 10)
            ? stored : null;
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
        migrateLegacyData(stack);
        NBTTagCompound root = getOrCreateTag(stack);
        NBTTagCompound pages = getIotaPages(stack);
        String pageKey = String.valueOf(getPage(stack, 1));
        if (datum == null) {
            pages.removeTag(pageKey);
            removeSealed(root, pageKey);
        } else {
            pages.setTag(pageKey, datum.serialize());
        }
        if (pages.hasNoTags()) {
            root.removeTag(TAG_PAGES);
        } else {
            root.setTag(TAG_PAGES, pages);
        }
    }

    /** Set the modern per-page sealed flag used by spellbook recipes. */
    public static void setSealed(ItemStack stack, boolean sealed) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        migrateLegacyData(stack);
        NBTTagCompound root = sealed ? getOrCreateTag(stack) : stack.getTagCompound();
        if (root == null) {
            return;
        }
        String pageKey = String.valueOf(getPage(stack, 1));
        NBTTagCompound sealedPages = root.hasKey(TAG_SEALED, 10)
            ? root.getCompoundTag(TAG_SEALED) : new NBTTagCompound();
        if (sealed) {
            sealedPages.setBoolean(pageKey, true);
        } else {
            sealedPages.removeTag(pageKey);
            root.removeTag(KEY_LEGACY_SEALED);
        }
        if (sealedPages.hasNoTags()) {
            root.removeTag(TAG_SEALED);
        } else {
            root.setTag(TAG_SEALED, sealedPages);
        }
    }

    public static boolean isSealed(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getTagCompound() == null) {
            return false;
        }
        migrateLegacyData(stack);
        NBTTagCompound root = stack.getTagCompound();
        if (root.getBoolean(KEY_LEGACY_SEALED)) {
            return true;
        }
        if (!root.hasKey(TAG_SEALED, 10)) {
            return false;
        }
        return root.getCompoundTag(TAG_SEALED)
            .getBoolean(String.valueOf(getPage(stack, 1)));
    }

    public static int getVariant(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null) {
            return 0;
        }
        return Math.max(0, Math.min(VARIANT_COUNT - 1, tag.getInteger(TAG_VARIANT)));
    }

    public static void setVariant(ItemStack stack, int variant) {
        if (stack == null || stack.isEmpty() || isSealed(stack)) {
            return;
        }
        NBTTagCompound tag = getOrCreateTag(stack);
        tag.setInteger(TAG_VARIANT,
            Math.max(0, Math.min(VARIANT_COUNT - 1, variant)));
    }

    public static void cycleVariant(ItemStack stack) {
        setVariant(stack, (getVariant(stack) + 1) % VARIANT_COUNT);
    }

    /** Number of the highest non-empty page, matching modern Hex's helper. */
    public static int highestPage(ItemStack stack) {
        migrateLegacyData(stack);
        return highestPageWithoutMigration(stack);
    }

    public static boolean arePagesEmpty(ItemStack stack) {
        return highestPage(stack) == 0;
    }

    /** Return the one-based active page, or {@code ifEmpty} for an empty book. */
    public static int getPage(ItemStack stack, int ifEmpty) {
        migrateLegacyData(stack);
        int highest = highestPageWithoutMigration(stack);
        if (highest <= 0) {
            return ifEmpty;
        }
        NBTTagCompound root = stack == null ? null : stack.getTagCompound();
        int page;
        if (root != null && root.hasKey(TAG_SELECTED_PAGE, 3)) {
            page = root.getInteger(TAG_SELECTED_PAGE);
        } else if (root != null && root.hasKey(KEY_ACTIVE, 3)) {
            page = root.getInteger(KEY_ACTIVE) + 1;
        } else {
            page = 1;
        }
        return Math.max(1, Math.min(highest, page));
    }

    /** Zero-based index used by the 1.12.2 GUI and item messages. */
    public static int getPageIndex(ItemStack stack) {
        int page = getPage(stack, 0);
        return page <= 0 ? 0 : page - 1;
    }

    /** Scroll the active page without wrapping past the first or last page. */
    public static int rotatePageIdx(ItemStack stack, boolean increase) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int page = getPage(stack, 0);
        int highest = highestPage(stack);
        if (page > 0 && highest > 0) {
            page = Math.max(1, Math.min(highest, page + (increase ? 1 : -1)));
        } else {
            page = 0;
        }
        getOrCreateTag(stack).setInteger(TAG_SELECTED_PAGE, page);
        restorePageName(stack, page);
        return page;
    }

    private static HexPattern cyclePage(ItemStack stack) {
        int pageCount = highestPage(stack);
        NBTTagCompound tag = getOrCreateTag(stack);
        if (pageCount == 0) {
            tag.setInteger(TAG_SELECTED_PAGE, 0);
            restorePageName(stack, 0);
            return null;
        }
        int next = getPage(stack, 1) % pageCount + 1;
        tag.setInteger(TAG_SELECTED_PAGE, next);
        restorePageName(stack, next);
        return getPattern(stack);
    }

    /** Store a scroll on the current page, appending when that page is occupied. */
    private static boolean writeActivePage(ItemStack stack, HexPattern pattern) {
        if (pattern == null || stack == null || stack.isEmpty()) {
            return false;
        }
        migrateLegacyData(stack);
        int page = getPage(stack, 0);
        if (page == 0) {
            page = 1;
        } else if (readIotaAtPage(stack, page) != null) {
            page = highestPage(stack) + 1;
        }
        if (page > MAX_PAGES) {
            return false;
        }
        NBTTagCompound root = getOrCreateTag(stack);
        root.setInteger(TAG_SELECTED_PAGE, page);
        if (isSealed(stack)) {
            return false;
        }
        ((ItemSpellbook) stack.getItem()).writeDatum(stack, new PatternIota(pattern));
        return readIotaAtPage(stack, page) != null;
    }

    private static ResourceLocation getActionId(HexPattern pattern) {
        if (pattern == null) {
            return null;
        }
        at.petra_k.hexcasting.api.casting.action.HexAction action =
            HexActionRegistry.get(pattern);
        return action == null ? null : HexActionRegistry.idFor(action);
    }

    private static Iota readStoredIota(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        try {
            NBTTagCompound tag = ((ItemSpellbook) stack.getItem()).readIotaTag(stack);
            return tag == null ? null : at.petra_k.hexcasting.common.lib.hex.HexIotaTypes.deserialize(tag);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Iota readIotaAtPage(ItemStack stack, int page) {
        if (page <= 0) {
            return null;
        }
        NBTTagCompound pages = getIotaPages(stack);
        NBTTagCompound tag = pages.getCompoundTag(String.valueOf(page));
        if (!tag.hasKey("type", 8) || !tag.hasKey("data", 10)) {
            return null;
        }
        try {
            return at.petra_k.hexcasting.common.lib.hex.HexIotaTypes.deserialize(tag);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** Convert both the original port's layout and action lists to modern pages. */
    private static void migrateLegacyData(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getTagCompound() == null) {
            return;
        }
        NBTTagCompound root = stack.getTagCompound();
        NBTTagCompound pages = root.hasKey(TAG_PAGES, 10)
            ? root.getCompoundTag(TAG_PAGES) : new NBTTagCompound();

        if (root.hasKey(KEY_IOTA_PAGES, 10)) {
            NBTTagCompound old = root.getCompoundTag(KEY_IOTA_PAGES);
            copyPageCompounds(old, pages);
        }
        if (root.hasKey(KEY_PATTERN_PAGES, 9)) {
            NBTTagList old = root.getTagList(KEY_PATTERN_PAGES, 10);
            for (int i = 0; i < Math.min(MAX_PAGES, old.tagCount()); i++) {
                String key = String.valueOf(i + 1);
                if (!pages.hasKey(key, 10)) {
                    try {
                        pages.setTag(key, new PatternIota(
                            HexPattern.fromNBT(old.getCompoundTagAt(i))).serialize());
                    } catch (RuntimeException ignored) {
                        // Keep valid pages when one old pattern is malformed.
                    }
                }
            }
        }
        // The first port used a string list under "pages".  A compound and a
        // list have different NBT types, so both layouts can be detected safely.
        if (root.hasKey(TAG_PAGES, 9)) {
            NBTTagList old = root.getTagList(TAG_PAGES, 8);
            HexActionRegistry.bootstrap();
            for (int i = 0; i < Math.min(MAX_PAGES, old.tagCount()); i++) {
                String key = String.valueOf(i + 1);
                if (pages.hasKey(key, 10)) {
                    continue;
                }
                try {
                    ResourceLocation id = new ResourceLocation(old.getStringTagAt(i));
                    HexPattern pattern = HexActionRegistry.getPattern(id);
                    if (pattern != null) {
                        pages.setTag(key, new PatternIota(pattern).serialize());
                    }
                } catch (RuntimeException ignored) {
                    // Ignore only malformed legacy action ids.
                }
            }
        }
        if (!pages.hasNoTags()) {
            root.setTag(TAG_PAGES, pages);
        }
        if (!root.hasKey(TAG_SELECTED_PAGE, 3) && root.hasKey(KEY_ACTIVE, 3)) {
            root.setInteger(TAG_SELECTED_PAGE,
                pages.hasNoTags() ? 0 : Math.max(1,
                    Math.min(MAX_PAGES, root.getInteger(KEY_ACTIVE) + 1)));
        }
    }

    private static void copyPageCompounds(NBTTagCompound source, NBTTagCompound target) {
        for (String key : source.getKeySet()) {
            try {
                int page = Integer.parseInt(key);
                if (page >= 1 && page <= MAX_PAGES && !target.hasKey(key, 10)) {
                    NBTTagCompound value = source.getCompoundTag(key);
                    if (value.hasKey("type", 8) && value.hasKey("data", 10)) {
                        target.setTag(key, value);
                    }
                }
            } catch (NumberFormatException ignored) {
                // Ignore non-page metadata.
            }
        }
    }

    private static NBTTagCompound getIotaPages(ItemStack stack) {
        migrateLegacyData(stack);
        if (stack == null || stack.isEmpty() || stack.getTagCompound() == null) {
            return new NBTTagCompound();
        }
        NBTTagCompound root = stack.getTagCompound();
        return root.hasKey(TAG_PAGES, 10)
            ? root.getCompoundTag(TAG_PAGES) : new NBTTagCompound();
    }

    private static int highestPageWithoutMigration(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getTagCompound() == null) {
            return 0;
        }
        NBTTagCompound root = stack.getTagCompound();
        if (!root.hasKey(TAG_PAGES, 10)) {
            return 0;
        }
        int highest = 0;
        for (String key : root.getCompoundTag(TAG_PAGES).getKeySet()) {
            try {
                highest = Math.max(highest, Integer.parseInt(key));
            } catch (NumberFormatException ignored) {
                // Ignore non-page metadata.
            }
        }
        return Math.min(MAX_PAGES, highest);
    }

    private static void removeSealed(NBTTagCompound root, String pageKey) {
        if (!root.hasKey(TAG_SEALED, 10)) {
            return;
        }
        NBTTagCompound sealed = root.getCompoundTag(TAG_SEALED);
        sealed.removeTag(pageKey);
        if (sealed.hasNoTags()) {
            root.removeTag(TAG_SEALED);
        } else {
            root.setTag(TAG_SEALED, sealed);
        }
    }

    private static void restorePageName(ItemStack stack, int page) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NBTTagCompound root = stack.getTagCompound();
        String name = null;
        if (page > 0 && root != null && root.hasKey(TAG_PAGE_NAMES, 10)) {
            NBTTagCompound names = root.getCompoundTag(TAG_PAGE_NAMES);
            if (names.hasKey(String.valueOf(page), 8)) {
                name = names.getString(String.valueOf(page));
            }
        }
        if (name == null || name.isEmpty()) {
            if (stack.hasDisplayName()) {
                stack.clearCustomName();
            }
        } else {
            stack.setStackDisplayName(name);
        }
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
