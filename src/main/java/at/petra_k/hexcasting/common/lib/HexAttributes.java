package at.petra_k.hexcasting.common.lib;

import at.petra_k.hexcasting.HexCasting;
import at.petra_k.hexcasting.common.config.HexConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;

/** The player attributes used by Hex's casting environment and client UI. */
@Mod.EventBusSubscriber(modid = HexCasting.MOD_ID)
public final class HexAttributes {
    public static final IAttribute GRID_ZOOM = attribute("grid_zoom", 1.0D, 0.5D, 4.0D);
    public static final IAttribute SCRY_SIGHT = attribute("scry_sight", 0.0D, 0.0D, 1.0D);
    public static final IAttribute FEEBLE_MIND = attribute("feeble_mind", 0.0D, 0.0D, 1.0D);
    public static final IAttribute MEDIA_CONSUMPTION =
        attribute("media_consumption", 1.0D, 0.0D, Double.MAX_VALUE);
    public static final IAttribute AMBIT_RADIUS =
        attribute("ambit_radius", 32.0D, 0.0D, Double.MAX_VALUE);
    public static final IAttribute SENTINEL_RADIUS =
        attribute("sentinel_radius", 16.0D, 0.0D, Double.MAX_VALUE);

    public static final UUID LENS_ZOOM_UUID =
        UUID.fromString("59d739b8-d419-45f7-a4ea-0efee0e3adf5");
    public static final UUID LENS_SIGHT_UUID =
        UUID.fromString("e2e6e5d4-f978-4c11-8fdc-82a5af83385c");

    private HexAttributes() {
    }

    private static IAttribute attribute(String id, double base, double min, double max) {
        return new RangedAttribute(null, HexCasting.MOD_ID + ".attributes." + id,
            base, min, max).setShouldWatch(true);
    }

    @SubscribeEvent
    public static void onEntityConstructing(EntityEvent.EntityConstructing event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }
        AbstractAttributeMap attributes = ((EntityLivingBase) event.getEntity()).getAttributeMap();
        register(attributes, GRID_ZOOM, HexConfig.gridZoom());
        register(attributes, SCRY_SIGHT, HexConfig.scrySight());
        register(attributes, FEEBLE_MIND, HexConfig.feebleMind());
        register(attributes, MEDIA_CONSUMPTION, HexConfig.mediaConsumption());
        register(attributes, AMBIT_RADIUS, HexConfig.ambitRadius());
        register(attributes, SENTINEL_RADIUS, HexConfig.sentinelRadius());
    }

    private static void register(AbstractAttributeMap attributes, IAttribute attribute,
                                 double base) {
        if (attributes.getAttributeInstance(attribute) == null) {
            IAttributeInstance instance = attributes.registerAttribute(attribute);
            instance.setBaseValue(base);
        }
    }

    /** Apply the lens modifiers to the same attributes used by the UI. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player == null) {
            return;
        }
        EntityPlayer player = event.player;
        AbstractAttributeMap attributes = player.getAttributeMap();
        register(attributes, GRID_ZOOM, HexConfig.gridZoom());
        register(attributes, SCRY_SIGHT, HexConfig.scrySight());
        IAttributeInstance zoom = attributes.getAttributeInstance(GRID_ZOOM);
        IAttributeInstance sight = attributes.getAttributeInstance(SCRY_SIGHT);
        boolean equipped = at.petra_k.hexcasting.common.item.ItemScryingLens.isEquipped(player);
        updateModifier(zoom, new AttributeModifier(LENS_ZOOM_UUID,
            "Scrying Lens Zoom", 0.33D, 2), equipped);
        updateModifier(sight, new AttributeModifier(LENS_SIGHT_UUID,
            "Scrying Lens Sight", 1.0D, 0), equipped);
    }

    private static void updateModifier(IAttributeInstance instance, AttributeModifier modifier,
                                       boolean present) {
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(modifier.getID());
        if (present && existing == null) {
            instance.applyModifier(modifier);
        } else if (!present && existing != null) {
            instance.removeModifier(existing);
        }
    }
}
