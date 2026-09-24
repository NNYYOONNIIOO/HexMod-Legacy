package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.addldata.ADMediaHolder;
import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.item.MediaHolderItem;
import at.petra_k.hexcasting.api.misc.MediaConstants;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.capability.HexItemMediaHolder;
import at.petra_k.hexcasting.common.item.ItemMediaMaterial;
import at.petra_k.hexcasting.common.misc.AmethystCompat;
import at.petra_k.hexcasting.interop.baubles.BaublesExCompat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Shared media discovery and extraction for the 1.12.2 casting backend.
 *
 * <p>The old port only summed item holders here, while the VM consumed the
 * persistent player reserve directly.  That made {@code get_media} disagree
 * with an actual cast and made static media items (notably vanilla amethyst
 * shards) invisible to spells.  This class now owns both operations and uses
 * the same sorted source list for querying and extraction.</p>
 */
public final class MediaInventoryHelper {
    private MediaInventoryHelper() {
    }

    /** Start a transaction over the player's normal media sources. */
    public static MediaTransaction begin(EntityPlayer player, IHexCastingData data) {
        return begin(player, data, null, false);
    }

    /**
     * Start a transaction over one explicitly selected holder, such as an
     * Impetus or a packaged spell.  Explicit holders are intentionally the
     * complete source list: circles and packaged spells must not silently
     * drain the caster's inventory instead.
     */
    public static MediaTransaction begin(EntityPlayer player, IHexCastingData data,
                                         ADMediaHolder preferred) {
        return begin(player, data, preferred, false);
    }

    /**
     * Start a transaction with an explicitly bound source first, optionally
     * followed by the caster's normal media sources.  Packaged spells use
     * this to model the modern order: consume the media stored in the item,
     * then fall back to the player's inventory when the item allows it.
     * Circle Impetuses use the three-argument overload and therefore remain
     * bound to the Impetus alone.
     */
    public static MediaTransaction begin(EntityPlayer player, IHexCastingData data,
                                         ADMediaHolder preferred,
                                         boolean includeInventoryFallback) {
        List<MediaSource> sources = new ArrayList<>();
        if (preferred != null) {
            // Keep an explicitly bound holder in the transaction even when it
            // is empty.  This is important for Cyphers: an exhausted Cypher
            // must not silently become a normal inventory cast.
            sources.add(new MediaSource(preferred, null, true));
            if (!includeInventoryFallback) {
                return new MediaTransaction(sources);
            }
        }

        if (player != null) {
            for (ItemStack stack : player.inventory.mainInventory) {
                addStackSource(sources, stack);
            }
            for (ItemStack stack : player.inventory.armorInventory) {
                addStackSource(sources, stack);
            }
            for (ItemStack stack : player.inventory.offHandInventory) {
                addStackSource(sources, stack);
            }
            BaublesExCompat.forEach(player, stack -> addStackSource(sources, stack));
        }
        if (data != null) {
            addSource(sources, data, null);
        }

        // Modern Hex sorts ascending and then reverses the complete list.
        // Reversing after the stable sort is intentional: sources with equal
        // priority and equal fullness also reverse their discovery order.
        // Reproduce that detail so get_media and a real cast choose identical
        // stacks even when several amethyst sources are tied.
        Comparator<MediaSource> sourceComparator = new Comparator<MediaSource>() {
            @Override
            public int compare(MediaSource left, MediaSource right) {
                int priority = Integer.compare(left.getPriority(), right.getPriority());
                if (priority != 0) {
                    return priority;
                }
                return compareMedia(left.getAvailable(), right.getAvailable());
            }
        };
        if (preferred != null) {
            List<MediaSource> normalSources = new ArrayList<>(
                sources.subList(1, sources.size()));
            Collections.sort(normalSources, sourceComparator);
            Collections.reverse(normalSources);
            sources.subList(1, sources.size()).clear();
            sources.addAll(normalSources);
        } else {
            Collections.sort(sources, sourceComparator);
            Collections.reverse(sources);
        }
        return new MediaTransaction(sources);
    }

    /** Return the exact amount currently available through normal sources. */
    public static long getAvailableMedia(EntityPlayer player, IHexCastingData data) {
        return begin(player, data).getAvailableMedia();
    }

