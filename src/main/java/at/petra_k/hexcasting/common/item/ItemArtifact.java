package at.petra_k.hexcasting.common.item;

/** A reusable packaged spell container that may draw media from inventory. */
public final class ItemArtifact extends ItemPackagedSpell {
    @Override
    protected boolean canDrawMediaFromInventory() {
        return true;
    }

    @Override
    protected int cooldown() {
        return 3;
    }
}
