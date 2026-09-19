package at.petra_k.hexcasting.common.network;

import at.petra_k.hexcasting.common.item.ItemAbacus;
import at.petra_k.hexcasting.common.item.ItemSpellbook;
import at.petra_k.hexcasting.common.lib.HexSounds;
import at.petrak.paucal.api.PaucalAPI;
import at.petrak.paucal.api.PaucalMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;

/** Client-to-server wheel input for the spellbook and abacus. */
public final class MsgShiftScrollC2S implements PaucalMessage {
    private double mainHandDelta;
    private double offHandDelta;
    private boolean sprinting;

    public MsgShiftScrollC2S() {
        this(0.0D, 0.0D, false);
    }

    public MsgShiftScrollC2S(double mainHandDelta, double offHandDelta,
                             boolean sprinting) {
        this.mainHandDelta = mainHandDelta;
        this.offHandDelta = offHandDelta;
        this.sprinting = sprinting;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        mainHandDelta = buf.readDouble();
        offHandDelta = buf.readDouble();
        sprinting = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(mainHandDelta);
        buf.writeDouble(offHandDelta);
        buf.writeBoolean(sprinting);
    }

    @Override
    public void handleMessage(EntityPlayer player) {
        handleMessage(player, Side.SERVER);
    }

    @Override
    public void handleMessage(EntityPlayer player, Side side) {
        if (side != Side.SERVER || player == null) {
            return;
        }
        handleForHand(player, EnumHand.MAIN_HAND, mainHandDelta);
        handleForHand(player, EnumHand.OFF_HAND, offHandDelta);
    }

    private void handleForHand(EntityPlayer player, EnumHand hand, double delta) {
        if (delta == 0.0D || Double.isNaN(delta) || Double.isInfinite(delta)) {
            return;
        }
        // A wheel packet is deliberately bounded.  The client sends one
        // normalized notch, while the bound also prevents a forged packet
        // from changing an abacus by an unreasonable amount.
        double bounded = Math.max(-32.0D, Math.min(32.0D, delta));
        ItemStack stack = player.getHeldItem(hand);
        if (stack.isEmpty()) {
            return;
        }
        if (stack.getItem() instanceof ItemSpellbook) {
            spellbook(player, stack, bounded);
        } else if (stack.getItem() instanceof ItemAbacus) {
            abacus(player, hand, stack, bounded);
        }
    }

    private void spellbook(EntityPlayer player, ItemStack stack, double delta) {
        int page = ItemSpellbook.rotatePageIdx(stack, delta < 0.0D);
        int highest = ItemSpellbook.highestPage(stack);
        if (highest <= 0) {
            player.sendStatusMessage(new TextComponentTranslation(
                "hexcasting.tooltip.spellbook.empty"), true);
            return;
        }

        String key = ItemSpellbook.isSealed(stack)
            ? "hexcasting.tooltip.spellbook.page.sealed"
            : "hexcasting.tooltip.spellbook.page";
        if (ItemSpellbook.isSealed(stack)) {
            player.sendStatusMessage(new TextComponentTranslation(key, page, highest,
                new TextComponentTranslation("hexcasting.tooltip.spellbook.sealed")
                    .setStyle(new net.minecraft.util.text.Style()
                        .setColor(TextFormatting.GOLD))), true);
        } else {
            player.sendStatusMessage(new TextComponentTranslation(key, page, highest)
                .setStyle(new net.minecraft.util.text.Style()
                    .setColor(TextFormatting.GRAY)), true);
        }
    }

    private void abacus(EntityPlayer player, EnumHand hand, ItemStack stack,
                        double delta) {
        boolean increase = delta < 0.0D;
        double step;
        float pitch;
        if (hand == EnumHand.MAIN_HAND) {
            step = sprinting ? 10.0D : 1.0D;
            pitch = sprinting ? 0.7F : 0.9F;
        } else {
            step = sprinting ? 0.01D : 0.1D;
            pitch = sprinting ? 1.3F : 1.0F;
        }

        int scale = Math.max(1, (int) Math.floor(Math.abs(delta)));
        double value = ItemAbacus.getValue(stack)
            + (increase ? 1.0D : -1.0D) * scale * step;
        ItemAbacus.setValue(stack, value);

        pitch *= increase ? 1.05F : 0.95F;
        pitch += (player.world.rand.nextFloat() - 0.5F) * 0.1F;
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            HexSounds.ABACUS,
            net.minecraft.util.SoundCategory.PLAYERS, 0.5F, pitch);
        player.sendStatusMessage(new TextComponentTranslation(
            "hexcasting.tooltip.abacus", ItemAbacus.getDisplayValue(stack))
            .setStyle(new net.minecraft.util.text.Style()
                .setColor(TextFormatting.GREEN)), true);
    }

    public static void register() {
        PaucalAPI.registerMessage(MsgShiftScrollC2S.class, Side.SERVER);
    }
}
