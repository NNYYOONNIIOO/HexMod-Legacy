package at.petra_k.hexcasting.common.capability;

import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import net.minecraft.nbt.NBTTagCompound;

/** Default persistent implementation of the player's casting state. */
public final class HexCastingData implements IHexCastingData {
    private static final String KEY_STACK = "casting_stack";

    private final CastingStack castingStack = new CastingStack();

    @Override
    public CastingStack getCastingStack() {
        return castingStack;
    }

    @Override
    public void clearCastingStack() {
        castingStack.clear();
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound result = new NBTTagCompound();
        result.setTag(KEY_STACK, castingStack.serialize());
        result.setTag("casting_state", castingStack.serializeState());
        return result;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
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
        castingStack.clear();
        if (nbt == null || !nbt.hasKey(KEY_STACK, 9)) {
            return;
        }
        try {
            CastingStack restored = CastingStack.deserialize(nbt.getTagList(KEY_STACK, 10));
            castingStack.restore(restored.snapshot());
        } catch (CastingException ignored) {
            // Corrupt or over-sized player data is discarded rather than
            // preventing the player from joining the world.
            castingStack.clear();
        }
    }
}
