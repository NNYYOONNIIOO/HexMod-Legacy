package at.petra_k.hexcasting.common.network;

import at.petra_k.hexcasting.common.item.ItemHexStaff;
import at.petra_k.hexcasting.common.casting.StaffCastExecutor;
import at.petra_k.hexcasting.common.world.PerWorldPatternData;
import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Server snapshot used to refresh the client staff GUI after it opens. */
public final class MsgStaffProgramS2C implements PaucalMessage {
    private int handOrdinal;
    private String staffInstanceId;
    private NBTTagList patternsData;
    private NBTTagList perWorldPatterns;
    private List<String> stackPreview;
    private int parenDepth;
    private boolean escapeNext;

    public MsgStaffProgramS2C() {
        this.handOrdinal = EnumHand.MAIN_HAND.ordinal();
        this.staffInstanceId = "";
        this.patternsData = new NBTTagList();
        this.perWorldPatterns = new NBTTagList();
        this.stackPreview = Collections.emptyList();
        this.parenDepth = 0;
        this.escapeNext = false;
    }

    public MsgStaffProgramS2C(EnumHand hand, NBTTagList patterns) {
        this(hand, null, patterns);
    }

    public MsgStaffProgramS2C(EnumHand hand, ItemStack staff, NBTTagList patterns) {
        this(hand, null, staff, patterns);
    }

    public MsgStaffProgramS2C(EnumHand hand, World world, ItemStack staff, NBTTagList patterns) {
        this.handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.staffInstanceId = ItemHexStaff.getInstanceId(staff);
        this.patternsData = patterns == null ? new NBTTagList() : patterns;
        this.perWorldPatterns = PerWorldPatternData.snapshot(world);
        StaffCastExecutor.CastOutcome state = StaffCastExecutor.getCurrentState(staff);
        this.stackPreview = state.getStackPreview();
        this.parenDepth = state.getParenDepth();
        this.escapeNext = state.isEscapeNext();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        handOrdinal = buf.readByte();
        NBTTagCompound payload = ByteBufUtils.readTag(buf);
        staffInstanceId = payload == null ? "" : payload.getString("staff_id");
        patternsData = payload == null ? new NBTTagList() : payload.getTagList("patterns", 10);
        perWorldPatterns = payload == null
            ? new NBTTagList() : payload.getTagList("per_world_patterns", 10);
        if (payload == null) {
            stackPreview = Collections.emptyList();
            parenDepth = 0;
            escapeNext = false;
        } else {
            NBTTagList preview = payload.getTagList("stack_preview", 8);
            List<String> decoded = new ArrayList<>();
            for (int i = 0; i < Math.min(64, preview.tagCount()); i++) {
                decoded.add(preview.getStringTagAt(i));
            }
            stackPreview = decoded;
            parenDepth = Math.max(0, payload.getInteger("paren_depth"));
            escapeNext = payload.getBoolean("escape_next");
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(handOrdinal);
        NBTTagCompound payload = new NBTTagCompound();
        payload.setString("staff_id", staffInstanceId == null ? "" : staffInstanceId);
        payload.setTag("patterns", patternsData == null ? new NBTTagList() : patternsData);
        payload.setTag("per_world_patterns",
            perWorldPatterns == null ? new NBTTagList() : perWorldPatterns);
        NBTTagList preview = new NBTTagList();
        if (stackPreview != null) {
            for (int i = 0; i < Math.min(64, stackPreview.size()); i++) {
                preview.appendTag(new NBTTagString(
                    stackPreview.get(i) == null ? "?" : stackPreview.get(i)));
            }
        }
        payload.setTag("stack_preview", preview);
        payload.setInteger("paren_depth", Math.max(0, parenDepth));
        payload.setBoolean("escape_next", escapeNext);
        ByteBufUtils.writeTag(buf, payload);
    }

    @Override
    public void handleMessage(EntityPlayer player) {
        handleMessage(player, Side.CLIENT);
    }

    @Override
    public void handleMessage(EntityPlayer player, Side side) {
        if (side != Side.CLIENT || player == null
            || handOrdinal < 0 || handOrdinal >= EnumHand.values().length) {
            return;
        }
        EnumHand hand = EnumHand.values()[handOrdinal];
        // The per-world table is useful even when this response arrives after
        // the held stack changed. Apply it before validating the staff so an
        // already-open GUI and ancient-scroll tooltips cannot miss the table.
        PerWorldPatternData.applyClientSnapshot(perWorldPatterns);
        ItemStack staff = player.getHeldItem(hand);
        if (!ItemHexStaff.isStaff(staff)) {
            return;
        }
        if (staffInstanceId != null && !staffInstanceId.isEmpty()) {
            String localInstanceId = ItemHexStaff.getInstanceId(staff);
            if (!localInstanceId.isEmpty() && !localInstanceId.equals(staffInstanceId)) {
                // This is a delayed response for a different physical staff.
                return;
            }
            ItemHexStaff.setInstanceId(staff, staffInstanceId);
        }
        ItemHexStaff.replaceProgram(staff, patternsData);
        if (patternsData == null || patternsData.tagCount() == 0) {
            // The authoritative empty snapshot is the final state of a
            // staff clear.  Use it as a second, ordered clearing signal so a
            // delayed orbit packet can never leave a stale rune ring around
            // the player after the dedicated clear packet was processed.
            try {
                Class<?> effects = Class.forName(
                    "at.petra_k.hexcasting.client.HexClientEffects");
                effects.getMethod("clearSpiralPatterns", java.util.UUID.class)
                    .invoke(null, player.getUniqueID());
            } catch (ReflectiveOperationException ignored) {
                // The client-only renderer is deliberately absent on a
                // dedicated server.
            }
        }
        try {
            Class<?> bridge = Class.forName(
                "at.petra_k.hexcasting.client.HexStaffClientSync");
            Class<?> effects = Class.forName(
                "at.petra_k.hexcasting.client.HexClientEffects");
            // The authoritative item snapshot is also a visual-state
            // resynchronization point. This covers a world reload where the
            // transient orbit packets arrived before the client player did.
            effects.getMethod("restoreOrbitPatterns").invoke(null);
            bridge.getMethod("refresh").invoke(null);
            bridge.getMethod("refreshCastingState", List.class, int.class, boolean.class)
                .invoke(null, stackPreview == null
                    ? Collections.<String>emptyList() : stackPreview,
                    parenDepth, escapeNext);
        } catch (ReflectiveOperationException ignored) {
            // The client-only bridge is intentionally absent on a dedicated server.
        }
    }

    public static void register() {
        PaucalAPI.registerMessage(MsgStaffProgramS2C.class, Side.CLIENT);
    }
}
