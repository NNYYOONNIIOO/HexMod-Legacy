package at.petra_k.hexcasting.api.capability;

import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import net.minecraft.nbt.NBTTagCompound;
import at.petra_k.hexcasting.api.addldata.ADMediaHolder;

/** Persistent player state used by the 1.12.2 casting backend. */
public interface IHexCastingData extends ADMediaHolder {
    CastingStack getCastingStack();

    void clearCastingStack();

    NBTTagCompound serializeNBT();

    void deserializeNBT(NBTTagCompound nbt);

    int getPigment();

    void setPigment(int pigment);

    /** Remaining ordinary flight time, in server ticks. */
    int getFlightTicks();

    void setFlightTicks(int ticks);

    /** Remaining Altiora collision grace, in server ticks. */
    int getAltioraTicks();

    void setAltioraTicks(int ticks);
}
