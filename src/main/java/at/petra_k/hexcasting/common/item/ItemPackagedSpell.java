package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import at.petra_k.hexcasting.api.item.MediaHolderItem;
import at.petra_k.hexcasting.common.capability.HexItemMediaHolder;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import at.petra_k.hexcasting.common.effect.HexCastingEffects;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.Mishap;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.casting.MishapFeedback;
import at.petra_k.hexcasting.interop.inline.HexInline;
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
import java.util.List;
import java.util.UUID;

/** A single-use or reusable packaged spell container for the 1.12.2 port. */
public class ItemPackagedSpell extends Item implements MediaHolderItem {
    /** Modern Hex stores executable Iotas in this list. */
    public static final String TAG_PROGRAM = "patterns";
    public static final String TAG_PIGMENT = "pigment";
    public static final String TAG_MEDIA = "hexcasting:media";
    public static final String TAG_MAX_MEDIA = "hexcasting:start_media";

    private static final String LEGACY_MEDIA = "media";
    private static final String LEGACY_MAX_MEDIA = "max_media";
    private static final String KEY_IOTA = "hexcasting_iota";
    private static final String KEY_PACKAGED_ACTION = "packaged_action";
    private static final String KEY_PATTERN_PROGRAM = "pattern_program";
    private static final String KEY_VARIANT = "variant";
    private static final int VARIANT_COUNT = 5;

    /** Returns the action stored in this packaged spell, or null for an empty item. */
    public static ResourceLocation getPackagedAction(ItemStack stack) {
        List<ResourceLocation> actions = getPackagedActions(stack);
        return actions.isEmpty() ? null : actions.get(0);
    }

    /** Reads the current program, while accepting the old single-action NBT format. */
    public static List<ResourceLocation> getPackagedActions(ItemStack stack) {
        List<ResourceLocation> actions = new ArrayList<>();
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null) {
            return actions;
        }

