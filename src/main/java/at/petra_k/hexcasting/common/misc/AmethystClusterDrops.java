package at.petra_k.hexcasting.common.misc;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.common.lib.HexItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Iterator;

/** Adjusts the selected provider's mature amethyst cluster drops. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class AmethystClusterDrops {
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
        boolean maxHarvestable = isMaxHarvestable(event);
        int dustCount = maxHarvestable
            ? 2 + event.getWorld().rand.nextInt(3 + fortune)
            : event.getWorld().rand.nextInt(3);
        Item dust = HexItems.EXTRA_ITEMS.get("amethyst_dust");
        if (dust != null && dustCount > 0) {
            event.getDrops().add(new ItemStack(dust, dustCount));
        }

        // The provider's normal shard drop is retained, but reduced by the
        // same 50% modifier as Hex's amethyst-cluster loot injection.  Do not
        // replace it with a random stack: that would discard other drop
        // conditions and could create shards when the provider supplied none.
        ItemStack shardIdentity = effectiveShardStack(clusterId, 1);

        Iterator<ItemStack> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            ItemStack stack = drops.next();
            if (isAmethystShard(stack, shardIdentity)) {
                int reducedCount = stack.getCount() / 2;
                if (reducedCount <= 0) {
                    drops.remove();
                } else {
                    stack.setCount(reducedCount);
                }
            }
        }

        Item charged = HexItems.EXTRA_ITEMS.get("charged_amethyst");
        float chance = maxHarvestable ? chargedChance(fortune) : 0.125F;
        if (charged != null && event.getWorld().rand.nextFloat() < chance) {
            event.getDrops().add(new ItemStack(charged, 1));
        }
        // HarvestDropsEvent is filtered once more by Block#dropBlockAsItem;
        // the replacement shards and charged crystal are intentional drops.
        event.setDropChance(1.0F);
    }

    private static boolean isMaxHarvestable(BlockEvent.HarvestDropsEvent event) {
        EntityPlayer harvester = event.getHarvester();
        if (harvester == null) {
            return false;
        }
        ItemStack tool = harvester.getHeldItemMainhand();
        return tool != null && !tool.isEmpty()
            && tool.canHarvestBlock(event.getState());
    }

    /**
     * Resolve FFD's effective item before falling back to the registry.  FFD
     * deliberately leaves its local shard unregistered in AUTO mode when a
     * supported backport supplies the content, so looking up only
     * farmers_future_delight:amethyst_shard can produce no drop at all.
     */
    private static ItemStack effectiveShardStack(ResourceLocation clusterId,
                                                 int count) {
        // The requested gameplay item is FFD's own shard.  Prefer it when it
        // is registered; the compatibility helper may intentionally redirect
        // the item to a different provider in AUTO mode.
        Item shard = findShard(clusterId);
        if (shard != null) {
            return new ItemStack(shard, count);
        }
        return invokeFfdEffectiveShard(count);
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
        if (AmethystCompat.isSelectedCluster(registryName)) {
            return true;
        }
        // Keep the old third-party integration only when neither preferred
        // provider is present.  This prevents FFD drops from being applied to
        // both providers when Caves Not Cliffs has priority.
        if (!AmethystCompat.hasProvider()
            && AmethystCompat.legacyClusterId().equals(registryName)) {
            return true;
        }
        ResourceLocation target = AmethystCompat.hasProvider()
            ? AmethystCompat.clusterId() : AmethystCompat.legacyClusterId();
        return target != null
            && state.getBlock() == ForgeRegistries.BLOCKS.getValue(target);
    }

    private static Item findShard(ResourceLocation clusterId) {
        ResourceLocation selectedShard = AmethystCompat.shardId();
        if (selectedShard != null) {
            Item matching = ForgeRegistries.ITEMS.getValue(selectedShard);
            if (matching != null) {
                return matching;
            }
        } else if (clusterId != null) {
            Item matching = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation(clusterId.getResourceDomain(), "amethyst_shard"));
            if (matching != null) {
                return matching;
            }
        }
        if (!AmethystCompat.hasProvider()) {
            Item legacy = ForgeRegistries.ITEMS.getValue(
                AmethystCompat.legacyShardId());
            if (legacy != null) {
                return legacy;
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
            if (AmethystCompat.hasProvider()) {
                if (AmethystCompat.provider().equals(id.getResourceDomain())) {
                    return candidate;
                }
            } else if (AmethystCompat.legacyShardId().getResourceDomain()
                .equals(id.getResourceDomain())) {
                return candidate;
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
        if (itemId == null || !"amethyst_shard".equals(itemId.getResourcePath())) {
            return false;
        }
        return AmethystCompat.isSelectedShard(itemId)
            || (!AmethystCompat.hasProvider()
                && AmethystCompat.legacyShardId().equals(itemId));
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
