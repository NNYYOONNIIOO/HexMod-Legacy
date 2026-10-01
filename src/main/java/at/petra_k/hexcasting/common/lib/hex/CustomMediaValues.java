package at.petra_k.hexcasting.common.lib.hex;

import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** CraftTweaker-defined static media values, keyed by item and metadata. */
public final class CustomMediaValues {
    private static final Map<Key, Long> VALUES = new HashMap<>();

    private CustomMediaValues() {
    }

    public static synchronized boolean add(ItemStack stack, long media) {
        Key key = Key.of(stack);
        if (key == null || media < 0L) {
            return false;
        }
        VALUES.put(key, media);
        return true;
    }

    public static synchronized boolean remove(ItemStack stack, long media) {
        Key key = Key.of(stack);
        if (key == null) {
            return false;
        }
        Long current = VALUES.get(key);
        if (current == null || current.longValue() != media) {
            return false;
        }
        VALUES.remove(key);
        return true;
    }

    public static synchronized boolean has(ItemStack stack) {
        Key key = Key.of(stack);
        return key != null && VALUES.containsKey(key);
    }

    public static synchronized long get(ItemStack stack) {
        Key key = Key.of(stack);
        Long value = key == null ? null : VALUES.get(key);
        return value == null ? 0L : value;
    }

    public static synchronized List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<Key, Long> value : VALUES.entrySet()) {
            entries.add(new Entry(new ItemStack(value.getKey().item, 1,
                value.getKey().metadata), value.getValue()));
        }
        return Collections.unmodifiableList(entries);
    }

    public static final class Entry {
        private final ItemStack stack;
        private final long mediaPerItem;

        private Entry(ItemStack stack, long mediaPerItem) {
            this.stack = stack;
            this.mediaPerItem = mediaPerItem;
        }

        public ItemStack getStack() {
            return stack.copy();
        }

        public long getMediaPerItem() {
            return mediaPerItem;
        }
    }

    private static final class Key {
        private final net.minecraft.item.Item item;
        private final int metadata;

        private Key(net.minecraft.item.Item item, int metadata) {
            this.item = item;
            this.metadata = metadata;
        }

        private static Key of(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return null;
            }
            return new Key(stack.getItem(), stack.getMetadata());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key)) {
                return false;
            }
            Key key = (Key) other;
            return item == key.item && metadata == key.metadata;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(item) + metadata;
        }
    }
}
