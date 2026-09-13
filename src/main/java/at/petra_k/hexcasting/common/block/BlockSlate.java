package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.casting.math.HexPattern;
import at.petra_k.hexcasting.common.item.ItemPatternScroll;
import at.petra_k.hexcasting.common.item.ItemSlate;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Carved slate used by Hex Casting circle structures. */
public final class BlockSlate extends Block {
    public BlockSlate() {
        super(Material.ROCK);
        setHardness(1.5F);
        setResistance(6.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntitySlate();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY,
                                    float hitZ) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntitySlate)) {
            return false;
        }
        TileEntitySlate slate = (TileEntitySlate) tileEntity;
        if (world.isRemote) {
            return true;
        }

        if (player.isSneaking()) {
            if (slate.getPattern() != null) {
                slate.setPattern(null);
                player.sendMessage(new TextComponentTranslation(
                    "hexcasting.message.slate_cleared"));
            } else {
                player.sendMessage(new TextComponentTranslation(
                    "hexcasting.message.slate_empty"));
            }
            return true;
        }

        EnumHand scrollHand = null;
        ItemStack held = player.getHeldItem(hand);
        if (isPatternScroll(held)) {
            scrollHand = hand;
        } else if (isPatternScroll(player.getHeldItemOffhand())) {
            scrollHand = EnumHand.OFF_HAND;
        }
        if (scrollHand != null) {
            ItemStack scroll = player.getHeldItem(scrollHand);
            HexPattern pattern = ItemPatternScroll.getPattern(scroll);
            if (pattern != null) {
                slate.setPattern(pattern);
                ItemPatternScroll.consumeForWrite(scroll, player);
                player.sendMessage(new TextComponentTranslation(
                    "hexcasting.message.slate_written", pattern.signature()));
            } else {
                player.sendMessage(new TextComponentTranslation(
                    "hexcasting.message.slate_invalid_scroll"));
            }
            return true;
        }

        HexPattern pattern = slate.getPattern();
        if (pattern == null) {
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.slate_empty"));
        } else {
            player.sendMessage(new TextComponentTranslation(
                "hexcasting.message.slate_pattern", pattern.signature()));
        }
        return true;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return createItemStack(world, pos);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world,
                         BlockPos pos, IBlockState state, int fortune) {
        drops.add(createItemStack(world, pos));
    }

    private ItemStack createItemStack(IBlockAccess world, BlockPos pos) {
        Item item = Item.getItemFromBlock(this);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntitySlate) {
            ItemSlate.writePattern(stack, ((TileEntitySlate) tileEntity).getPattern());
        }
        return stack;
    }

    private static boolean isPatternScroll(ItemStack stack) {
        return stack != null && !stack.isEmpty()
            && stack.getItem() instanceof ItemPatternScroll;
    }
}
