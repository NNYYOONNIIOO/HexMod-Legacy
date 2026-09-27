package at.petra_k.hexcasting.api.addldata;

import java.util.UUID;

/** Capability-facing colour provider for a pigment item. */
public interface ADPigment {
    int getColor(UUID owner, float time, double x, double y, double z);
}
