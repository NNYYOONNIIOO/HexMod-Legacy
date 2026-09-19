package at.petra_k.hexcasting.common.misc;

import at.petra_k.hexcasting.api.HexAPI;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Iterator;

/** Adjusts Farmers Future Delight's mature amethyst cluster drops. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class AmethystClusterDrops {
    private static final ResourceLocation CLUSTER =
        new ResourceLocation("farmers_future_delight", "amethyst_cluster");
    private static final ResourceLocation SHARD =
        new ResourceLocation("farmers_future_delight", "amethyst_shard");

    private AmethystClusterDrops() {
    }

    @SubscribeEvent
    public static void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event == null || event.getWorld() == null || event.getWorld().isRemote
            || event.getState() == null || event.getState().getBlock() == null
            || !CLUSTER.equals(event.getState().getBlock().getRegistryName())
            || event.isSilkTouching()) {
            return;
        }

        Item shard = Item.REGISTRY.getObject(SHARD);
        if (shard == null) {
            return;
        }

        // Replace the backport's fixed 2/4 shard result, and also prevent a
        // second shard entry supplied by another harvest hook from stacking
        // on top of the adjusted amount.
        Iterator<ItemStack> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            ItemStack stack = drops.next();
            if (stack != null && !stack.isEmpty() && shard == stack.getItem()) {
                drops.remove();
            }
        }

        int fortune = Math.max(0, event.getFortuneLevel());
        int maximum = 4 + fortune;
        int count = 2 + event.getWorld().rand.nextInt(maximum - 1);
        event.getDrops().add(new ItemStack(shard, count));
    }
}
