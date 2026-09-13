package at.petra_k.hexcasting.common.item;

/** A packaged spell that is consumed after a successful cast. */
public final class ItemCypher extends ItemPackagedSpell {
    @Override
    protected boolean consumeOnUse() {
        return true;
    }
}
