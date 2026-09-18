package at.petra_k.hexcasting.common.block;

import at.petra_k.hexcasting.api.HexAPI;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Full directional amethyst pillar, matching Hex's six-facing blockstate. */
@Mod.EventBusSubscriber(modid = HexAPI.MOD_ID)
public final class BlockHexPillar extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    public BlockHexPillar() {
        super(Material.ROCK);
        setHardness(1.5F);
        setResistance(6.0F);
        setLightOpacity(0);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.getFront(meta % 6));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    /** 1.12 has no Block#onProjectileHit hook, so use Forge's impact event. */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event == null || event.getRayTraceResult() == null
            || event.getRayTraceResult().typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        RayTraceResult hit = event.getRayTraceResult();
        World world = event.getEntity().world;
        if (world == null || world.isRemote
            || !(world.getBlockState(hit.getBlockPos()).getBlock()
                instanceof BlockHexPillar)) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_HIT,
            SoundCategory.BLOCKS, 1.0F,
            0.5F + world.rand.nextFloat() * 1.2F);
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_CHIME,
            SoundCategory.BLOCKS, 1.0F,
            0.5F + world.rand.nextFloat() * 1.2F);
    }
}
