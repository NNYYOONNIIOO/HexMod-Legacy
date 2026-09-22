package at.petra_k.hexcasting.common.misc;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.lib.HexBlocks;
import at.petra_k.hexcasting.common.lib.HexItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Implements the Silk Touch-only drop rule for the base quenched block. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class QuenchedAllayDrops {
    private QuenchedAllayDrops() {
    }

    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.HIGHEST)
    public static void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event == null || event.getWorld() == null || event.getWorld().isRemote
            || event.getState() == null
            || event.getState().getBlock() != HexBlocks.BLOCKS.get("quenched_allay")
            || event.isSilkTouching()) {
            return;
        }

        Item shard = HexItems.EXTRA_ITEMS.get("quenched_allay_shard");
        event.getDrops().clear();
        if (shard != null) {
            int count = 2 + event.getWorld().rand.nextInt(4);
            event.getDrops().add(new ItemStack(shard, count));
        }
        event.setDropChance(1.0F);
    }
}
