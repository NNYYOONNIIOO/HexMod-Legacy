package at.petra_k.hexcasting.common.lib.hex;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.sideeffects.EvalSound;
import at.petra_k.hexcasting.common.lib.HexSounds;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Sound precedence used by the casting VM. */
public final class HexEvalSounds {
    public static final int NORMAL_PRIORITY = 0;
    public static final int SPELL_PRIORITY = 1000;
    public static final int HERMES_PRIORITY = 2000;
    public static final int THOTH_PRIORITY = 2500;
    public static final int MUTE_PRIORITY = 3000;
    public static final int MISHAP_PRIORITY = 4000;

    public static final EvalSound NOTHING =
        new EvalSound(null, Integer.MIN_VALUE);
    public static final EvalSound NORMAL_EXECUTE =
        new EvalSound(HexSounds.CAST_NORMAL, NORMAL_PRIORITY);
    public static final EvalSound SPELL =
        new EvalSound(HexSounds.CAST_SPELL, SPELL_PRIORITY);
    public static final EvalSound HERMES =
        new EvalSound(HexSounds.CAST_HERMES, HERMES_PRIORITY);
    public static final EvalSound THOTH =
        new EvalSound(HexSounds.CAST_THOTH, THOTH_PRIORITY);
    public static final EvalSound MUTE =
        new EvalSound(null, MUTE_PRIORITY);
    public static final EvalSound MISHAP =
        new EvalSound(HexSounds.CAST_FAILURE, MISHAP_PRIORITY);

    private static final Set<String> SPELL_ACTIONS = new HashSet<>(Arrays.asList(
        "print", "ignite", "extinguish", "add_motion", "beep",
        "summon_rain", "dispel_rain", "explode", "explode/fire",
        "break_block", "raycast", "raycast_axis", "raycast_entity",
        "conjure_block", "conjure_light", "flight", "flight/can_fly",
        "flight/range", "flight/time", "colorize", "brainsweep",
        "teleport/great", "craft/battery", "craft/cypher", "craft/trinket",
        "craft/artifact", "edify", "erase", "place_block", "bonemeal",
        "lightning", "blink", "create_water", "create_lava", "destroy_water",
        "potion_absorption", "potion_haste", "potion_levitation",
        "potion_night_vision", "potion_poison", "potion_regeneration",
        "potion_slowness", "potion_strength", "potion_weakness",
        "potion_wither", "thanatos"
    ));

    private HexEvalSounds() {
    }

    /**
     * Resolve the closest sound equivalent for the Java port's action table.
     * Actions in the modern implementation carry this value in OperationResult;
     * the 1.12.2 action API predates that field, so the stable action id is the
     * compatibility boundary.
     */
    public static EvalSound forAction(HexAction action, ResourceLocation id) {
        if (id == null || action == null) {
            return NORMAL_EXECUTE;
        }
        String path = id.getResourceDomain().equals(HexAPI.MOD_ID)
            ? id.getResourcePath() : id.toString();
        if ("eval".equals(path) || "eval/cc".equals(path)) {
            return HERMES;
        }
        if ("for_each".equals(path)) {
            return THOTH;
        }
        if ("halt".equals(path)) {
            return SPELL;
        }
        return SPELL_ACTIONS.contains(path) ? SPELL : NORMAL_EXECUTE;
    }
}
