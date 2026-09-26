package at.petra_k.hexcasting.common.network;

import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Client-side note playback for the beep action. */
public final class MsgBeepS2C implements PaucalMessage {
    private double posX;
    private double posY;
    private double posZ;
    private int note;
    private int instrument;

    public MsgBeepS2C() {
        this.note = 0;
        this.instrument = 0;
    }

    public MsgBeepS2C(double posX, double posY, double posZ, int note, int instrument) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.note = clamp(note, 0, 24);
        this.instrument = clamp(instrument, 0, 9);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        posX = buf.readDouble();
        posY = buf.readDouble();
        posZ = buf.readDouble();
        note = clamp(buf.readInt(), 0, 24);
        instrument = clamp(buf.readInt(), 0, 9);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(posX);
        buf.writeDouble(posY);
        buf.writeDouble(posZ);
        buf.writeInt(note);
        buf.writeInt(instrument);
    }

    @Override
    public void handleMessage(EntityPlayer player) {
        handleMessage(player, Side.CLIENT);
    }

    @Override
    public void handleMessage(EntityPlayer player, Side side) {
        if (side != Side.CLIENT || player == null || player.world == null) {
            return;
        }
        float pitch = (float) Math.pow(2.0D, (note - 12) / 12.0D);
        player.world.playSound(posX, posY, posZ, soundFor(instrument),
            SoundCategory.PLAYERS, 3.0F, pitch, false);
        player.world.spawnParticle(EnumParticleTypes.NOTE, posX, posY + 0.2D, posZ,
            note / 24.0D, 0.0D, 0.0D);
    }

    private static SoundEvent soundFor(int instrument) {
        switch (instrument) {
            case 1:
                return SoundEvents.BLOCK_NOTE_BASEDRUM;
            case 2:
                return SoundEvents.BLOCK_NOTE_SNARE;
            case 3:
                return SoundEvents.BLOCK_NOTE_HAT;
            case 4:
                return SoundEvents.BLOCK_NOTE_BASS;
            case 5:
                return SoundEvents.BLOCK_NOTE_FLUTE;
            case 6:
                return SoundEvents.BLOCK_NOTE_BELL;
            case 7:
                return SoundEvents.BLOCK_NOTE_GUITAR;
            case 8:
                return SoundEvents.BLOCK_NOTE_CHIME;
            case 9:
                return SoundEvents.BLOCK_NOTE_XYLOPHONE;
            default:
                return SoundEvents.BLOCK_NOTE_HARP;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void register() {
        PaucalAPI.registerMessage(MsgBeepS2C.class, Side.CLIENT);
    }
}
