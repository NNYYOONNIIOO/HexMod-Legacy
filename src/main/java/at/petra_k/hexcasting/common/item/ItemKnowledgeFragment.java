package at.petra_k.hexcasting.common.item;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Knowledge-bearing item used by the lore fragment and creative unlocker. */
public final class ItemKnowledgeFragment extends Item {
    private static final String KEY_UNLOCKED = "hexcasting_knowledge_unlocked";
    private static final List<String> LORE_ADVANCEMENTS = Arrays.asList(
        "lore/cardamom1",
        "lore/cardamom2",
        "lore/cardamom3",
        "lore/cardamom4",
        "lore/cardamom5",
        "lore/experiment1",
        "lore/experiment2",
        "lore/inventory"
    );
    private final String variant;

    public ItemKnowledgeFragment(String variant) {
        this.variant = variant;
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            if ("lore_fragment".equals(variant)) {
                useLoreFragment(world, player, stack);
            } else if ("creative_unlocker".equals(variant)) {
                player.getEntityData().setBoolean(KEY_UNLOCKED, true);
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocal("hexcasting.message." + variant)));
            } else {
                player.sendMessage(new TextComponentString(
                    I18n.translateToLocal("hexcasting.message." + variant)));
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /** Port of Hex's lore-fragment progression behavior for Forge 1.12.2. */
    private void useLoreFragment(World world, EntityPlayer player, ItemStack stack) {
        if (!(player instanceof EntityPlayerMP) || !(world instanceof WorldServer)) {
            stack.shrink(1);
            return;
        }

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        WorldServer serverWorld = (WorldServer) world;
        Advancement found = null;
        List<String> shuffled = new ArrayList<>(LORE_ADVANCEMENTS);
        Collections.shuffle(shuffled);

        if (serverWorld.getMinecraftServer() != null) {
            for (String id : shuffled) {
                Advancement advancement = serverWorld.getMinecraftServer()
                    .getAdvancementManager()
                    .getAdvancement(new ResourceLocation("hexcasting", id));
                if (advancement != null
                    && !serverPlayer.getAdvancements().getProgress(advancement).isDone()) {
                    found = advancement;
                    break;
                }
            }
        }

        if (found != null) {
            serverPlayer.getAdvancements().grantCriterion(found, "grant");
        } else {
            serverPlayer.sendStatusMessage(new TextComponentString(
                I18n.translateToLocal("hexcasting.message.lore_fragment")), true);
            serverPlayer.addExperience(20);
        }
        stack.shrink(1);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        tooltip.add(I18n.translateToLocal("hexcasting.tooltip." + variant));
    }
}
