package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.PatternIota;
import at.petra_k.hexcasting.api.item.IotaHolderItem;
import at.petra_k.hexcasting.common.casting.HexEvaluator;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.common.lib.hex.HexActions;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;
import net.minecraft.util.text.translation.I18n;
import at.petra_k.hexcasting.interop.inline.HexInline;

/** Action-selectable portable casting item for the 1.12.2 port. */
public final class ItemHexFocus extends Item implements IotaHolderItem {
    private static final String KEY_SELECTED_ACTION = "selected_action";
    private static final String KEY_SEALED = "sealed";
    private static final String KEY_VARIANT = "variant";

    public ItemHexFocus() {
        setMaxStackSize(1);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String key = isSealed(stack)
            ? "item.hexcasting.focus.sealed.name"
            : "item.hexcasting.focus.name";
        String translated = I18n.translateToLocal(key);
        return key.equals(translated) ? super.getItemStackDisplayName(stack) : translated;
    }

    @Override
    public NBTTagCompound readIotaTag(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        return tag != null && tag.hasKey(TAG_DATA, 10)
            ? tag.getCompoundTag(TAG_DATA) : null;
    }

    @Override
    public boolean writeable(ItemStack stack) {
        return !isSealed(stack);
    }

    @Override
    public boolean canWrite(ItemStack stack, Iota iota) {
        return iota == null || !isSealed(stack);
    }

    @Override
    public void writeDatum(ItemStack stack, Iota iota) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (iota == null) {
            if (tag != null) {
                tag.removeTag(TAG_DATA);
                tag.removeTag(KEY_SELECTED_ACTION);
                tag.removeTag(KEY_SEALED);
            }
            return;
        }
        if (isSealed(stack)) {
            return;
        }
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setTag(TAG_DATA, iota.serialize());
    }

    public static boolean isSealed(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        return tag != null && tag.getBoolean(KEY_SEALED);
    }

    public static void seal(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setBoolean(KEY_SEALED, true);
    }

    public static int getVariant(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        return tag == null ? 0 : Math.max(0, Math.min(7, tag.getInteger(KEY_VARIANT)));
    }

    public static void setVariant(ItemStack stack, int variant) {
        if (stack == null || stack.isEmpty() || isSealed(stack)) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(KEY_VARIANT, Math.max(0, Math.min(7, variant)));
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, net.minecraft.client.util.ITooltipFlag flag) {
        HexActionRegistry.bootstrap();
        ResourceLocation selected = resolveSelectedAction(stack);
        HexPattern selectedPattern = selected == null ? null : HexActionRegistry.getPattern(selected);
        String actionText = selected == null
            ? I18n.translateToLocal("hexcasting.tooltip.none")
            : localizeAction(selected);
        String patternText = selectedPattern == null
            ? I18n.translateToLocal("hexcasting.tooltip.none")
            : HexInline.formatPattern(selectedPattern);
        tooltip.add(I18n.translateToLocal("hexcasting.tooltip.pattern") + ": "
            + actionText + " " + patternText);
        tooltip.add(I18n.translateToLocal("hexcasting.tooltip.cycle"));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (!world.isRemote) {
            HexActionRegistry.bootstrap();
            ResourceLocation selected = resolveSelectedAction(held);
            ItemStack offhand = player.getHeldItemOffhand();
            if (!player.isSneaking() && !offhand.isEmpty()
                && offhand.getItem() instanceof ItemPatternScroll) {
                HexPattern pattern = ItemPatternScroll.getPattern(offhand);
                if (pattern == null) {
                    player.sendMessage(new TextComponentString(
                        I18n.translateToLocal("hexcasting.tooltip.scroll.empty")));
                    return new ActionResult<>(EnumActionResult.SUCCESS, held);
                }
                writeDatum(held, new PatternIota(pattern));
                ItemPatternScroll.consumeForWrite(offhand, player);
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted(
                        "hexcasting.message.focus_written",
                        HexInline.formatPattern(pattern))));
                return new ActionResult<>(EnumActionResult.SUCCESS, held);
            }
            if (player.isSneaking()) {
                ResourceLocation next = HexActionRegistry.nextId(selected);
                if (next != null) {
                    setSelectedAction(held, next);
                    player.sendMessage(new TextComponentString(
                        I18n.translateToLocalFormatted(
                            "hexcasting.message.pattern_selected",
                            next.getResourcePath()
                        )
                    ));
                }
                return new ActionResult<>(EnumActionResult.SUCCESS, held);
            }

            try {
                HexPattern pattern = HexActionRegistry.getPattern(selected);
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
                        "hexcasting.message.result",
                        selected.getResourcePath(),
                        resultText
                    )
                ));
            } catch (CastingException exception) {
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted(
                        "hexcasting.message.error",
                        exception.getMessage()
                    )
                ));
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, held);
    }

    private static ResourceLocation resolveSelectedAction(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof ItemHexFocus) {
            try {
                Iota stored = ((ItemHexFocus) stack.getItem()).readIota(stack);
                if (stored instanceof PatternIota) {
                    at.petra_k.hexcasting.api.casting.action.HexAction action =
                        HexActionRegistry.get(((PatternIota) stored).getPattern());
                    ResourceLocation id = HexActionRegistry.idFor(action);
                    if (id != null) {
                        return id;
                    }
                }
            } catch (CastingException ignored) {
                // Fall back to legacy selected_action data below.
            }
        }
        ResourceLocation fallback = HexActions.PUSH_ONE_ID;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(KEY_SELECTED_ACTION, 8)) {
            ResourceLocation stored = new ResourceLocation(tag.getString(KEY_SELECTED_ACTION));
            if (HexActionRegistry.get(stored) != null) {
                return stored;
            }
        }
        return HexActionRegistry.get(fallback) == null ? HexActionRegistry.firstId() : fallback;
    }

    public static void setSelectedAction(ItemStack stack, ResourceLocation id) {
        HexActionRegistry.bootstrap();
        HexPattern pattern = id == null ? null : HexActionRegistry.getPattern(id);
        if (pattern != null) {
            ItemHexFocus focus = stack != null && stack.getItem() instanceof ItemHexFocus
                ? (ItemHexFocus) stack.getItem() : null;
            if (focus != null) {
                focus.writeDatum(stack, new PatternIota(pattern));
            }
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setString(KEY_SELECTED_ACTION, id.toString());
    }
    private static String localizeAction(ResourceLocation id) {
        String key = "hexcasting.action." + id.getResourcePath();
        String translated = I18n.translateToLocal(key);
        return key.equals(translated) ? id.getResourcePath() : translated;
    }

}
