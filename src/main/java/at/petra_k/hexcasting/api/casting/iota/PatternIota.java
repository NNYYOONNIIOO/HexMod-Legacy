package at.petra_k.hexcasting.api.casting.iota;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;

/** Iota containing a drawable Hex Pattern. */
public final class PatternIota extends Iota {
    public static final String KEY_PATTERN = "pattern";
    public static final IotaType<PatternIota> TYPE =
        new IotaType<>("pattern", data -> new PatternIota(HexPattern.fromNBT(data.getCompoundTag(KEY_PATTERN))));

    private final HexPattern pattern;

    public PatternIota(HexPattern pattern) {
        super(TYPE);
        this.pattern = Objects.requireNonNull(pattern, "pattern");
    }

    public HexPattern getPattern() {
        return pattern;
    }

    @Override
    public HexPattern getPayload() {
        return pattern;
    }

    @Override
    public boolean isTruthy() {
        return true;
    }

    @Override
    public String display() {
        return pattern.signature();
    }

    @Override
    protected void serializePayload(NBTTagCompound data) {
        data.setTag(KEY_PATTERN, pattern.serializeToNBT());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PatternIota && pattern.equals(((PatternIota) other).pattern);
    }

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }
}
