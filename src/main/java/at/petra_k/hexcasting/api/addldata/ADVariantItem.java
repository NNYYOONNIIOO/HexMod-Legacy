package at.petra_k.hexcasting.api.addldata;

/** Capability-facing view of an item's visual variant. */
public interface ADVariantItem {
    int numVariants();

    int getVariant();

    void setVariant(int variant);
}