    /**
     * Extract media directly from an item stack, including static media items.
     * This is used by Craft Phial and by integrations which operate on an
     * item entity rather than the player's inventory.
     */
    public static long extractMedia(ItemStack stack, long amount,
                                    boolean drainForBatteries, boolean simulate) {
        MediaSource source = sourceForStack(stack, drainForBatteries);
        if (source == null) {
            return 0L;
        }
        return source.withdraw(amount, simulate);
    }

    /**
     * Extract from a dropped item entity without losing the entity's remainder.
     * ItemMediaMaterial has per-item media semantics, so it uses its precise
     * split implementation; all other holders are copied back to the entity
     * after extraction so a failed or capped phial never destroys the excess.
     */
    public static long extractMedia(EntityItem entity, long amount,
                                    boolean drainForBatteries, boolean simulate) {
        if (entity == null || entity.getItem() == null || entity.getItem().isEmpty()) {
            return 0L;
        }
        ItemStack stack = entity.getItem();
        if (stack.getItem() instanceof ItemMediaMaterial) {
            ItemMediaMaterial material = (ItemMediaMaterial) stack.getItem();
            if (drainForBatteries && !material.canConstructBattery(stack)) {
                return 0L;
            }
            return material.withdrawMediaFromEntity(entity, amount, simulate);
        }
        if (simulate) {
            return extractMedia(stack, amount, drainForBatteries, true);
        }
        ItemStack working = stack.copy();
        long extracted = extractMedia(working, amount, drainForBatteries, false);
        entity.setItem(working);
        if (working.isEmpty()) {
            entity.setDead();
        }
        return extracted;
    }

    /** Whether this stack can provide media for a normal spell. */
    public static boolean isMediaItem(ItemStack stack) {
        MediaSource source = sourceForStack(stack, false);
        return source != null && source.getAvailable() > 0L;
    }

    /** Whether this stack can provide media to construct a battery/phial. */
    public static boolean isBatteryMediaItem(ItemStack stack) {
        MediaSource source = sourceForStack(stack, true);
        return source != null && source.getAvailable() > 0L;
    }

    public static boolean isBatteryMediaEntity(EntityItem entity) {
        return entity != null && isBatteryMediaItem(entity.getItem());
    }

