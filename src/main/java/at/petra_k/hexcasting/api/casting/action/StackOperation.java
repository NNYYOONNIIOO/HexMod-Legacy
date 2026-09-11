package at.petra_k.hexcasting.api.casting.action;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.Iota;

import java.util.List;

/** A pure operation that may return zero or more Iotas. */
@FunctionalInterface
public interface StackOperation {
    List<Iota> apply(List<Iota> arguments) throws CastingException;
}
