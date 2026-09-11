package at.petra_k.hexcasting.api.casting.action;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.iota.Iota;

import java.util.List;

/** A pure operation over the arguments collected by an {@link OperationAction}. */
@FunctionalInterface
public interface IotaOperation {
    Iota apply(List<Iota> arguments) throws CastingException;
}
