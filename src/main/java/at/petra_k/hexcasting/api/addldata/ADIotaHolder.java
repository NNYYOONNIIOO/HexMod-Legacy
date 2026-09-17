package at.petra_k.hexcasting.api.addldata;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.nbt.NBTTagCompound;

/** Capability-facing view of an item that stores one Iota in its NBT. */
public interface ADIotaHolder {
    NBTTagCompound readIotaTag();

    default Iota readIota() throws CastingException {
        NBTTagCompound tag = readIotaTag();
        return tag == null ? null : HexIotaTypes.deserialize(tag);
    }

    boolean writeIota(Iota iota, boolean simulate);

    boolean writeable();
}