        if (tag.hasKey(TAG_PROGRAM, 9)) {
            NBTTagList list = tag.getTagList(TAG_PROGRAM, 8);
            for (int i = 0; i < list.tagCount(); i++) {
                addValidAction(actions, list.getStringTagAt(i));
            }
        }
        if (actions.isEmpty() && tag.hasKey(KEY_PACKAGED_ACTION, 8)) {
            addValidAction(actions, tag.getString(KEY_PACKAGED_ACTION));
        }
        if (actions.isEmpty()) {
            HexActionRegistry.bootstrap();
            for (Iota iota : readStoredProgram(stack)) {
                if (!(iota instanceof PatternIota)) {
                    continue;
                }
                at.petra_k.hexcasting.api.casting.action.HexAction action =
                    HexActionRegistry.get(((PatternIota) iota).getPattern());
                ResourceLocation id = HexActionRegistry.idFor(action);
                if (id != null) {
                    actions.add(id);
                }
            }
        }
        return actions;
    }

    /** Read the complete executable program, including non-pattern Iotas. */
    public static List<Iota> getPackagedIotas(ItemStack stack) {
        List<Iota> stored = readStoredProgram(stack);
        if (!stored.isEmpty()) {
            return stored;
        }
        List<Iota> result = new ArrayList<>();
        for (ResourceLocation action : getPackagedActions(stack)) {
            HexPattern pattern = HexActionRegistry.getPattern(action);
            if (pattern != null) {
                result.add(new PatternIota(pattern));
            }
        }
        return result;
    }

    /** Reads exact drawable patterns, falling back to legacy registered actions. */
    public static List<HexPattern> getPackagedPatterns(ItemStack stack) {
        List<HexPattern> patterns = new ArrayList<>();
        for (Iota iota : getPackagedIotas(stack)) {
            if (iota instanceof PatternIota) {
                patterns.add(((PatternIota) iota).getPattern());
            }
        }
        return patterns;
    }

    private static List<Iota> readStoredProgram(ItemStack stack) {
        List<Iota> result = new ArrayList<>();
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null) {
            return result;
        }

        if (tag.hasKey(KEY_IOTA, 10)) {
            addSerializedIota(result, tag.getCompoundTag(KEY_IOTA));
            if (!result.isEmpty()) {
                return result;
            }
        }

        if (tag.hasKey(TAG_PROGRAM, 9)) {
            NBTTagList list = tag.getTagList(TAG_PROGRAM, 10);
            for (int i = 0; i < list.tagCount(); i++) {
                addSerializedIota(result, list.getCompoundTagAt(i));
            }
            if (!result.isEmpty()) {
                return result;
            }
        }

        if (tag.hasKey(KEY_PATTERN_PROGRAM, 9)) {
            NBTTagList list = tag.getTagList(KEY_PATTERN_PROGRAM, 10);
            for (int i = 0; i < list.tagCount(); i++) {
                try {
                    result.add(new PatternIota(HexPattern.fromNBT(list.getCompoundTagAt(i))));
                } catch (RuntimeException ignored) {
                    // Preserve valid entries when one old entry is malformed.
                }
            }
        }
        return result;
    }

    private static void addSerializedIota(List<Iota> result, NBTTagCompound serialized) {
        if (serialized == null || serialized.hasNoTags()) {
            return;
        }
        try {
            if (serialized.hasKey("type", 8)) {
                result.add(HexIotaTypes.deserialize(serialized));
            } else if (serialized.hasKey(HexPattern.TAG_START_DIR, 1)
                && serialized.hasKey(HexPattern.TAG_ANGLES, 7)) {
                // The first port stored bare HexPattern compounds here.
                result.add(new PatternIota(HexPattern.fromNBT(serialized)));
            }
        } catch (RuntimeException ignored) {
            // A damaged entry must not make an otherwise usable package crash.
        }
    }

    private static void addValidAction(List<ResourceLocation> actions, String rawId) {
        try {
            ResourceLocation id = new ResourceLocation(rawId);
            if (HexActionRegistry.getPattern(id) != null) {
                actions.add(id);
            }
        } catch (RuntimeException ignored) {
            // Ignore malformed entries so one damaged stack does not crash item rendering.
        }
    }

    public static int getVariant(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null) {
            return 0;
        }
        return Math.max(0, Math.min(VARIANT_COUNT - 1, tag.getInteger(KEY_VARIANT)));
    }

    public static void setVariant(ItemStack stack, int variant) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        int normalized = Math.max(0, Math.min(VARIANT_COUNT - 1, variant));
        NBTTagCompound tag = stack.getTagCompound();
        if (normalized == 0) {
            if (tag != null) {
                tag.removeTag(KEY_VARIANT);
            }
            return;
        }
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(KEY_VARIANT, normalized);
    }

    public static void cycleVariant(ItemStack stack) {
        setVariant(stack, (getVariant(stack) + 1) % VARIANT_COUNT);
    }

    @Override
    public long getMaxMedia(ItemStack stack) {
        long maximum = readMediaTag(stack, TAG_MAX_MEDIA, LEGACY_MAX_MEDIA);
        if (maximum <= 0L) {
            maximum = readMediaTag(stack, TAG_MEDIA, LEGACY_MEDIA);
        }
        return Math.max(0L, maximum);
    }

    @Override
    public long getMedia(ItemStack stack) {
        return Math.max(0L, Math.min(getMaxMedia(stack),
            readMediaTag(stack, TAG_MEDIA, LEGACY_MEDIA)));
    }

    @Override
    public void setMedia(ItemStack stack, long media) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        long maximum = getMaxMedia(stack);
        long clamped = Math.max(0L, Math.min(maximum, media));
        NBTTagCompound tag = getOrCreateTag(stack);
        tag.setLong(TAG_MEDIA, clamped);
        tag.setLong(TAG_MAX_MEDIA, maximum);
    }

    @Override
    public boolean canRecharge(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canProvide(ItemStack stack) {
        // Packaged media is consumed only by the package's own VM, never by
        // the ordinary inventory media scan.
        return false;
    }

    @Override
    public boolean canConstructBattery(ItemStack stack) {
        return false;
    }

    @Override
    public int getConsumptionPriority(ItemStack stack) {
        return 0;
    }

    /** Whether this package may fall back to the caster's media inventory. */
    protected boolean canDrawMediaFromInventory() {
        return false;
    }

    /** Cyphers override this to disappear once their stored media is spent. */
    protected boolean breakAfterDepletion() {
        return false;
    }

    /** Modern defaults are zero; concrete packaged items provide their values. */
    protected int cooldown() {
        return 0;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        HexActionRegistry.bootstrap();
        if (player.getCooldownTracker().hasCooldown(this)) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if (player.isSneaking()) {
            clearPackagedAction(stack);
            player.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.program_cleared")));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        ItemStack offhand = player.getHeldItemOffhand();
        if (!offhand.isEmpty() && offhand.getItem() instanceof ItemPatternScroll) {
            HexPattern pattern = ItemPatternScroll.getPattern(offhand);
            if (pattern == null) {
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocal("hexcasting.tooltip.scroll.empty")));
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
            appendPackagedPattern(stack, pattern);
            ItemPatternScroll.consumeForWrite(offhand, player);
            int count = getPackagedPatterns(stack).size();
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.program_added",
                    HexInline.formatPattern(pattern), count, count)));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        List<Iota> program = getPackagedIotas(stack);
        if (program.isEmpty()) {
            player.sendMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.program_empty")));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        CastingStack result = new CastingStack();
        CastingVM vm = new CastingVM(result);
        vm.setPlayer(player);
        vm.setCastingHand(hand);
        vm.setCastingData(HexCapabilities.CASTING_DATA == null
            ? null : player.getCapability(HexCapabilities.CASTING_DATA, null));
        // The package is always the first source.  Keeping an empty holder
        // bound is significant for Cyphers and Trinkets: once their stored
        // media is exhausted they must fail, rather than accidentally
        // consuming the caster's inventory.  Artifacts explicitly opt into
        // the fallback list after their own media is spent.
        vm.setMediaHolder(new HexItemMediaHolder(this, stack),
            canDrawMediaFromInventory());

        List<HexPattern> visualPatterns = new ArrayList<>();
        for (Iota iota : program) {
            if (iota instanceof PatternIota) {
                visualPatterns.add(((PatternIota) iota).getPattern());
            }
        }
        try {
            vm.enqueueIotas(program);
            vm.run(CastingVM.DEFAULT_MAX_OPERATIONS);
            String resultText = result.isEmpty()
                ? I18n.translateToLocal("hexcasting.message.empty_stack")
                : result.peek().display();
            player.sendMessage(new TextComponentString(
                I18n.translateToLocalFormatted("hexcasting.message.program_result", resultText)));
            HexCastingEffects.onPortableCast(player, hand, visualPatterns, true,
                vm.getSound() == null ? null : vm.getSound().getSound());
            player.getCooldownTracker().setCooldown(this, Math.max(0, cooldown()));
            if (breakAfterDepletion() && getMedia(stack) <= 0L
                && !player.capabilities.isCreativeMode) {
                stack.shrink(1);
                player.renderBrokenItemStack(stack);
            }
        } catch (CastingException exception) {
            Mishap mishap = MishapFeedback.asMishap(exception,
                visualPatterns.isEmpty() ? null : visualPatterns.get(visualPatterns.size() - 1),
                null, player, vm.getParenDepth(), vm.getOperationsConsumed());
            MishapFeedback.send(player, mishap);
            HexCastingEffects.onPortableCast(player, hand, visualPatterns, false,
                null, mishap);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        HexActionRegistry.bootstrap();
        List<ResourceLocation> actions = getPackagedActions(stack);
        List<Iota> program = getPackagedIotas(stack);
        List<HexPattern> patterns = getPackagedPatterns(stack);
        MediaTooltip.add(tooltip, getMedia(stack), getMaxMedia(stack));
        if (patterns.isEmpty()) {
            if (program.isEmpty()) {
                tooltip.add(I18n.translateToLocal("hexcasting.tooltip.none"));
            } else {
                tooltip.add(I18n.translateToLocalFormatted(
                    "hexcasting.tooltip.staff_program", program.size(), program.size()));
            }
            return;
        }
        if (actions.isEmpty()) {
            for (HexPattern pattern : patterns) {
                tooltip.add(I18n.translateToLocal("hexcasting.tooltip.pattern") + ": "
                    + HexInline.formatPattern(pattern, HexInline.DEFAULT_PATTERN_COLOR));
            }
            return;
        }
        for (ResourceLocation action : actions) {
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.action") + ": " + localizeAction(action));
            HexPattern pattern = HexActionRegistry.getPattern(action);
            if (pattern != null) {
                tooltip.add(I18n.translateToLocal("hexcasting.tooltip.pattern") + ": "
                    + HexInline.formatPattern(pattern, HexInline.DEFAULT_PATTERN_COLOR));
            }
        }
    }

    /** Cyphers override this because they are consumed after a successful cast. */
    protected boolean consumeOnUse() {
        return false;
    }

    /** Write a complete executable program and its captured media to a stack. */
    public static void writePackagedProgram(ItemStack stack, List<? extends Iota> program,
                                            long media) {
        if (stack == null || stack.isEmpty() || program == null || program.isEmpty()) {
            return;
        }
        NBTTagCompound tag = getOrCreateTag(stack);
        NBTTagList serialized = new NBTTagList();
        for (Iota iota : program) {
            if (iota != null) {
                serialized.appendTag(iota.serialize());
            }
        }
        tag.setTag(TAG_PROGRAM, serialized);
        tag.removeTag(KEY_IOTA);
        tag.removeTag(KEY_PATTERN_PROGRAM);
        tag.removeTag(KEY_PACKAGED_ACTION);
        long captured = Math.max(0L, media);
        tag.setLong(TAG_MAX_MEDIA, captured);
        tag.setLong(TAG_MEDIA, captured);
    }

    /** Store the pigment snapshot used by a crafted package. */
    public static void setPigment(ItemStack stack, int color, String variant, UUID owner) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NBTTagCompound pigment = new NBTTagCompound();
        pigment.setInteger("color", color & 0xFFFFFF);
        pigment.setString("variant", variant == null ? "default_colorizer" : variant);
        pigment.setString("owner", owner == null ? new UUID(0L, 0L).toString() : owner.toString());
        getOrCreateTag(stack).setTag(TAG_PIGMENT, pigment);
    }

    public static int getPigmentColor(ItemStack stack, int fallback) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_PIGMENT, 10)) {
            return fallback & 0xFFFFFF;
        }
        NBTTagCompound pigment = tag.getCompoundTag(TAG_PIGMENT);
        return pigment.hasKey("color", 3) ? pigment.getInteger("color") & 0xFFFFFF
            : fallback & 0xFFFFFF;
    }

    public static void setPackagedAction(ItemStack stack, ResourceLocation action) {
        if (stack == null || stack.isEmpty() || action == null
            || HexActionRegistry.getPattern(action) == null) {
            return;
        }
        clearPackagedAction(stack);
        appendPackagedAction(stack, action);
    }

    public static void appendPackagedAction(ItemStack stack, ResourceLocation action) {
        if (stack == null || stack.isEmpty() || action == null
            || HexActionRegistry.getPattern(action) == null) {
            return;
        }
        if (stack.getTagCompound() != null
            && (stack.getTagCompound().hasKey(KEY_PATTERN_PROGRAM, 9)
                || stack.getTagCompound().hasKey(KEY_IOTA, 10)
                || stack.getTagCompound().hasKey(TAG_PROGRAM, 9)
                    && stack.getTagCompound().getTagList(TAG_PROGRAM, 10).tagCount() > 0)) {
            appendPackagedPattern(stack, HexActionRegistry.getPattern(action));
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        NBTTagList patterns = tag.hasKey(TAG_PROGRAM, 9)
            ? tag.getTagList(TAG_PROGRAM, 8) : new NBTTagList();
        patterns.appendTag(new NBTTagString(action.toString()));
        tag.setTag(TAG_PROGRAM, patterns);
        tag.removeTag(KEY_PACKAGED_ACTION);
    }

    /** Replace the packaged program with one exact drawable pattern. */
    public static void setPackagedPattern(ItemStack stack, HexPattern pattern) {
        clearPackagedAction(stack);
        appendPackagedPattern(stack, pattern);
    }

    /** Append an exact pattern, converting any legacy action list first. */
    public static void appendPackagedPattern(ItemStack stack, HexPattern pattern) {
        if (stack == null || stack.isEmpty() || pattern == null) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        NBTTagList patterns;
        if (tag.hasKey(KEY_IOTA, 10)
            || tag.hasKey(TAG_PROGRAM, 9)
                && tag.getTagList(TAG_PROGRAM, 10).tagCount() > 0) {
            patterns = new NBTTagList();
            for (Iota iota : getPackagedIotas(stack)) {
                patterns.appendTag(iota.serialize());
            }
            patterns.appendTag(new PatternIota(pattern).serialize());
            tag.setTag(TAG_PROGRAM, patterns);
            tag.removeTag(KEY_IOTA);
            tag.removeTag(KEY_PATTERN_PROGRAM);
            tag.removeTag(KEY_PACKAGED_ACTION);
            return;
        } else if (tag.hasKey(KEY_PATTERN_PROGRAM, 9)) {
            patterns = tag.getTagList(KEY_PATTERN_PROGRAM, 10);
        } else {
            patterns = new NBTTagList();
            for (ResourceLocation action : getPackagedActions(stack)) {
                HexPattern oldPattern = HexActionRegistry.getPattern(action);
                if (oldPattern != null) {
                    patterns.appendTag(oldPattern.serializeToNBT());
                }
            }
        }
        patterns.appendTag(pattern.serializeToNBT());
        tag.setTag(KEY_PATTERN_PROGRAM, patterns);
        tag.removeTag(TAG_PROGRAM);
        tag.removeTag(KEY_PACKAGED_ACTION);
    }

    public static void clearPackagedAction(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.getTagCompound() != null) {
            stack.getTagCompound().removeTag(KEY_PACKAGED_ACTION);
            stack.getTagCompound().removeTag(TAG_PROGRAM);
            stack.getTagCompound().removeTag(KEY_PATTERN_PROGRAM);
            stack.getTagCompound().removeTag(KEY_IOTA);
            stack.getTagCompound().removeTag(TAG_PIGMENT);
            stack.getTagCompound().removeTag(TAG_MEDIA);
            stack.getTagCompound().removeTag(TAG_MAX_MEDIA);
            stack.getTagCompound().removeTag(LEGACY_MEDIA);
            stack.getTagCompound().removeTag(LEGACY_MAX_MEDIA);
        }
    }

    private static long readMediaTag(ItemStack stack, String primary, String legacy) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null) {
            return 0L;
        }
        for (String key : new String[] {primary, legacy}) {
            if (tag.hasKey(key, 4)) {
                return Math.max(0L, tag.getLong(key));
            }
            if (tag.hasKey(key, 3)) {
                return Math.max(0L, tag.getInteger(key));
            }
        }
        return 0L;
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }

    private static String localizeAction(ResourceLocation id) {
        String key = "hexcasting.action." + id.getResourcePath();
        String translated = I18n.translateToLocal(key);
        return key.equals(translated) ? id.getResourcePath() : translated;
    }

    public ItemPackagedSpell() {
        setMaxStackSize(1);
    }
}