    /**
     * Resolve the media capability for an item without requiring it to be a
     * source.  Recharge targets are allowed to be write-only holders, so this
     * deliberately does not call {@link ADMediaHolder#canProvide()}.
     *
     * <p>Native 1.12 items are adapted first.  Forge can attach a capability
     * to the same stack as well, but the item contract remains authoritative
     * for native holders so their custom NBT and stack rules are preserved.</p>
     */
    public static ADMediaHolder findMediaHolder(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof MediaHolderItem) {
            return new HexItemMediaHolder((MediaHolderItem) stack.getItem(), stack);
        }
        if (HexCapabilities.MEDIA != null) {
            return stack.getCapability(HexCapabilities.MEDIA, null);
        }
        return null;
    }

    /** Whether an item has a rechargeable reservoir with non-zero capacity. */
    public static boolean canRechargeItem(ItemStack stack) {
        ADMediaHolder holder = findMediaHolder(stack);
        return holder != null && holder.canRecharge()
            && holder.insertMedia(-1L, true) > 0L;
    }

    /** Query or insert media through the same adapter used by spell actions. */
    public static long insertMedia(ItemStack stack, long amount, boolean simulate) {
        ADMediaHolder holder = findMediaHolder(stack);
        if (holder == null || !holder.canRecharge()) {
            return 0L;
        }
        return Math.max(0L, holder.insertMedia(amount, simulate));
    }

    private static void addStackSource(List<MediaSource> sources, ItemStack stack) {
        MediaSource source = sourceForStack(stack, false);
        if (source != null && source.getAvailable() > 0L) {
            sources.add(source);
        }
    }

    private static void addSource(List<MediaSource> sources, ADMediaHolder holder,
                                  ItemStack stack) {
        if (holder != null && holder.canProvide()
            && holder.withdrawMedia(-1L, true) > 0L) {
            sources.add(new MediaSource(holder, stack));
        }
    }

    private static MediaSource sourceForStack(ItemStack stack, boolean drainForBatteries) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        ADMediaHolder holder = findMediaHolder(stack);
        if (holder != null && holder.canProvide()
            && (!drainForBatteries || holder.canConstructBattery())) {
            return new MediaSource(holder, stack);
        }

        long worth = staticMediaWorth(stack);
        if (worth <= 0L) {
            return null;
        }
        return new MediaSource(new StaticMediaHolder(stack, worth,
            staticMediaPriority(stack)), stack);
    }

    /**
     * 1.12.2 has no common item tags equivalent to the modern media tags, so
     * the optional integrations are identified by registry name.  Unknown
     * items remain untouched; known external amethyst shards participate
     * without making either integration a hard Java dependency.
     */
    private static long staticMediaWorth(ItemStack stack) {
        ResourceLocation id = stack.getItem().getRegistryName();
        if (id == null) {
            return 0L;
        }
        String domain = id.getResourceDomain();
        String path = id.getResourcePath();
        if ("minecraft".equals(domain) && "amethyst_shard".equals(path)) {
            return MediaConstants.SHARD_UNIT;
        }
        ResourceLocation selectedShard = AmethystCompat.shardId();
        if (selectedShard != null && selectedShard.equals(id)) {
            return MediaConstants.SHARD_UNIT;
        }
        // Keep the older optional provider readable when it is present, but
        // never let it compete with the selected Caves/Farmer provider.
        if (AmethystCompat.legacyShardId().equals(id)
            && selectedShard == null) {
            return MediaConstants.SHARD_UNIT;
        }
        if ("hexcasting".equals(domain) && "quenched_allay".equals(path)) {
            return MediaConstants.QUENCHED_BLOCK_UNIT;
        }
        return 0L;
    }

    private static int staticMediaPriority(ItemStack stack) {
        ResourceLocation id = stack.getItem().getRegistryName();
        if (id != null && "hexcasting".equals(id.getResourceDomain())
            && "quenched_allay".equals(id.getResourcePath())) {
            return (int) ADMediaHolder.QUENCHED_ALLAY_PRIORITY;
        }
        return (int) ADMediaHolder.AMETHYST_SHARD_PRIORITY;
    }

    private static int compareMedia(long left, long right) {
        return left < right ? -1 : left == right ? 0 : 1;
    }

    /** One discoverable and mutable media source. */
    public static final class MediaSource {
        private final ADMediaHolder holder;
        private final ItemStack stack;
        private final boolean explicit;

        private MediaSource(ADMediaHolder holder, ItemStack stack) {
            this(holder, stack, false);
        }

        private MediaSource(ADMediaHolder holder, ItemStack stack, boolean explicit) {
            this.holder = holder;
            this.stack = stack;
            this.explicit = explicit;
        }

        public long getAvailable() {
            return (explicit || holder.canProvide())
                ? Math.max(0L, holder.withdrawMedia(-1L, true)) : 0L;
        }

        public long withdraw(long amount, boolean simulate) {
            if (!explicit && !holder.canProvide()) {
                return 0L;
            }
            return Math.max(0L, holder.withdrawMedia(amount, simulate));
        }

        public int getPriority() {
            return holder.getConsumptionPriority();
        }

        private ItemStack getStack() {
            return stack;
        }

        private ADMediaHolder getHolder() {
            return holder;
        }
    }

    /**
     * Atomic media extraction.  A failed cast restores both item stacks and
     * capability-backed media to the exact state present before the cast.
     */
    public static final class MediaTransaction {
        private final List<MediaSource> sources;
        private final List<SourceSnapshot> snapshots = new ArrayList<>();
        private boolean finished;

        private MediaTransaction(List<MediaSource> sources) {
            this.sources = sources;
        }

        public long getAvailableMedia() {
            long total = 0L;
            for (MediaSource source : sources) {
                total = saturatingAdd(total, source.getAvailable());
            }
            return total;
        }

        public void consume(long amount) throws at.petra_k.hexcasting.api.casting.eval.CastingException {
            if (finished || amount <= 0L) {
                return;
            }
            long available = getAvailableMedia();
            if (available < amount) {
                throw at.petra_k.hexcasting.api.casting.eval.Mishap.notEnoughMedia(
                    amount, available);
            }

            long remaining = amount;
            for (MediaSource source : sources) {
                if (remaining <= 0L) {
                    break;
                }
                if (source.getAvailable() <= 0L) {
                    continue;
                }
                snapshot(source);
                long extracted = source.withdraw(remaining, false);
                remaining -= extracted;
            }
            if (remaining > 0L) {
                rollback();
                throw at.petra_k.hexcasting.api.casting.eval.Mishap.notEnoughMedia(
                    amount, available);
            }
        }

        public void commit() {
            if (!finished) {
                finished = true;
                snapshots.clear();
            }
        }

        public void rollback() {
            if (finished) {
                return;
            }
            for (int i = snapshots.size() - 1; i >= 0; i--) {
                snapshots.get(i).restore();
            }
            snapshots.clear();
            finished = true;
        }

        private void snapshot(MediaSource source) {
            for (SourceSnapshot existing : snapshots) {
                if (existing.source == source) {
                    return;
                }
            }
            snapshots.add(new SourceSnapshot(source));
        }

        private static final class SourceSnapshot {
            private final MediaSource source;
            private final ItemStack stack;
            private final ItemStack beforeStack;
            private final long beforeMedia;

            private SourceSnapshot(MediaSource source) {
                this.source = source;
                this.stack = source.getStack();
                this.beforeStack = stack == null || stack.isEmpty() ? null : stack.copy();
                this.beforeMedia = source.getHolder().getMedia();
            }

            private void restore() {
                if (stack != null && beforeStack != null) {
                    // A stack may have reached EMPTY after extraction.  The
                    // original object is still the inventory slot object in
                    // 1.12.2, so restore its count and NBT in place.
                    stack.setCount(beforeStack.getCount());
                    stack.setItemDamage(beforeStack.getItemDamage());
                    stack.setTagCompound(beforeStack.getTagCompound() == null
                        ? null : beforeStack.getTagCompound().copy());
                }
                // Capability-backed holders may keep their value outside the
                // ItemStack NBT.  Restore the holder as well as the visible
                // stack, while static holders simply ignore setMedia.
                source.getHolder().setMedia(beforeMedia);
            }
        }
    }

    /** Static media such as vanilla amethyst shards and a Quenched block. */
    private static final class StaticMediaHolder implements ADMediaHolder {
        private final ItemStack stack;
        private final long worth;
        private final int priority;

        private StaticMediaHolder(ItemStack stack, long worth, int priority) {
            this.stack = stack;
            this.worth = Math.max(0L, worth);
            this.priority = priority;
        }

        @Override
        public long getMedia() {
            return multiply(worth, stack.getCount());
        }

        @Override
        public long getMaxMedia() {
            return getMedia();
        }

        @Override
        public void setMedia(long media) {
            // Static media is represented by item count, not a tag.
        }

        @Override
        public boolean canRecharge() {
            return false;
        }

        @Override
        public boolean canProvide() {
            return true;
        }

        @Override
        public int getConsumptionPriority() {
            return priority;
        }

        @Override
        public boolean canConstructBattery() {
            return true;
        }

        @Override
        public long withdrawMedia(long amount, boolean simulate) {
            long available = getMedia();
            long requested = amount < 0L ? available : Math.max(0L, amount);
            if (available <= 0L || requested <= 0L) {
                return 0L;
            }
            long count = ceilDivide(requested, worth);
            count = Math.min(count, Math.max(0, stack.getCount()));
            long extracted = multiply(worth, count);
            if (!simulate && count > 0L) {
                stack.shrink((int) Math.min(Integer.MAX_VALUE, count));
            }
            return extracted;
        }
    }

    private static long multiply(long left, long right) {
        if (left <= 0L || right <= 0L) {
            return 0L;
        }
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private static long ceilDivide(long numerator, long denominator) {
        if (numerator <= 0L || denominator <= 0L) {
            return 0L;
        }
        long quotient = numerator / denominator;
        return numerator % denominator == 0L ? quotient : quotient + 1L;
    }

    private static long saturatingAdd(long left, long right) {
        if (right <= 0L || Long.MAX_VALUE - left < right) {
            return right <= 0L ? left : Long.MAX_VALUE;
        }
        return left + right;
    }
}
