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
            if (player.isSneaking()) {
                ResourceLocation next = HexActionRegistry.nextId(current);
                if (next != null) {
                    setActionId(stack, next);
                    player.sendMessage(new TextComponentString(
                        "Hex scroll pattern: " + next.getResourcePath()
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
                    HexEvaluator.evaluate(Collections.singletonList(pattern), data.getCastingStack());
                    result = data.getCastingStack();
                }
                player.sendMessage(new TextComponentString(
                    "Hex Scroll [" + current.getResourcePath() + "]: "
                        + (result.isEmpty() ? "empty stack" : result.peek().display())
                ));
            } catch (CastingException exception) {
                player.sendMessage(new TextComponentString(
                    "Hex scroll error: " + exception.getMessage()
                ));
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (tab != getCreativeTab()) {
            return;
        }
        HexActionRegistry.bootstrap();
        for (ResourceLocation id : HexActionRegistry.byId().keySet()) {
            ItemStack scroll = new ItemStack(this);
            setActionId(scroll, id);
            items.add(scroll);
        }
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        HexActionRegistry.bootstrap();
        ResourceLocation id = getActionId(stack);
        HexPattern pattern = HexActionRegistry.getPattern(id);
        tooltip.add("Action: " + id.getResourcePath());
        if (pattern != null) {
            tooltip.add("Pattern: " + pattern.signature());
        }
        tooltip.add("Sneak + right click to cycle patterns");
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        ResourceLocation id = getActionId(stack);
        return super.getItemStackDisplayName(stack) + " [" + id.getResourcePath() + "]";
    }

    private static ResourceLocation getActionId(ItemStack stack) {
        ResourceLocation fallback = HexActions.PUSH_ONE_ID;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(KEY_ACTION, 8)) {
            ResourceLocation stored = new ResourceLocation(tag.getString(KEY_ACTION));
            if (HexActionRegistry.get(stored) != null) {
                return stored;
            }
        }
        return HexActionRegistry.get(fallback) == null ? HexActionRegistry.firstId() : fallback;
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
}
