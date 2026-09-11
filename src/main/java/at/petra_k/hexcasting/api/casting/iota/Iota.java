package at.petra_k.hexcasting.api.casting.iota;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;

/** Base value carried by the Hex Casting evaluation stack. */
public abstract class Iota {
    public static final int MAX_SERIALIZATION_DEPTH = 256;
    public static final int MAX_SERIALIZATION_TOTAL = 1024;

    private final IotaType<?> type;

    protected Iota(IotaType<?> type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public final IotaType<?> getType() {
        return type;
    }

    /**
     * Compare Iotas using the tolerant value semantics used by arithmetic and
     * collection operators. Exact equality remains available through
     * {@link Object#equals(Object)} for persistence and map/set keys.
     */
    public static boolean tolerates(Iota left, Iota right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null
            || !left.getType().getId().equals(right.getType().getId())) {
            return false;
        }
        if (left instanceof DoubleIota && right instanceof DoubleIota) {
            return Math.abs(((DoubleIota) left).getValue()
                - ((DoubleIota) right).getValue()) <= 1.0E-5D;
        }
        if (left instanceof Vec3Iota && right instanceof Vec3Iota) {
            net.minecraft.util.math.Vec3d a = ((Vec3Iota) left).getValue();
            net.minecraft.util.math.Vec3d b = ((Vec3Iota) right).getValue();
            return Math.abs(a.x - b.x) <= 1.0E-5D
                && Math.abs(a.y - b.y) <= 1.0E-5D
                && Math.abs(a.z - b.z) <= 1.0E-5D;
        }
        if (left instanceof ListIota && right instanceof ListIota) {
            java.util.List<Iota> leftItems = ((ListIota) left).getItems();
            java.util.List<Iota> rightItems = ((ListIota) right).getItems();
            if (leftItems.size() != rightItems.size()) {
                return false;
            }
            for (int i = 0; i < leftItems.size(); i++) {
                if (!tolerates(leftItems.get(i), rightItems.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return left.equals(right);
    }

    public abstract Object getPayload();

    public abstract boolean isTruthy();

    /** A compact, server-safe representation used by tooltips and diagnostics. */
    public abstract String display();

    protected abstract void serializePayload(NBTTagCompound data);

    public final NBTTagCompound serialize() {
        NBTTagCompound out = new NBTTagCompound();
        out.setString("type", type.getId());
        NBTTagCompound data = new NBTTagCompound();
        serializePayload(data);
        out.setTag("data", data);
        return out;
    }

    @Override
    public String toString() {
        return type.getId() + "(" + display() + ")";
    }
}
