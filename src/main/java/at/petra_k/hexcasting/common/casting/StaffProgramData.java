package at.petra_k.hexcasting.common.casting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * Player-scoped spell-program storage for the 1.12.2 staff bridge.
 *
 * <p>Modern Hex keeps the staff-cast program in player-side casting state
 * rather than treating the held item as the VM.  Forge 1.12.2 has no direct
 * equivalent of that state, so this class uses the player's persisted entity
 * data as the durable common-side store.  The item NBT remains a compatibility
 * cache for the client GUI and old worlds.</p>
 */
public final class StaffProgramData {
    private static final String KEY_PATTERNS = "hexcasting_staff_patterns";

    private StaffProgramData() {
    }

    public static boolean hasPatterns(EntityPlayer player) {
        NBTTagCompound root = getRoot(player, false);
        return root != null && root.hasKey(KEY_PATTERNS, 9);
    }

    public static NBTTagList getPatterns(EntityPlayer player) {
        NBTTagCompound root = getRoot(player, false);
        if (root == null || !root.hasKey(KEY_PATTERNS, 9)) {
            return new NBTTagList();
        }
        return root.getTagList(KEY_PATTERNS, 10);
    }

    public static void replace(EntityPlayer player, NBTTagList patterns) {
        if (player == null) {
            return;
        }
        NBTTagCompound root = getRoot(player, true);
        if (patterns == null) {
            root.removeTag(KEY_PATTERNS);
        } else {
            root.setTag(KEY_PATTERNS, patterns);
        }
    }

    public static void clear(EntityPlayer player) {
        NBTTagCompound root = getRoot(player, false);
        if (root != null) {
            root.removeTag(KEY_PATTERNS);
        }
    }

    private static NBTTagCompound getRoot(EntityPlayer player, boolean create) {
        if (player == null) {
            return null;
        }
        NBTTagCompound entityData = player.getEntityData();
        if (!entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) {
            if (!create) {
                return null;
            }
            NBTTagCompound persisted = new NBTTagCompound();
            entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
            return persisted;
        }
        return entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }
}
