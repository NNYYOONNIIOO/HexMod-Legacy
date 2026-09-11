package at.petra_k.hexcasting.api.casting.action;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;

/** A single operation that consumes and/or produces Iotas. */
@FunctionalInterface
public interface HexAction {
    void execute(CastingStack stack) throws CastingException;
}
