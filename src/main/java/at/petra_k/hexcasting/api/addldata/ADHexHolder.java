package at.petra_k.hexcasting.api.addldata;

import at.petra_k.hexcasting.api.casting.iota.Iota;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/** Capability-facing view of an item containing an executable Hex. */
public interface ADHexHolder {
    boolean canDrawMediaFromInventory();

    boolean hasHex();

    List<Iota> getHex(World world);

    void writeHex(List<Iota> program, int pigment, String pigmentVariant,
                  UUID pigmentOwner, long media);

    void clearHex();

    int getPigment();

    String getPigmentVariant();

    UUID getPigmentOwner();
}
