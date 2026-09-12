package at.petra_k.hexcasting.common.casting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;

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
    private static final String KEY_PATTERNS_PREFIX = "hexcasting_staff_patterns_";
    private static final String LEGACY_KEY_PATTERNS = "hexcasting_staff_patterns";

    private StaffProgramData() {
    }

    public static boolean hasPatterns(EntityPlayer player, EnumHand hand) {
        NBTTagCompound root = getRoot(player, false);
        return root != null && (root.hasKey(keyFor(hand), 9)
            || (isMain(hand) && root.hasKey(LEGACY_KEY_PATTERNS, 9)));
    }

    public static NBTTagList getPatterns(EntityPlayer player, EnumHand hand) {
        NBTTagCompound root = getRoot(player, false);
        if (root == null) {
            return new NBTTagList();
        }
        String key = keyFor(hand);
        if (root.hasKey(key, 9)) {
            return root.getTagList(key, 10);
        }
        if (isMain(hand) && root.hasKey(LEGACY_KEY_PATTERNS, 9)) {
            return root.getTagList(LEGACY_KEY_PATTERNS, 10);
        }
        return new NBTTagList();
    }

    public static void replace(EntityPlayer player, EnumHand hand, NBTTagList patterns) {
        if (player == null) {
            return;
        }
        NBTTagCompound root = getRoot(player, true);
        String key = keyFor(hand);
        if (patterns == null) {
            root.removeTag(key);
        } else {
            root.setTag(key, patterns);
        }
        if (isMain(hand)) {
            // Remove the pre-hand-split key after the first write so old data
            // is migrated without leaving two competing sources of truth.
            root.removeTag(LEGACY_KEY_PATTERNS);
        }
    }

    public static void clear(EntityPlayer player, EnumHand hand) {
        NBTTagCompound root = getRoot(player, false);
        if (root != null) {
            root.removeTag(keyFor(hand));
            if (isMain(hand)) {
                root.removeTag(LEGACY_KEY_PATTERNS);
            }
        }
    }

    private static String keyFor(EnumHand hand) {
        return KEY_PATTERNS_PREFIX + (isMain(hand) ? "main" : "off");
    }

    private static boolean isMain(EnumHand hand) {
        return hand == null || hand == EnumHand.MAIN_HAND;
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
