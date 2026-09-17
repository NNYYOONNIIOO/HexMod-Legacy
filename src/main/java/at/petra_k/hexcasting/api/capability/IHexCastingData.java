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

    /** Remaining ordinary flight time, in server ticks. */
    int getFlightTicks();

    void setFlightTicks(int ticks);

    /** Remaining Altiora collision grace, in server ticks. */
    int getAltioraTicks();

    void setAltioraTicks(int ticks);

    boolean isAltioraActive();

    void setAltioraActive(boolean active);
}
