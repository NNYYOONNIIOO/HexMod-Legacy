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
    private static final ResourceLocation[] CLUSTERS = {
        new ResourceLocation("farmers_future_delight", "amethyst_cluster"),
        // Farmers Future Delight can hand this feature to an installed
        // backport in AUTO mode.  In that case the world contains the
        // provider's block rather than an FFD block, while the gameplay
        // expectation is still the FFD cluster drop rule.
        new ResourceLocation("cavesnotcliffs", "amethyst_cluster"),
        new ResourceLocation("depthsupdate", "amethyst_cluster")
    };
    private static final ResourceLocation[] SHARDS = {
        new ResourceLocation("farmers_future_delight", "amethyst_shard"),
        new ResourceLocation("cavesnotcliffs", "amethyst_shard"),
        new ResourceLocation("depthsupdate", "amethyst_shard")
    };

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
        ResourceLocation clusterId = event.getState().getBlock().getRegistryName();
        int fortune = Math.max(0, event.getFortuneLevel());
        int maximum = 4 + fortune;
        int count = 2 + event.getWorld().rand.nextInt(maximum - 1);
        ItemStack shardDrop = effectiveShardStack(clusterId, count);
        if (shardDrop.isEmpty()) {
            return;
        }

        // Replace the backport's fixed 2/4 shard result, and also prevent a
        // second shard entry supplied by another harvest hook from stacking
        // on top of the adjusted amount.
        Iterator<ItemStack> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            ItemStack stack = drops.next();
            if (isAmethystShard(stack, shardDrop)) {
                drops.remove();
            }
        }

        event.getDrops().add(shardDrop);

        Item charged = HexItems.EXTRA_ITEMS.get("charged_amethyst");
        if (charged != null && event.getWorld().rand.nextFloat()
            < chargedChance(fortune)) {
            event.getDrops().add(new ItemStack(charged, 1));
        }
        // HarvestDropsEvent is filtered once more by Block#dropBlockAsItem;
        // the replacement shards and charged crystal are intentional drops.
        event.setDropChance(1.0F);
    }

    /**
     * Resolve FFD's effective item before falling back to the registry.  FFD
     * deliberately leaves its local shard unregistered in AUTO mode when a
     * supported backport supplies the content, so looking up only
     * farmers_future_delight:amethyst_shard can produce no drop at all.
     */
    private static ItemStack effectiveShardStack(ResourceLocation clusterId,
                                                 int count) {
        ItemStack effective = invokeFfdEffectiveShard(count);
        if (!effective.isEmpty()) {
            return effective;
        }
        Item shard = findShard(clusterId);
        return shard == null ? ItemStack.EMPTY : new ItemStack(shard, count);
    }

    private static ItemStack invokeFfdEffectiveShard(int count) {
        try {
            Class<?> itemsClass = Class.forName(
                "xy177.farmersfuturedelight.common.registry.FFDItems");
            java.lang.reflect.Field field = itemsClass.getField("AMETHYST_SHARD");
            Object localItem = field.get(null);
            java.lang.reflect.Method method = itemsClass.getMethod(
                "effectiveStack", Item.class, int.class);
            Object result = method.invoke(null, localItem, count);
            if (result instanceof ItemStack) {
                ItemStack stack = (ItemStack) result;
                if (!stack.isEmpty()) {
                    stack.setCount(count);
                    return stack;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // FFD is optional and its compatibility implementation is not a
            // compile-time dependency of this mod.
        }
        return ItemStack.EMPTY;
    }

    private static boolean isTargetCluster(IBlockState state) {
        ResourceLocation registryName = state.getBlock().getRegistryName();
        if (registryName != null) {
            for (ResourceLocation cluster : CLUSTERS) {
                if (cluster.equals(registryName)) {
                    return true;
                }
            }
        }
        for (ResourceLocation cluster : CLUSTERS) {
            if (state.getBlock() == ForgeRegistries.BLOCKS.getValue(cluster)) {
                return true;
            }
        }
        return false;
    }

    private static Item findShard(ResourceLocation clusterId) {
        if (clusterId != null) {
            Item matching = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation(clusterId.getResourceDomain(), "amethyst_shard"));
            if (matching != null) {
                return matching;
            }
        }
        for (ResourceLocation shardId : SHARDS) {
            Item shard = ForgeRegistries.ITEMS.getValue(shardId);
            if (shard != null) {
                return shard;
            }
        }
        // Some backports expose the effective item under a compatibility
        // registry name while retaining the amethyst_shard path.  Keep this
        // hook independent of that implementation detail.
        for (Item candidate : ForgeRegistries.ITEMS.getValuesCollection()) {
            ResourceLocation id = candidate == null ? null : candidate.getRegistryName();
            if (id == null || !"amethyst_shard".equals(id.getResourcePath())) {
                continue;
            }
            for (ResourceLocation known : SHARDS) {
                if (known.getResourceDomain().equals(id.getResourceDomain())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean isAmethystShard(ItemStack stack, ItemStack replacement) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) {
            return false;
        }
        if (replacement != null && !replacement.isEmpty()
            && stack.getItem() == replacement.getItem()
            && stack.getMetadata() == replacement.getMetadata()) {
            return true;
        }
        ResourceLocation itemId = stack.getItem().getRegistryName();
        // A compatibility provider may rename the local item (for example to
        // crystal_shard), so the replacement identity check above is the
        // authoritative test.  Registry-path matching covers the normal FFD
        // and backport registrations as well.
        return itemId != null && "amethyst_shard".equals(itemId.getResourcePath());
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
