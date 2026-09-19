package at.petra_k.hexcasting.api.casting.eval.sideeffects;

import net.minecraft.util.SoundEvent;

/** The sound selected for a casting evaluation and its precedence. */
public final class EvalSound {
    private final SoundEvent sound;
    private final int priority;

    public EvalSound(SoundEvent sound, int priority) {
        this.sound = sound;
        this.priority = priority;
    }

    public SoundEvent getSound() {
        return sound;
    }

    public int getPriority() {
        return priority;
    }

    /** Return whichever sound has the higher precedence. */
    public EvalSound greaterOf(EvalSound other) {
        if (other == null || priority >= other.priority) {
            return this;
        }
        return other;
    }
}
