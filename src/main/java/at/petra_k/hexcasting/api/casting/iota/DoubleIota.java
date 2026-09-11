package at.petra_k.hexcasting.api.casting.iota;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;

/** Numeric Iota backed by a double, matching Hex Casting's numeric semantics. */
public final class DoubleIota extends Iota {
    public static final String KEY_VALUE = "value";
    public static final IotaType<DoubleIota> TYPE =
        new IotaType<>("double", data -> new DoubleIota(data.getDouble(KEY_VALUE)));

    private final double value;

    public DoubleIota(double value) {
        super(TYPE);
        this.value = value;
    }

    @Override
    public Double getPayload() {
        return value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public boolean isTruthy() {
        return value != 0.0D && !Double.isNaN(value);
    }

    @Override
    public String display() {
        return Double.toString(value);
    }

    @Override
    protected void serializePayload(NBTTagCompound data) {
        data.setDouble(KEY_VALUE, value);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DoubleIota
            && Double.compare(value, ((DoubleIota) other).value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
