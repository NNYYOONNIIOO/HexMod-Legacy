package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import net.minecraft.nbt.NBTTagCompound;
import at.petra_k.hexcasting.api.misc.MediaConstants;

import java.util.UUID;

/** Default persistent implementation of the player's casting state. */
public final class HexCastingData implements IHexCastingData {
    private static final String KEY_STACK = "casting_stack";
    private static final String KEY_MEDIA = "media";
    private static final String KEY_PIGMENT = "pigment";
    private static final String KEY_PIGMENT_INTERNALIZED = "pigment_internalized";
    private static final String KEY_FLIGHT_TICKS = "flight_ticks";
    private static final String KEY_FLIGHT_ACTIVE = "flight_active";
    private static final String KEY_FLIGHT_DIMENSION = "flight_dimension";
    private static final String KEY_FLIGHT_ORIGIN_X = "flight_origin_x";
    private static final String KEY_FLIGHT_ORIGIN_Y = "flight_origin_y";
    private static final String KEY_FLIGHT_ORIGIN_Z = "flight_origin_z";
    private static final String KEY_FLIGHT_RADIUS = "flight_radius";
    private static final String KEY_ALTIORA_TICKS = "altiora_ticks";
    private static final int DEFAULT_PIGMENT = 0xAA66FF;
    private static final String DEFAULT_PIGMENT_VARIANT = "default_colorizer";

    private final CastingStack castingStack = new CastingStack();
    private long media;
    private int pigment = DEFAULT_PIGMENT;
    private String pigmentVariant = DEFAULT_PIGMENT_VARIANT;
    private UUID pigmentOwner = new UUID(0L, 0L);
    private boolean pigmentInternalized;
    private int flightTicks;
    private boolean flightActive;
    private int flightDimension;
    private double flightOriginX;
    private double flightOriginY;
    private double flightOriginZ;
    private double flightRadius = -1.0D;
    private int altioraTicks;
    private boolean altioraActive;

    @Override
    public CastingStack getCastingStack() {
        return castingStack;
    }

    @Override
    public void clearCastingStack() {
        castingStack.clear();
    }

    @Override
    public long getMedia() {
        return media;
    }

    @Override
    public long getMaxMedia() {
        return MediaConstants.DEFAULT_PLAYER_MAX_MEDIA;
    }

    @Override
    public void setMedia(long media) {
        this.media = clampMedia(media);
    }

    @Override
    public int getPigment() {
        return pigment;
    }

    @Override
    public void setPigment(int pigment) {
        this.pigment = pigment & 0xFFFFFF;
    }

    @Override
    public String getPigmentVariant() {
        return pigmentVariant;
    }

    @Override
    public UUID getPigmentOwner() {
        return pigmentOwner;
    }

    @Override
    public void setPigmentVariant(String variant, UUID owner) {
        pigmentVariant = variant == null || variant.isEmpty()
            ? DEFAULT_PIGMENT_VARIANT : variant;
        pigmentOwner = owner == null ? new UUID(0L, 0L) : owner;
    }

    @Override
    public boolean hasInternalizedPigment() {
        return pigmentInternalized;
    }

    @Override
    public void setInternalizedPigment(boolean internalized) {
        pigmentInternalized = internalized;
    }

    @Override
    public int getFlightTicks() {
        return flightTicks;
    }

    @Override
    public void setFlightTicks(int ticks) {
        // -1 is the modern sentinel for a flight with no time limit.
        flightTicks = Math.max(-1, ticks);
    }

    @Override
    public boolean isFlightActive() {
        return flightActive;
    }

    @Override
    public void setFlightActive(boolean active) {
        flightActive = active;
        if (!active) {
            flightTicks = 0;
            flightRadius = -1.0D;
        }
    }

    @Override
    public int getFlightDimension() {
        return flightDimension;
    }

    @Override
    public void setFlightDimension(int dimension) {
        flightDimension = dimension;
    }

    @Override
    public double getFlightOriginX() {
        return flightOriginX;
    }

    @Override
    public double getFlightOriginY() {
        return flightOriginY;
    }

    @Override
    public double getFlightOriginZ() {
        return flightOriginZ;
    }

    @Override
    public void setFlightOrigin(double x, double y, double z) {
        flightOriginX = x;
        flightOriginY = y;
        flightOriginZ = z;
    }

    @Override
    public double getFlightRadius() {
        return flightRadius;
    }

    @Override
    public void setFlightRadius(double radius) {
        flightRadius = radius;
    }

    @Override
    public int getAltioraTicks() {
        return altioraTicks;
    }

    @Override
    public void setAltioraTicks(int ticks) {
        altioraTicks = Math.max(0, ticks);
    }

    @Override
    public boolean isAltioraActive() {
        return altioraActive;
    }

    @Override
    public void setAltioraActive(boolean active) {
        altioraActive = active;
        if (!active) {
            altioraTicks = 0;
        }
    }

    @Override
    public boolean canRecharge() {
        return true;
    }

    @Override
    public boolean canProvide() {
        return true;
    }

    @Override
    public int getConsumptionPriority() {
        return 4000;
    }

