package at.petra_k.hexcasting.common.network;

import at.petra_k.hexcasting.common.world.PerWorldPatternData;
import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Authoritative client copy of Hex's per-world great-spell stroke table.
 *
 * <p>This is deliberately independent of the staff program packet. Ancient
 * scrolls and the staff GUI both need the table, including when no staff is
 * currently held in the synchronized hand.</p>
 */
public final class MsgPerWorldPatternsS2C implements PaucalMessage {
    private NBTTagList patterns;

    public MsgPerWorldPatternsS2C() {
        this.patterns = new NBTTagList();
    }

    public MsgPerWorldPatternsS2C(World world) {
        this.patterns = PerWorldPatternData.snapshot(world);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NBTTagCompound payload = ByteBufUtils.readTag(buf);
        patterns = payload == null
            ? new NBTTagList() : payload.getTagList("patterns", 10);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setTag("patterns", patterns == null ? new NBTTagList() : patterns);
        ByteBufUtils.writeTag(buf, payload);
    }

    @Override
    public void handleMessage(EntityPlayer player) {
        handleMessage(player, Side.CLIENT);
    }

    @Override
    public void handleMessage(EntityPlayer player, Side side) {
        if (side == Side.CLIENT) {
            PerWorldPatternData.applyClientSnapshot(patterns);
        }
    }

    public static void register() {
        PaucalAPI.registerMessage(MsgPerWorldPatternsS2C.class, Side.CLIENT);
    }
}
