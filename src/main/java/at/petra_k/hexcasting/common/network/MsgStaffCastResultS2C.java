package at.petra_k.hexcasting.common.network;

import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.petra_k.hexcasting.common.casting.StaffCastExecutor;

/** Client feedback for one server-side staff VM step. */
public final class MsgStaffCastResultS2C implements PaucalMessage {
    private int handOrdinal;
    private int patternIndex;
    private int resolutionOrdinal;
    private int stackSize;
    private int parenDepth;
    private boolean escapeNext;
    private List<String> stackPreview;

    public MsgStaffCastResultS2C() {
        handOrdinal = EnumHand.MAIN_HAND.ordinal();
        patternIndex = -1;
        resolutionOrdinal = StaffCastExecutor.Resolution.ERRORED.ordinal();
        stackPreview = Collections.emptyList();
    }

    public MsgStaffCastResultS2C(EnumHand hand, boolean success, int stackSize) {
        this(hand, -1, success ? StaffCastExecutor.Resolution.EVALUATED
            : StaffCastExecutor.Resolution.ERRORED, stackSize,
            0, false, Collections.<String>emptyList());
    }

    public MsgStaffCastResultS2C(EnumHand hand, int patternIndex,
                                 StaffCastExecutor.CastOutcome outcome) {
        this(hand, patternIndex,
            outcome == null ? StaffCastExecutor.Resolution.ERRORED
                : outcome.getResolution(),
            outcome == null ? 0 : outcome.getStackSize(),
            outcome == null ? 0 : outcome.getParenDepth(),
            outcome != null && outcome.isEscapeNext(),
            outcome == null ? Collections.<String>emptyList()
                : outcome.getStackPreview());
    }

    private MsgStaffCastResultS2C(EnumHand hand, int patternIndex,
                                  StaffCastExecutor.Resolution resolution,
                                  int stackSize, int parenDepth,
                                  boolean escapeNext, List<String> stackPreview) {
        handOrdinal = hand == null ? EnumHand.MAIN_HAND.ordinal() : hand.ordinal();
        this.patternIndex = patternIndex;
        this.resolutionOrdinal = resolution == null
            ? StaffCastExecutor.Resolution.ERRORED.ordinal() : resolution.ordinal();
        this.stackSize = Math.max(0, stackSize);
        this.parenDepth = Math.max(0, parenDepth);
        this.escapeNext = escapeNext;
        this.stackPreview = new ArrayList<>(stackPreview == null
            ? Collections.<String>emptyList() : stackPreview);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        handOrdinal = buf.readByte();
        patternIndex = buf.readInt();
        resolutionOrdinal = buf.readByte();
        stackSize = Math.max(0, buf.readInt());
        parenDepth = Math.max(0, buf.readInt());
        escapeNext = buf.readBoolean();
        int previewCount = Math.max(0, Math.min(64, buf.readByte()));
        stackPreview = new ArrayList<>(previewCount);
        for (int i = 0; i < previewCount; i++) {
            stackPreview.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(handOrdinal);
        buf.writeInt(patternIndex);
        buf.writeByte(resolutionOrdinal);
        buf.writeInt(stackSize);
        buf.writeInt(parenDepth);
        buf.writeBoolean(escapeNext);
        int previewCount = Math.min(64, stackPreview == null ? 0 : stackPreview.size());
        buf.writeByte(previewCount);
        for (int i = 0; i < previewCount; i++) {
            ByteBufUtils.writeUTF8String(buf, stackPreview.get(i));
        }
    }

    @Override
    public void handleMessage(EntityPlayer player) {
        handleMessage(player, Side.CLIENT);
    }

    @Override
    public void handleMessage(EntityPlayer player, Side side) {
        if (side != Side.CLIENT || player == null) {
            return;
        }
        StaffCastExecutor.Resolution resolution = resolution();
        boolean success = resolution != StaffCastExecutor.Resolution.ERRORED
            && resolution != StaffCastExecutor.Resolution.INVALID;
        String message = success
            ? I18n.translateToLocalFormatted(
                "hexcasting.message.program_result", stackSize)
            : I18n.translateToLocal("hexcasting.message.cast_failed");
        try {
            Class<?> bridge = Class.forName(
                "at.petra_k.hexcasting.client.HexStaffClientSync");
            bridge.getMethod("showCastResult", String.class, int.class, int.class,
                List.class, int.class, boolean.class).invoke(null, message,
                patternIndex, resolutionOrdinal, stackPreview == null
                    ? Collections.<String>emptyList() : stackPreview,
                parenDepth, escapeNext);
        } catch (ReflectiveOperationException ignored) {
            // The client-only bridge is intentionally absent on a dedicated server.
        }
    }

    private StaffCastExecutor.Resolution resolution() {
        StaffCastExecutor.Resolution[] values = StaffCastExecutor.Resolution.values();
        return resolutionOrdinal < 0 || resolutionOrdinal >= values.length
            ? StaffCastExecutor.Resolution.ERRORED : values[resolutionOrdinal];
    }

    public static void register() {
        PaucalAPI.registerMessage(MsgStaffCastResultS2C.class, Side.CLIENT);
    }
}
