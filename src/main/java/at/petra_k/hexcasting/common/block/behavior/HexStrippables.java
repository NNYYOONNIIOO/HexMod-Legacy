package at.petra_k.hexcasting.common.block.behavior;

import at.petra_k.hexcasting.HexCasting;
import at.petra_k.hexcasting.common.lib.HexBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/** Forge 1.12.2 implementation of the modern axe-strip block map. */
@Mod.EventBusSubscriber(modid = HexCasting.MOD_ID)
public final class HexStrippables {
    private static final Map<Block, Block> STRIPPABLES = createMap();

    private HexStrippables() {
    }

    private static Map<Block, Block> createMap() {
        Map<Block, Block> result = new IdentityHashMap<>();
        add(result, "edified_log", "stripped_edified_log");
        add(result, "edified_log_amethyst", "stripped_edified_log");
        add(result, "edified_log_aventurine", "stripped_edified_log");
        add(result, "edified_log_citrine", "stripped_edified_log");
        add(result, "edified_log_purple", "stripped_edified_log");
        add(result, "edified_wood", "stripped_edified_wood");
        return Collections.unmodifiableMap(result);
    }

    private static void add(Map<Block, Block> result, String source, String output) {
        Block sourceBlock = HexBlocks.BLOCKS.get(source);
        Block outputBlock = HexBlocks.BLOCKS.get(output);
        if (sourceBlock != null && outputBlock != null) {
            result.put(sourceBlock, outputBlock);
        }
    }

    /**
     * 1.12.2 predates Forge's BlockToolModificationEvent.  Intercept the
     * same right-click interaction used by vanilla-era axe integrations and
     * only consume the axe after the server has changed the block.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event == null || event.getWorld() == null || event.getWorld().isRemote
            || event.getHand() == null) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = player == null ? ItemStack.EMPTY : player.getHeldItem(event.getHand());
        if (held.isEmpty() || !(held.getItem() instanceof ItemAxe)) {
            return;
        }

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        Block stripped = STRIPPABLES.get(state.getBlock());
        if (stripped == null) {
            return;
        }

        IBlockState strippedState = stripped.getDefaultState();
        if (state.getPropertyKeys().contains(BlockRotatedPillar.AXIS)
            && strippedState.getPropertyKeys().contains(BlockRotatedPillar.AXIS)) {
            strippedState = strippedState.withProperty(BlockRotatedPillar.AXIS,
                state.getValue(BlockRotatedPillar.AXIS));
        }
        if (!world.setBlockState(pos, strippedState, 11)) {
            return;
        }
        if (!player.capabilities.isCreativeMode) {
            held.damageItem(1, player);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_WOOD_PLACE,
            SoundCategory.BLOCKS, 1.0F, 1.0F);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
    }
}
