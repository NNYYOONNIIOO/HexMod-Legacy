package at.petra_k.hexcasting.common.casting;

import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Portable implementation of the print action.
 *
 * <p>The 1.20.1 action emits a player-facing side effect. The 1.12.2 core
 * evaluator currently has no caster/world context, so this first port keeps
 * the stack unchanged and emits the rendered Iota to the mod log. A later
 * environment adapter can replace this sink without changing the pattern or
 * stack semantics.</p>
 */
public final class PrintAction implements HexAction {
    private static final Logger LOGGER = LogManager.getLogger("HexCasting");

    @Override
    public void execute(CastingStack stack) throws CastingException {
        Iota value = stack.peek();
        LOGGER.info("[Hex Casting] {}", value.display());
    }
}
