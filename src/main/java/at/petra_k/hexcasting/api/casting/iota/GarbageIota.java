package at.petra_k.hexcasting.api.casting.iota;

import net.minecraft.nbt.NBTTagCompound;

/** Sentinel value used when a computation deliberately discards its result. */
public final class GarbageIota extends Iota {
    public static final IotaType<GarbageIota> TYPE =
        new IotaType<>("garbage", data -> new GarbageIota());

    public GarbageIota() {
        super(TYPE);
    }

    @Override
    public Object getPayload() {
        return null;
    }

    @Override
    public boolean isTruthy() {
        return false;
    }

    @Override
    public String display() {
        return "garbage";
    }

    @Override
    protected void serializePayload(NBTTagCompound data) {
        // No payload.
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GarbageIota;
    }

    @Override
    public int hashCode() {
        return TYPE.getId().hashCode();
    }
}
