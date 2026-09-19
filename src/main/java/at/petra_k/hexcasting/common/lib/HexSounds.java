package at.petra_k.hexcasting.common.lib;

import at.petra_k.hexcasting.api.HexAPI;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hex Casting's named sound events.
 *
 * <p>The 1.20.1 implementation registers these through the platform sound
 * registry.  Forge 1.12.2 uses the registry event bus instead, so the events
 * are created once here and registered before any gameplay code can reference
 * them.</p>
 */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class HexSounds {
    private static final Map<ResourceLocation, SoundEvent> SOUNDS =
        new LinkedHashMap<>();

    public static final SoundEvent START_PATTERN = sound("casting.pattern.start");
    public static final SoundEvent ADD_TO_PATTERN = sound("casting.pattern.add_segment");

    public static final SoundEvent CASTING_AMBIANCE = sound("casting.ambiance");

    public static final SoundEvent CAST_NORMAL = sound("casting.cast.normal");
    public static final SoundEvent CAST_SPELL = sound("casting.cast.spell");
    public static final SoundEvent CAST_HERMES = sound("casting.cast.hermes");
    public static final SoundEvent CAST_THOTH = sound("casting.cast.thoth");
    public static final SoundEvent CAST_FAILURE = sound("casting.cast.fail");

    public static final SoundEvent ABACUS = sound("abacus");
    public static final SoundEvent ABACUS_SHAKE = sound("abacus.shake");

    public static final SoundEvent STAFF_RESET = sound("staff.reset");

    public static final SoundEvent SPELL_CIRCLE_FIND_BLOCK =
        sound("spellcircle.find_block");
    public static final SoundEvent SPELL_CIRCLE_FAIL = sound("spellcircle.fail");

    public static final SoundEvent SCROLL_DUST = sound("scroll.dust");
    public static final SoundEvent SCROLL_SCRIBBLE = sound("scroll.scribble");

    public static final SoundEvent IMPETUS_LOOK_TICK = sound("impetus.fletcher.tick");
    public static final SoundEvent IMPETUS_REDSTONE_DING =
        sound("impetus.redstone.register");
    public static final SoundEvent IMPETUS_REDSTONE_CLEAR =
        sound("impetus.redstone.clear");

    public static final SoundEvent READ_LORE_FRAGMENT = sound("lore_fragment.read");

    public static final SoundEvent FLIGHT_AMBIENCE = sound("flight.ambience");
    public static final SoundEvent FLIGHT_FINISH = sound("flight.finish");

    private HexSounds() {
    }

    private static SoundEvent sound(String name) {
        ResourceLocation id = HexAPI.modLoc(name);
        SoundEvent event = new SoundEvent(id).setRegistryName(id);
        if (SOUNDS.put(id, event) != null) {
            throw new IllegalArgumentException("Duplicate sound id " + id);
        }
        return event;
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        SOUNDS.values().forEach(event.getRegistry()::register);
    }
}
