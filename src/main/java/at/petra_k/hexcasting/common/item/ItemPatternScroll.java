package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.casting.HexEvaluator;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.common.lib.hex.HexActions;
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
public final class ItemPatternScroll extends Item {
    private static final String KEY_ACTION = "action";

    public ItemPatternScroll() {
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            HexActionRegistry.bootstrap();
            ResourceLocation current = getActionId(stack);
            if (!player.isSneaking() && ItemHexStaff.isStaff(player.getHeldItemOffhand())) {
                boolean added = ItemHexStaff.appendAction(
                    player.getHeldItemOffhand(), current);
                if (added) {
                    consumeForWrite(stack, player);
                    player.sendMessage(new TextComponentString(
                        I18n.translateToLocalFormatted(
                            "hexcasting.message.program_added",
                            localizeAction(current),
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
                ItemHexFocus.setSelectedAction(player.getHeldItemOffhand(), current);
                consumeForWrite(stack, player);
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocalFormatted(
                        "hexcasting.message.program_added",
                        localizeAction(current), 1, 1)));
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
            if (player.isSneaking()) {
                ResourceLocation next = HexActionRegistry.nextId(current);
                if (next != null) {
                    setActionId(stack, next);
                    player.sendMessage(new TextComponentString(
                        I18n.translateToLocalFormatted(
                            "hexcasting.message.scroll_selected",
                            next.getResourcePath()
                        )
                    ));
                }
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }

            try {
                HexPattern pattern = HexActionRegistry.getPattern(current);
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
                        current.getResourcePath(),
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
        HexPattern pattern = HexActionRegistry.getPattern(id);
        tooltip.add(I18n.translateToLocal("hexcasting.tooltip.action") + ": " + localizeAction(id));
        if (pattern != null) {
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.pattern") + ": " + HexInline.formatPattern(pattern));
        }
        tooltip.add(I18n.translateToLocal("hexcasting.tooltip.cycle"));
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        ResourceLocation id = getActionId(stack);
        return I18n.translateToLocalFormatted(
            "hexcasting.item.pattern_scroll.variant",
            super.getItemStackDisplayName(stack), localizeAction(id));
    }

    public static ResourceLocation getActionId(ItemStack stack) {
        HexActionRegistry.bootstrap();
        ResourceLocation fallback = HexActions.PUSH_ONE_ID;
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
        ResourceLocation defaultId = HexActionRegistry.get(fallback) == null
            ? HexActionRegistry.firstId() : fallback;
        return defaultId == null ? fallback : defaultId;
    }

    private static void setActionId(ItemStack stack, ResourceLocation id) {
        if (id == null) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setString(KEY_ACTION, id.toString());
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
        String key = "hexcasting.action." + id.getResourcePath();
        String translated = I18n.translateToLocal(key);
        return key.equals(translated) ? id.getResourcePath() : translated;
    }

}
