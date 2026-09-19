package at.petra_k.hexcasting.common.effect;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.item.ItemColorizer;
import at.petra_k.hexcasting.common.item.ItemPackagedSpell;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;

import java.util.UUID;

/**
 * Immutable pigment snapshot used by server feedback, blocks, and particles.
 *
 * <p>The modern implementation carries a frozen pigment rather than just an
 * RGB value.  Keeping the variant and owner here is important for animated
 * pigments: a constructed block must continue sampling the same gradient
 * after the staff is put away.</p>
 */
public final class HexPigmentSource {
    public static final int DEFAULT_COLOR = 0xAA66FF;
    public static final String DEFAULT_VARIANT = "default_colorizer";
    public static final UUID NIL_UUID = new UUID(0L, 0L);

    private final int color;
    private final String variant;
    private final UUID owner;

    private HexPigmentSource(int color, String variant, UUID owner) {
        this.color = color & 0xFFFFFF;
        this.variant = variant == null || variant.isEmpty()
            ? DEFAULT_VARIANT : variant;
        this.owner = owner == null ? NIL_UUID : owner;
    }

    public static HexPigmentSource of(int color, String variant, UUID owner) {
        return new HexPigmentSource(color, variant, owner);
    }

    public static HexPigmentSource defaultSource() {
        return new HexPigmentSource(DEFAULT_COLOR, DEFAULT_VARIANT, NIL_UUID);
    }

    /** Snapshot a player's persistent internal pigment. */
    public static HexPigmentSource fromData(IHexCastingData data) {
        return data == null ? null : new HexPigmentSource(data.getPigment(),
            data.getPigmentVariant(), data.getPigmentOwner());
    }

    /**
     * Snapshot pigment NBT stored on a casting item.  The packaged spell has
     * its own frozen pigment tag, while staffs and foci use ItemColorizer's
     * compatibility tags.
     */
    public static HexPigmentSource fromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        int color = ItemColorizer.getColor(stack);
        if (color >= 0) {
            return new HexPigmentSource(color, ItemColorizer.getVariant(stack),
                ItemColorizer.getOwner(stack));
        }
        if (!(stack.getItem() instanceof ItemPackagedSpell)) {
            return null;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(ItemPackagedSpell.TAG_PIGMENT, 10)) {
            return null;
        }
        NBTTagCompound pigment = tag.getCompoundTag(ItemPackagedSpell.TAG_PIGMENT);
        int packagedColor = pigment.hasKey("color", 3)
            ? pigment.getInteger("color") : DEFAULT_COLOR;
        String variant = pigment.hasKey("variant", 8)
            ? pigment.getString("variant") : DEFAULT_VARIANT;
        UUID owner = NIL_UUID;
        if (pigment.hasKey("owner", 8)) {
            try {
                owner = UUID.fromString(pigment.getString("owner"));
            } catch (IllegalArgumentException ignored) {
                // Keep the nil owner for malformed or legacy package data.
            }
        }
        return new HexPigmentSource(packagedColor, variant, owner);
    }

    /** Resolve the item pigment first, then the player's persistent pigment. */
    public static HexPigmentSource resolve(EntityPlayer player, EnumHand preferredHand) {
        if (player == null) {
            return defaultSource();
        }
        EnumHand preferred = preferredHand == null ? EnumHand.MAIN_HAND : preferredHand;
        HexPigmentSource source = fromStack(player.getHeldItem(preferred));
        if (source != null) {
            return source;
        }
        EnumHand other = preferred == EnumHand.MAIN_HAND
            ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        source = fromStack(player.getHeldItem(other));
        if (source != null) {
            return source;
        }
        IHexCastingData data = HexCapabilities.CASTING_DATA == null
            ? null : player.getCapability(HexCapabilities.CASTING_DATA, null);
        source = fromData(data);
        return source == null ? defaultSource() : source;
    }

    /** Sample the frozen pigment at a world/render position. */
    public int sample(float time, double x, double y, double z) {
        return HexPigmentColors.color(variant, color, owner, time, x, y, z);
    }

    public int getColor() {
        return color;
    }

    public String getVariant() {
        return variant;
    }

    public UUID getOwner() {
        return owner;
    }
}
