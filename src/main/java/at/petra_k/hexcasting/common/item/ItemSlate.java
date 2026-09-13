package at.petra_k.hexcasting.common.item;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.block.TileEntitySlate;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.List;

/** Item form of a slate, including written-pattern block entity data. */
public final class ItemSlate extends ItemBlock {
    private static final String TAG_BLOCK_ENTITY = "BlockEntityTag";

    public ItemSlate(Block block) {
        super(block);
        setMaxStackSize(16);
        addPropertyOverride(new ResourceLocation("hexcasting", "written"),
            new IItemPropertyGetter() {
                @Override
                public float apply(ItemStack stack, World world, EntityLivingBase entity) {
                    return getPattern(stack) == null ? 0.0F : 1.0F;
                }
            });
    }

    public static HexPattern getPattern(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getTagCompound() == null) {
            return null;
        }
        NBTTagCompound blockEntityTag = stack.getTagCompound()
            .getCompoundTag(TAG_BLOCK_ENTITY);
        if (!blockEntityTag.hasKey(TileEntitySlate.TAG_PATTERN, 10)) {
            return null;
        }
        NBTTagCompound patternTag = blockEntityTag
            .getCompoundTag(TileEntitySlate.TAG_PATTERN);
        if (!patternTag.hasKey(HexPattern.TAG_START_DIR, 1)
            || !patternTag.hasKey(HexPattern.TAG_ANGLES, 7)) {
            return null;
        }
        try {
            return HexPattern.fromNBT(patternTag);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static void writePattern(ItemStack stack, HexPattern pattern) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            if (pattern == null) {
                return;
            }
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        if (pattern == null) {
            if (tag.hasKey(TAG_BLOCK_ENTITY, 10)) {
                NBTTagCompound blockEntityTag = tag.getCompoundTag(TAG_BLOCK_ENTITY);
                blockEntityTag.removeTag(TileEntitySlate.TAG_PATTERN);
                if (blockEntityTag.hasNoTags()) {
                    tag.removeTag(TAG_BLOCK_ENTITY);
                }
            }
            return;
        }
        NBTTagCompound blockEntityTag = tag.hasKey(TAG_BLOCK_ENTITY, 10)
            ? tag.getCompoundTag(TAG_BLOCK_ENTITY) : new NBTTagCompound();
        blockEntityTag.setTag(TileEntitySlate.TAG_PATTERN, pattern.serializeToNBT());
        tag.setTag(TAG_BLOCK_ENTITY, blockEntityTag);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
                                      EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        // ItemBlock may consume or replace the held stack during placement;
        // capture the written block-entity data before delegating to it.
        HexPattern writtenPattern = getPattern(player.getHeldItem(hand).copy());
        IBlockState clickedState = world.getBlockState(pos);
        BlockPos placementPos = clickedState.getBlock().isReplaceable(world, pos)
            ? pos : pos.offset(facing);
        EnumActionResult result = super.onItemUse(
            player, world, pos, hand, facing, hitX, hitY, hitZ);
        if (result == EnumActionResult.SUCCESS && !world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(placementPos);
            if (tileEntity instanceof TileEntitySlate) {
                if (writtenPattern != null) {
                    ((TileEntitySlate) tileEntity).setPattern(writtenPattern);
                }
            }
        }
        return result;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        HexPattern pattern = getPattern(stack);
        if (pattern == null) {
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.slate_blank"));
        } else {
            tooltip.add(I18n.translateToLocal("hexcasting.tooltip.slate_written"));
            tooltip.add(I18n.translateToLocalFormatted(
                "hexcasting.tooltip.slate_pattern", pattern.signature()));
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String name = super.getItemStackDisplayName(stack);
        HexPattern pattern = getPattern(stack);
        return pattern == null ? name : I18n.translateToLocalFormatted(
            "hexcasting.item.slate.written", name, pattern.signature());
    }

    /** Keep blank slates free of stale BlockEntityTag data after clearing. */
    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity,
                         int itemSlot, boolean isSelected) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(TAG_BLOCK_ENTITY, 10)
            && getPattern(stack) == null) {
            tag.removeTag(TAG_BLOCK_ENTITY);
        }
    }
}
