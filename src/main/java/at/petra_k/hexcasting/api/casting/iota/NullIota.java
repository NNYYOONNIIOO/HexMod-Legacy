package at.petra_k.hexcasting.api.casting.iota;

import net.minecraft.nbt.NBTTagCompound;

/** Explicit null value used by Hex Casting's stack semantics. */
public final class NullIota extends Iota {
    public static final IotaType<NullIota> TYPE =
        new IotaType<>("null", data -> new NullIota());

    public NullIota() {
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
        return "null";
    }

    @Override
    protected void serializePayload(NBTTagCompound data) {
        // Null has no payload.
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof NullIota;
    }

    @Override
    public int hashCode() {
        return TYPE.getId().hashCode();
    }
}
