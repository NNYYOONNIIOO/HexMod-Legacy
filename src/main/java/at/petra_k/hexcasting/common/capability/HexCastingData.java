package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import net.minecraft.nbt.NBTTagCompound;
import at.petra_k.hexcasting.api.misc.MediaConstants;

/** Default persistent implementation of the player's casting state. */
public final class HexCastingData implements IHexCastingData {
    private static final String KEY_STACK = "casting_stack";
    private static final String KEY_MEDIA = "media";
    private static final String KEY_PIGMENT = "pigment";
    private static final int DEFAULT_PIGMENT = 0xAA66FF;

    private final CastingStack castingStack = new CastingStack();
    private long media;
    private int pigment = DEFAULT_PIGMENT;

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
        result.setTag("casting_state", castingStack.serializeState());
        return result;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        castingStack.clear();
        media = 0L;
        pigment = DEFAULT_PIGMENT;
        if (nbt == null) return;
        media = clampMedia(nbt.getLong(KEY_MEDIA));
        if (nbt.hasKey(KEY_PIGMENT, 3)) {
            pigment = nbt.getInteger(KEY_PIGMENT) & 0xFFFFFF;
        }
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
