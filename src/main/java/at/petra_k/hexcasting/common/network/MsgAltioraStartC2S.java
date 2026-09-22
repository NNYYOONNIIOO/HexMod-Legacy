package at.petra_k.hexcasting.common.network;

import at.petra_k.hexcasting.common.lib.hex.HexActions;
import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;

/** Client request to deploy the virtual Altiora elytra. */
public final class MsgAltioraStartC2S implements PaucalMessage {
    public MsgAltioraStartC2S() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // This message intentionally has no client-controlled payload.
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // This message intentionally has no client-controlled payload.
    }

    @Override
    public void handleMessage(EntityPlayer player) {
        handleMessage(player, Side.SERVER);
    }

    @Override
    public void handleMessage(EntityPlayer player, Side side) {
        if (side == Side.SERVER) {
            HexActions.tryStartAltiora(player);
        }
    }

    public static void register() {
        PaucalAPI.registerMessage(MsgAltioraStartC2S.class, Side.SERVER);
    }
}
