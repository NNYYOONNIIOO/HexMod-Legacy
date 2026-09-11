package at.petra_k.hexcasting.api.capability;

import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import net.minecraft.nbt.NBTTagCompound;

/** Persistent player state used by the 1.12.2 casting backend. */
public interface IHexCastingData {
    CastingStack getCastingStack();

    void clearCastingStack();

    NBTTagCompound serializeNBT();

    void deserializeNBT(NBTTagCompound nbt);
}
