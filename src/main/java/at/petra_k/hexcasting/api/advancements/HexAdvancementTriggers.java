package at.petra_k.hexcasting.api.advancements;

import net.minecraft.advancements.CriteriaTriggers;

/** Registration and access point for Hex's custom advancement criteria. */
public final class HexAdvancementTriggers {
    public static final SpendMediaTrigger SPEND_MEDIA_TRIGGER = new SpendMediaTrigger();
    public static final OvercastTrigger OVERCAST_TRIGGER = new OvercastTrigger();
    public static final FailToCastGreatSpellTrigger FAIL_GREAT_SPELL_TRIGGER =
        new FailToCastGreatSpellTrigger();
    private static boolean registered;

    private HexAdvancementTriggers() {
    }

    public static synchronized void registerTriggers() {
        if (registered) {
            return;
        }
        CriteriaTriggers.register(SPEND_MEDIA_TRIGGER);
        CriteriaTriggers.register(OVERCAST_TRIGGER);
        CriteriaTriggers.register(FAIL_GREAT_SPELL_TRIGGER);
        registered = true;
    }
}
