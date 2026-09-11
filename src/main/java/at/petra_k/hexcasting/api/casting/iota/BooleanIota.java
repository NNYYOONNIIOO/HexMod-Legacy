package at.petra_k.hexcasting.api.casting.iota;

import net.minecraft.nbt.NBTTagCompound;

/** Boolean Iota. */
public final class BooleanIota extends Iota {
    public static final String KEY_VALUE = "value";
    public static final IotaType<BooleanIota> TYPE =
        new IotaType<>("boolean", data -> new BooleanIota(data.getBoolean(KEY_VALUE)));

    private final boolean value;

    public BooleanIota(boolean value) {
        super(TYPE);
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public Boolean getPayload() {
        return value;
    }

    @Override
    public boolean isTruthy() {
        return value;
    }

    @Override
    public String display() {
        return Boolean.toString(value);
    }

    @Override
    protected void serializePayload(NBTTagCompound data) {
        data.setBoolean(KEY_VALUE, value);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BooleanIota && value == ((BooleanIota) other).value;
    }

    @Override
    public int hashCode() {
        return value ? 1 : 0;
    }
}
