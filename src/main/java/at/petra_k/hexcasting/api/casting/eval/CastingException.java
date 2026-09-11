package at.petra_k.hexcasting.api.casting.eval;

/** A controlled failure while evaluating a Hex spell. */
public class CastingException extends Exception {
    public CastingException(String message) {
        super(message);
    }

    public CastingException(String message, Throwable cause) {
        super(message, cause);
    }
}