    @Override
    public boolean canConstructBattery() {
        return false;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound result = new NBTTagCompound();
        result.setTag(KEY_STACK, castingStack.serialize());
        result.setLong(KEY_MEDIA, media);
        result.setInteger(KEY_PIGMENT, pigment);
        result.setString("pigment_variant", pigmentVariant);
        result.setString("pigment_owner", pigmentOwner.toString());
        result.setBoolean(KEY_PIGMENT_INTERNALIZED, pigmentInternalized);
        result.setInteger(KEY_FLIGHT_TICKS, flightTicks);
        result.setBoolean(KEY_FLIGHT_ACTIVE, flightActive);
        result.setInteger(KEY_FLIGHT_DIMENSION, flightDimension);
        result.setDouble(KEY_FLIGHT_ORIGIN_X, flightOriginX);
        result.setDouble(KEY_FLIGHT_ORIGIN_Y, flightOriginY);
        result.setDouble(KEY_FLIGHT_ORIGIN_Z, flightOriginZ);
        result.setDouble(KEY_FLIGHT_RADIUS, flightRadius);
        result.setInteger(KEY_ALTIORA_TICKS, altioraTicks);
        result.setBoolean("altiora_active", altioraActive);
        result.setTag("casting_state", castingStack.serializeState());
        return result;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        castingStack.clear();
        media = 0L;
        pigment = DEFAULT_PIGMENT;
        pigmentVariant = DEFAULT_PIGMENT_VARIANT;
        pigmentOwner = new UUID(0L, 0L);
        pigmentInternalized = false;
        flightTicks = 0;
        flightActive = false;
        flightDimension = 0;
        flightOriginX = 0.0D;
        flightOriginY = 0.0D;
        flightOriginZ = 0.0D;
        flightRadius = -1.0D;
        if (nbt == null) return;
        media = clampMedia(nbt.getLong(KEY_MEDIA));
        if (nbt.hasKey(KEY_PIGMENT, 3)) {
            pigment = nbt.getInteger(KEY_PIGMENT) & 0xFFFFFF;
        }
        pigmentVariant = nbt.hasKey("pigment_variant", 8)
            ? nbt.getString("pigment_variant") : DEFAULT_PIGMENT_VARIANT;
        if (pigmentVariant.isEmpty()) {
            pigmentVariant = DEFAULT_PIGMENT_VARIANT;
        }
        pigmentOwner = new UUID(0L, 0L);
        if (nbt.hasKey("pigment_owner", 8)) {
            try {
                pigmentOwner = UUID.fromString(nbt.getString("pigment_owner"));
            } catch (IllegalArgumentException ignored) {
                // Keep the default owner for malformed legacy data.
            }
        }
        if (nbt.hasKey(KEY_PIGMENT_INTERNALIZED, 1)) {
            pigmentInternalized = nbt.getBoolean(KEY_PIGMENT_INTERNALIZED);
        } else {
            // Older saves did not carry an explicit marker. Preserve their
            // non-default pigments instead of silently treating them as an
            // untouched capability.
            pigmentInternalized = pigment != DEFAULT_PIGMENT
                || !DEFAULT_PIGMENT_VARIANT.equals(pigmentVariant)
                || !new UUID(0L, 0L).equals(pigmentOwner);
        }
        flightTicks = Math.max(-1, nbt.getInteger(KEY_FLIGHT_TICKS));
        flightActive = nbt.getBoolean(KEY_FLIGHT_ACTIVE)
            || flightTicks != 0;
        flightDimension = nbt.hasKey(KEY_FLIGHT_DIMENSION, 3)
            ? nbt.getInteger(KEY_FLIGHT_DIMENSION) : 0;
        flightOriginX = nbt.hasKey(KEY_FLIGHT_ORIGIN_X, 6)
            ? nbt.getDouble(KEY_FLIGHT_ORIGIN_X) : 0.0D;
        flightOriginY = nbt.hasKey(KEY_FLIGHT_ORIGIN_Y, 6)
            ? nbt.getDouble(KEY_FLIGHT_ORIGIN_Y) : 0.0D;
        flightOriginZ = nbt.hasKey(KEY_FLIGHT_ORIGIN_Z, 6)
            ? nbt.getDouble(KEY_FLIGHT_ORIGIN_Z) : 0.0D;
        flightRadius = nbt.hasKey(KEY_FLIGHT_RADIUS, 6)
            ? nbt.getDouble(KEY_FLIGHT_RADIUS) : -1.0D;
        if (!Double.isFinite(flightOriginX) || !Double.isFinite(flightOriginY)
            || !Double.isFinite(flightOriginZ)
            || !Double.isFinite(flightRadius)) {
            flightActive = false;
            flightTicks = 0;
            flightRadius = -1.0D;
        }
        altioraTicks = Math.max(0, nbt.getInteger(KEY_ALTIORA_TICKS));
        altioraActive = nbt.getBoolean("altiora_active")
            || nbt.hasKey(KEY_ALTIORA_TICKS, 3) && altioraTicks > 0;
        if (nbt.hasKey("casting_state", 10)) {
            try {
                CastingStack loaded = CastingStack.deserializeState(nbt.getCompoundTag("casting_state"));
                castingStack.restore(loaded.snapshot());
                castingStack.writeLocal(loaded.readLocal());
                return;
            } catch (Exception ignored) {
                castingStack.clear();
                return;
            }
        }
        if (!nbt.hasKey(KEY_STACK, 9)) return;
        try {
            CastingStack restored = CastingStack.deserialize(nbt.getTagList(KEY_STACK, 10));
            castingStack.restore(restored.snapshot());
        } catch (CastingException ignored) {
            castingStack.clear();
        }
    }
    private long clampMedia(long value) {
        return Math.max(0L, Math.min(value, getMaxMedia()));
    }

}
