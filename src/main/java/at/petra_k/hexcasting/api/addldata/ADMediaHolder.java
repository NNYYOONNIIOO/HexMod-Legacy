package at.petra_k.hexcasting.api.addldata;

/** A holder for Hex Casting media, measured in raw dust units. */
public interface ADMediaHolder {
    long QUENCHED_ALLAY_PRIORITY = 800L;
    long QUENCHED_SHARD_PRIORITY = 900L;
    long CHARGED_AMETHYST_PRIORITY = 1000L;
    long AMETHYST_SHARD_PRIORITY = 2000L;
    long AMETHYST_DUST_PRIORITY = 3000L;
    long BATTERY_PRIORITY = 4000L;

    long getMedia();
    long getMaxMedia();
    void setMedia(long media);
    boolean canRecharge();
    boolean canProvide();
    int getConsumptionPriority();
    boolean canConstructBattery();

    default long withdrawMedia(long cost, boolean simulate) {
        long available = Math.max(0L, getMedia());
        long requested = cost < 0L ? available : Math.max(0L, cost);
        long extracted = Math.min(requested, available);
        if (!simulate && extracted > 0L) setMedia(available - extracted);
        return extracted;
    }

    default long insertMedia(long amount, boolean simulate) {
        long current = Math.max(0L, getMedia());
        long capacity = Math.max(0L, getMaxMedia());
        long empty = capacity - current;
        if (empty <= 0L) return 0L;
        long requested = amount < 0L ? empty : Math.max(0L, amount);
        long inserting = Math.min(requested, empty);
        if (!simulate && inserting > 0L) setMedia(current + inserting);
        return inserting;
    }
}

