package at.petra_k.hexcasting.api.item;

import at.petra_k.hexcasting.api.casting.iota.Iota;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/** Item contract for a packaged, executable Hex. */
public interface HexHolderItem extends MediaHolderItem {
    boolean canDrawMediaFromInventory(ItemStack stack);

    boolean hasHex(ItemStack stack);

    List<Iota> getHex(ItemStack stack, World world);

    void writeHex(ItemStack stack, List<Iota> program, int pigment,
                  String pigmentVariant, UUID pigmentOwner, long media);

    void clearHex(ItemStack stack);

    int getPigment(ItemStack stack);

    String getPigmentVariant(ItemStack stack);

    UUID getPigmentOwner(ItemStack stack);
}
