package at.petra_k.hexcasting.api.item;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Contract for items that store one Hex Iota in their own NBT. */
public interface IotaHolderItem {
    String TAG_DATA = "data";

    NBTTagCompound readIotaTag(ItemStack stack);

    default Iota readIota(ItemStack stack) throws CastingException {
        NBTTagCompound tag = readIotaTag(stack);
        return tag == null ? null : HexIotaTypes.deserialize(tag);
    }

    boolean writeable(ItemStack stack);

    boolean canWrite(ItemStack stack, Iota iota);

    void writeDatum(ItemStack stack, Iota iota);
}
