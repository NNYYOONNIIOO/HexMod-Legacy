package at.petra_k.hexcasting.api.casting.iota;

import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Ordered collection of Iotas. */
public final class ListIota extends Iota {
    public static final String KEY_ITEMS = "items";
    public static final IotaType<ListIota> TYPE =
        new IotaType<>("list", data -> {
            NBTTagList serialized = data.getTagList(KEY_ITEMS, 10);
            ArrayList<Iota> items = new ArrayList<>(serialized.tagCount());
            for (int i = 0; i < serialized.tagCount(); i++) {
                items.add(HexIotaTypes.deserialize(serialized.getCompoundTagAt(i)));
            }
            return new ListIota(items);
        });

    private final List<Iota> items;

    public ListIota(List<? extends Iota> items) {
        super(TYPE);
        ArrayList<Iota> copy = new ArrayList<>(items.size());
        for (Iota item : items) {
            copy.add(Objects.requireNonNull(item, "items cannot contain null"));
        }
        this.items = copy;
    }

    public List<Iota> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public List<Iota> getPayload() {
        return getItems();
    }

    @Override
    public boolean isTruthy() {
        return !items.isEmpty();
    }

    @Override
    public String display() {
        return "[" + items.size() + " iotas]";
    }

    @Override
    protected void serializePayload(NBTTagCompound data) {
        NBTTagList serialized = new NBTTagList();
        for (Iota item : items) {
            serialized.appendTag(item.serialize());
        }
        data.setTag(KEY_ITEMS, serialized);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ListIota && items.equals(((ListIota) other).items);
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }
}
