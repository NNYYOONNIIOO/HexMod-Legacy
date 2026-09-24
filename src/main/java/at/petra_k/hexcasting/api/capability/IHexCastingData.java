package at.petra_k.hexcasting.api.capability;

import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import net.minecraft.nbt.NBTTagCompound;
import at.petra_k.hexcasting.api.addldata.ADMediaHolder;

import java.util.UUID;

/** Persistent player state used by the 1.12.2 casting backend. */
public interface IHexCastingData extends ADMediaHolder {
    CastingStack getCastingStack();

    void clearCastingStack();

    NBTTagCompound serializeNBT();

    void deserializeNBT(NBTTagCompound nbt);

    int getPigment();

    void setPigment(int pigment);

    String getPigmentVariant();

    UUID getPigmentOwner();

    void setPigmentVariant(String variant, UUID owner);

    /** Whether the caster has explicitly internalized a pigment. */
    boolean hasInternalizedPigment();

    void setInternalizedPigment(boolean internalized);

    /** Remaining ordinary flight time, in server ticks. */
    int getFlightTicks();

    void setFlightTicks(int ticks);

    /** Whether a range- or time-limited Hex flight is currently active. */
    boolean isFlightActive();

    void setFlightActive(boolean active);

    /** Dimension containing the origin of the active flight, if any. */
    int getFlightDimension();

    void setFlightDimension(int dimension);

    double getFlightOriginX();

    double getFlightOriginY();

    double getFlightOriginZ();

    void setFlightOrigin(double x, double y, double z);

    /** Horizontal radius; a negative value denotes time-limited flight. */
    double getFlightRadius();

    void setFlightRadius(double radius);

    /** Remaining Altiora collision grace, in server ticks. */
    int getAltioraTicks();

    void setAltioraTicks(int ticks);

    boolean isAltioraActive();

    void setAltioraActive(boolean active);
}
