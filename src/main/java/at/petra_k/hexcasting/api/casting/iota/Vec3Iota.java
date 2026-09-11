package at.petra_k.hexcasting.api.casting.iota;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

/** Three-dimensional vector Iota backed by Minecraft 1.12.2's Vec3d. */
public final class Vec3Iota extends Iota {
    public static final String KEY_X = "x";
    public static final String KEY_Y = "y";
    public static final String KEY_Z = "z";
    public static final IotaType<Vec3Iota> TYPE =
        new IotaType<>("vec3", data -> new Vec3Iota(new Vec3d(
            data.getDouble(KEY_X), data.getDouble(KEY_Y), data.getDouble(KEY_Z)
        )));

    private final Vec3d value;

    public Vec3Iota(Vec3d value) {
        super(TYPE);
        this.value = Objects.requireNonNull(value, "value");
    }

    public Vec3d getValue() {
        return value;
    }

    @Override
    public Vec3d getPayload() {
        return value;
    }

    @Override
    public boolean isTruthy() {
        return value.x != 0.0D || value.y != 0.0D || value.z != 0.0D;
    }

    @Override
    public String display() {
        return "(" + value.x + ", " + value.y + ", " + value.z + ")";
    }

    @Override
    protected void serializePayload(NBTTagCompound data) {
        data.setDouble(KEY_X, value.x);
        data.setDouble(KEY_Y, value.y);
        data.setDouble(KEY_Z, value.z);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Vec3Iota && value.equals(((Vec3Iota) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
