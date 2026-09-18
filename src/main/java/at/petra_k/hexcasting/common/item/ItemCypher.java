package at.petra_k.hexcasting.common.item;

/** A packaged spell that breaks after its captured media is exhausted. */
public final class ItemCypher extends ItemPackagedSpell {
    @Override
    protected boolean breakAfterDepletion() {
        return true;
    }

    @Override
    protected int cooldown() {
        return 8;
    }
}
