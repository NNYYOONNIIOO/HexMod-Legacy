package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.addldata.ADHexHolder;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.item.HexHolderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/** Adapter exposing a stack-backed HexHolderItem as a Forge capability. */
public final class HexItemHexHolder implements ADHexHolder {
    private final HexHolderItem item;
    private final ItemStack stack;

    public HexItemHexHolder(HexHolderItem item, ItemStack stack) {
        this.item = item;
        this.stack = stack;
    }

    @Override
    public boolean canDrawMediaFromInventory() {
        return item.canDrawMediaFromInventory(stack);
    }

    @Override
    public boolean hasHex() {
        return item.hasHex(stack);
    }

    @Override
    public List<Iota> getHex(World world) {
        return item.getHex(stack, world);
    }

    @Override
    public void writeHex(List<Iota> program, int pigment, String pigmentVariant,
                         UUID pigmentOwner, long media) {
        item.writeHex(stack, program, pigment, pigmentVariant, pigmentOwner, media);
    }

    @Override
    public void clearHex() {
        item.clearHex(stack);
    }

    @Override
    public int getPigment() {
        return item.getPigment(stack);
    }

    @Override
    public String getPigmentVariant() {
        return item.getPigmentVariant(stack);
    }

    @Override
    public UUID getPigmentOwner() {
        return item.getPigmentOwner(stack);
    }
}
