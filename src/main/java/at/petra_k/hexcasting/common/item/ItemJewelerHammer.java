package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.HexAPI;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/** A durable 1.12.2 tool item used by the Hex Casting crafting flow. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class ItemJewelerHammer extends ItemPickaxe {
    public ItemJewelerHammer() {
        super(Item.ToolMaterial.IRON);
        setMaxStackSize(1);
        setMaxDamage(Item.ToolMaterial.DIAMOND.getMaxUses());
    }

    /** Prevent the hammer from breaking full cubes; it is intended for jewelers' shapes. */
    public static boolean shouldFailToBreak(EntityPlayer player, IBlockState state, BlockPos pos) {
        return player != null
            && player.getHeldItemMainhand().getItem() instanceof ItemJewelerHammer
            && state != null
            && state.getBlock().isFullCube(state);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (shouldFailToBreak(event.getPlayer(), event.getState(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        tooltip.add(I18n.translateToLocal("hexcasting.tooltip.jeweler_hammer"));
    }
}
