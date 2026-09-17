package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** The translucent temporary block created by Hex Casting. */
public class BlockConjured extends Block {
    public BlockConjured() {
        this(Material.GLASS);
    }

    protected BlockConjured(Material material) {
        super(material);
        setHardness(0.3F);
        setResistance(0.3F);
        setLightOpacity(0);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return null;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        world.notifyBlockUpdate(pos, state, state, 3);
        super.onBlockAdded(world, pos, state);
    }

    @Override
    public void onEntityWalk(World world, BlockPos pos, Entity entity) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityConjured) {
            ((TileEntityConjured) tile).walkParticle(entity);
        }
        super.onEntityWalk(world, pos, entity);
    }

    /** Use Hex's colored landing burst instead of vanilla block fragments. */
    @Override
    public boolean addLandingEffects(IBlockState state, WorldServer world,
                                     BlockPos pos, IBlockState landedState,
                                     EntityLivingBase entity,
                                     int numberOfParticles) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityConjured) {
            ((TileEntityConjured) tile).landParticle(entity, numberOfParticles);
        }
        return true;
    }

    /** The visual effect is supplied by the block entity, as in Hex's source. */
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    /**
     * Use Hex's colored conjure cloud for destruction instead of vanilla's
     * 4x4x4 block fragments.  The 1.20.1 source deliberately suppresses the
     * ordinary block-destroy effect for this invisible block; the 1.12.2
     * port keeps that behavior while supplying the equivalent Hex particle.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public boolean addDestroyEffects(World world, BlockPos pos,
                                     ParticleManager manager) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityConjured) {
            ((TileEntityConjured) tile).destroyParticle();
            return true;
        }
        return false;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityConjured();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile != null) {
            tile.invalidate();
        }
        super.breakBlock(world, pos, state);
    }
}
