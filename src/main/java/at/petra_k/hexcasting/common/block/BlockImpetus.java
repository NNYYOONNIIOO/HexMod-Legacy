package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.casting.StaffCastExecutor;
import at.petra_k.hexcasting.common.item.ItemHexStaff;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.List;

/**
 * Ordinary impetus trigger for the first 1.12.2 circle slice.
 *
 * <p>The full Hex circle graph is still being ported, but an impetus already
 * has a useful, server-authoritative trigger boundary here: right-clicking it
 * with a programmed staff starts that staff's saved VM continuation and feeds
 * its patterns in source order.</p>
 */
public class BlockImpetus extends Block {
    public BlockImpetus() {
        super(Material.IRON);
        setHardness(3.0F);
        setResistance(10.0F);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY,
                                    float hitZ) {
        if (world.isRemote) {
            return true;
        }
        ItemStack staff = player.getHeldItem(hand);
        if (!ItemHexStaff.isStaff(staff)) {
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.staff_error"));
            return true;
        }
        List<HexPattern> patterns = ItemHexStaff.getProgramPatterns(staff);
        if (patterns.isEmpty()) {
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.program_empty"));
            return true;
        }

        // An impetus starts a fresh trigger while the staff itself keeps the
        // continuation between individual patterns in this trigger.
        StaffCastExecutor.clear(staff);
        for (HexPattern pattern : patterns) {
            if (!StaffCastExecutor.execute(player, hand, staff, pattern)) {
                break;
            }
        }
        return true;
    }
}
