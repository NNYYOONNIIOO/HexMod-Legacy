package at.petra_k.hexcasting.interop.crafttweaker;

import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.Mishap;

/** Transports checked VM Mishaps through ZenScript's unchecked callback API. */
final class CustomSpellFailure extends RuntimeException {
    private final CastingException castingException;

    private CustomSpellFailure(CastingException castingException) {
        super(castingException == null ? null : castingException.getMessage(),
            castingException);
        this.castingException = castingException;
    }

    static CustomSpellFailure from(CastingException exception) {
        return new CustomSpellFailure(exception);
    }

    static CustomSpellFailure mishap(String key, String detail) {
        return new CustomSpellFailure(Mishap.invalidValue(key, detail));
    }

    CastingException getCastingException() {
        return castingException;
    }
}
