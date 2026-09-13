package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.item.IotaHolderItem;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import at.petra_k.hexcasting.common.casting.HexEvaluator;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;
import net.minecraft.util.text.translation.I18n;
import at.petra_k.hexcasting.interop.inline.HexInline;

/** A portable, NBT-backed spell pattern for the 1.12.2 port. */
public final class ItemPatternScroll extends Item implements IotaHolderItem {
    private final int blockSize;
    private static final String KEY_ACTION = "action";
    private static final String KEY_PATTERN = "pattern";

    public ItemPatternScroll() {
        this(1);
    }

    public ItemPatternScroll(int maxPatterns) {
        this.blockSize = Math.max(1, Math.min(3, maxPatterns));
        setMaxStackSize(1);
    }

    public int getBlockSize() {
        return blockSize;
    }

    @Override
    public NBTTagCompound readIotaTag(ItemStack stack) {
        HexPattern pattern = getPattern(stack);
        if (pattern == null) {
            return null;
        }
        return new PatternIota(pattern).serialize();
    }

    @Override
    public boolean writeable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canWrite(ItemStack stack, Iota datum) {
        return datum == null || datum instanceof PatternIota;
    }

    @Override
    public void writeDatum(ItemStack stack, Iota datum) {
        if (!canWrite(stack, datum)) {
            return;
        }
        if (datum == null) {
            setPattern(stack, null);
            return;
        }
        PatternIota pattern = (PatternIota) datum;
        setPattern(stack, pattern.getPattern());
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            HexActionRegistry.bootstrap();
            ResourceLocation current = getActionId(stack);
            HexPattern currentPattern = getPattern(stack);
            if (!player.isSneaking() && ItemHexStaff.isStaff(player.getHeldItemOffhand())) {
                if (currentPattern == null) {
                    player.sendMessage(new TextComponentString(
                        I18n.translateToLocal("hexcasting.tooltip.scroll.empty")));
                    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
                }
                boolean added = ItemHexStaff.appendPattern(
                    player.getHeldItemOffhand(), currentPattern, 0, 0);
                if (added) {
                    consumeForWrite(stack, player);
                    player.sendMessage(new TextComponentString(
                        I18n.translateToLocalFormatted(
                            "hexcasting.message.program_added",
                            current == null ? HexInline.formatPattern(currentPattern) : localizeAction(current),
                            ItemHexStaff.getProgramSize(player.getHeldItemOffhand()),
                            ItemHexStaff.MAX_PROGRAM_SIZE)));
                } else {
                    player.sendMessage(new TextComponentString(
                        I18n.translateToLocalFormatted(
                            "hexcasting.message.program_full",
                            ItemHexStaff.MAX_PROGRAM_SIZE)));
                }
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
            if (!player.isSneaking()
                && !player.getHeldItemOffhand().isEmpty()
                && player.getHeldItemOffhand().getItem() instanceof ItemHexFocus) {
                if (currentPattern == null) {
                    player.sendMessage(new TextComponentString(
                        I18n.translateToLocal("hexcasting.tooltip.scroll.empty")));
                    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
                }
                ItemHexFocus focus = (ItemHexFocus) player.getHeldItemOffhand().getItem();
                focus.writeDatum(player.getHeldItemOffhand(), new PatternIota(currentPattern));
                consumeForWrite(stack, player);
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted(
                        "hexcasting.message.focus_written",
                        HexInline.formatPattern(currentPattern))));
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
            if (player.isSneaking()) {
                setPattern(stack, null);
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocal("hexcasting.message.scroll_cleared")));
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }

            if (currentPattern == null) {
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocal("hexcasting.tooltip.scroll.empty")));
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }

            try {
                HexPattern pattern = currentPattern;
                IHexCastingData data = HexCapabilities.CASTING_DATA == null
                    ? null
                    : player.getCapability(HexCapabilities.CASTING_DATA, null);
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
                    I18n.translateToLocalFormatted(
                        "hexcasting.message.scroll_result",
                        current == null ? HexInline.formatPattern(pattern) : current.getResourcePath(),
                        resultText
                    )
                ));
            } catch (CastingException exception) {
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted(
                        "hexcasting.message.scroll_error",
                        exception.getMessage()
                    )
                ));
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (tab == getCreativeTab()) {
            // Keep one ordinary scroll in the creative inventory. Action-bearing
            // scrolls remain valid NBT stacks supplied by recipes or commands.
            items.add(new ItemStack(this));
        }
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        HexActionRegistry.bootstrap();
        ResourceLocation id = getActionId(stack);
        HexPattern pattern = getPattern(stack);
        if (pattern == null) {
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.scroll.empty"));
            return;
        }
        if (id != null) {
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.action") + ": " + localizeAction(id));
        }
        tooltip.add(I18n.translateToLocal("hexcasting.tooltip.pattern") + ": " + HexInline.formatPattern(pattern));
        tooltip.add(I18n.translateToLocal("hexcasting.tooltip.cycle"));
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        ResourceLocation id = getActionId(stack);
        if (id == null) {
            return super.getItemStackDisplayName(stack);
        }
        return I18n.translateToLocalFormatted(
            "hexcasting.item.pattern_scroll.variant",
            super.getItemStackDisplayName(stack), localizeAction(id));
    }

    public static ResourceLocation getActionId(ItemStack stack) {
        HexActionRegistry.bootstrap();
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(KEY_ACTION, 8)) {
            try {
                ResourceLocation stored = new ResourceLocation(tag.getString(KEY_ACTION));
                if (HexActionRegistry.get(stored) != null) {
                    return stored;
                }
            } catch (RuntimeException ignored) {
                // Invalid or stale NBT falls back to a registered action.
            }
        }
        HexPattern stored = getStoredPattern(stack);
        if (stored != null) {
            try {
                at.petra_k.hexcasting.api.casting.action.HexAction action = HexActionRegistry.get(stored);
                return HexActionRegistry.idFor(action);
            } catch (RuntimeException ignored) {
                // A custom pattern may not have a registered action id.
            }
        }
        return null;
    }

    private static HexPattern getStoredPattern(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.getTagCompound() != null) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag.hasKey(KEY_PATTERN, 10)) {
                NBTTagCompound patternTag = tag.getCompoundTag(KEY_PATTERN);
                if (patternTag.hasKey(HexPattern.TAG_START_DIR, 1)
                    && patternTag.hasKey(HexPattern.TAG_ANGLES, 7)) {
                    try {
                        return HexPattern.fromNBT(patternTag);
                    } catch (RuntimeException ignored) {
                        // Fall through to the legacy action representation.
                    }
                }
            }
        }
        return null;
    }

    /** Return the exact pattern stored on this scroll, with legacy action fallback. */
    public static HexPattern getPattern(ItemStack stack) {
        HexPattern stored = getStoredPattern(stack);
        if (stored != null) {
            return stored;
        }
        ResourceLocation action = getActionId(stack);
        return action == null ? null : HexActionRegistry.getPattern(action);
    }

    public static boolean hasPattern(ItemStack stack) {
        return getPattern(stack) != null;
    }

    /** Store or clear the complete drawable pattern on a scroll. */
    public static void setPattern(ItemStack stack, HexPattern pattern) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (pattern == null) {
            if (tag != null) {
                tag.removeTag(KEY_PATTERN);
                tag.removeTag(KEY_ACTION);
            }
            return;
        }
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setTag(KEY_PATTERN, pattern.serializeToNBT());
        tag.removeTag(KEY_ACTION);
    }

    /** Consumes a written scroll unless the player is in creative mode. */
    public static boolean consumeForWrite(ItemStack stack, EntityPlayer player) {
        if (stack == null || stack.isEmpty() || player == null || player.capabilities.isCreativeMode) {
            return false;
        }
        stack.shrink(1);
        return true;
    }

    private static String localizeAction(ResourceLocation id) {
        if (id == null) {
            return I18n.translateToLocal("hexcasting.tooltip.scroll.empty");
        }
        String key = "hexcasting.action." + id.getResourcePath();
        String translated = I18n.translateToLocal(key);
        return key.equals(translated) ? id.getResourcePath() : translated;
    }

}
