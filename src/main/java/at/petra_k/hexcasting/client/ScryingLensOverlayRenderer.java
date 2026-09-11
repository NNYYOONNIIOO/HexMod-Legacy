package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.item.ItemScryingLens;
import net.minecraft.client.Minecraft;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.properties.IProperty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

/**
 * A small 1.12-compatible counterpart to Hex Casting's scrying-lens HUD.
 *
 * The 1.20 implementation uses a registry of rich overlay providers. 1.12
 * has no equivalent client overlay API, so the first compatibility layer
 * exposes the same useful baseline through the vanilla text overlay: the
 * looked-at block, its redstone signal, and state properties commonly used by
 * redstone components.
 */
@Mod.EventBusSubscriber(modid = "hexcasting", value = Side.CLIENT)
public final class ScryingLensOverlayRenderer {
    private ScryingLensOverlayRenderer() {
    }

    @SubscribeEvent
    public static void onOverlayText(RenderGameOverlayEvent.Text event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null || !isHoldingLens(player)) {
            return;
        }

        RayTraceResult hit = minecraft.objectMouseOver;
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.getBlockPos() == null) {
            return;
        }

        BlockPos pos = hit.getBlockPos();
        IBlockState state = player.world.getBlockState(pos);
        event.getLeft().add("§5" + state.getBlock().getLocalizedName());
        event.getLeft().add("§7Hex: redstone " + player.world.getRedstonePower(pos, EnumFacing.UP));

        for (Map.Entry<IProperty<?>, Comparable<?>> property : state.getProperties().entrySet()) {
            String name = property.getKey().getName();
            if (isUsefulProperty(name)) {
                event.getLeft().add("§7" + name + ": " + property.getValue());
            }
        }
    }

    private static boolean isHoldingLens(EntityPlayer player) {
        ItemStack main = player.getHeldItemMainhand();
        ItemStack off = player.getHeldItemOffhand();
        return (!main.isEmpty() && main.getItem() instanceof ItemScryingLens)
            || (!off.isEmpty() && off.getItem() instanceof ItemScryingLens);
    }

    private static boolean isUsefulProperty(String name) {
        return "power".equals(name)
            || "powered".equals(name)
            || "lit".equals(name)
            || "delay".equals(name)
            || "locked".equals(name);
    }
}
