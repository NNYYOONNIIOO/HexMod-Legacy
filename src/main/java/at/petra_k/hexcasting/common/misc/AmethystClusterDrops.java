package at.petra_k.hexcasting.common.misc;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.lib.HexItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

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

    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
    public static void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event == null || event.getWorld() == null || event.getWorld().isRemote
            || event.getState() == null || event.getState().getBlock() == null
            || !isTargetCluster(event.getState())
            || event.isSilkTouching()) {
            return;
        }

        // Forge's registry is the authoritative view for third-party items;
        // using it also avoids depending on the backport's registry timing.
        Item shard = ForgeRegistries.ITEMS.getValue(SHARD);
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

        Item charged = HexItems.EXTRA_ITEMS.get("charged_amethyst");
        if (charged != null && event.getWorld().rand.nextFloat()
            < chargedChance(fortune)) {
            event.getDrops().add(new ItemStack(charged, 1));
        }
    }

    private static boolean isTargetCluster(IBlockState state) {
        ResourceLocation registryName = state.getBlock().getRegistryName();
        return CLUSTER.equals(registryName)
            || state.getBlock() == ForgeRegistries.BLOCKS.getValue(CLUSTER);
    }

    private static float chargedChance(int fortune) {
        switch (fortune) {
            case 0: return 0.25F;
            case 1: return 0.35F;
            case 2: return 0.50F;
            case 3: return 0.75F;
            default: return 1.0F;
        }
    }
}
