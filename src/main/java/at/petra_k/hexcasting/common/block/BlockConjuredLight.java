package at.petra_k.hexcasting.common.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.MaterialTransparent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** An invisible light source created by Hex Casting. */
public final class BlockConjuredLight extends BlockConjured {
    /**
     * A distinct transparent material is required here.  1.12.2's client
     * block-breaking controller treats the exact Material.AIR singleton as
     * "nothing" and refuses to start/finish a left-click break.  This keeps
     * all air-like rendering and movement properties while making the light
     * a real targetable block.
     */
    private static final Material LIGHT_MATERIAL =
        new MaterialTransparent(MapColor.AIR);
    private static final AxisAlignedBB LIGHT_AABB = new AxisAlignedBB(
        5.0D / 16.0D, 5.0D / 16.0D, 5.0D / 16.0D,
        11.0D / 16.0D, 11.0D / 16.0D, 11.0D / 16.0D);

    public BlockConjuredLight() {
        super(LIGHT_MATERIAL);
        setLightLevel(1.0F);
        setLightOpacity(0);
        // Hex's light uses an instabreak block property. It is still a real
        // block, so the normal left-click block-breaking path must be able
        // to remove it; non-collision is handled independently below.
        setHardness(0.0F);
        setResistance(0.0F);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /** Keep the small selection shape used by Hex while remaining intangible. */
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return LIGHT_AABB;
    }

    /** A normal block may replace the temporary light at the same position. */
    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isCollidable() {
        // Keep ray tracing enabled so the 5/16..11/16 selection box can be
        // targeted.  Physical collision remains disabled below by returning
        // null from getCollisionBoundingBox.
        return true;
    }

    @Override
    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
        // Material.AIR normally makes the vanilla ray-trace gate reject this
        // block. Selection and physical collision are separate in 1.12.2:
        // allow the ray trace here while getCollisionBoundingBox remains
        // null so entities can pass through the light.
        return true;
    }

    @Override
    public RayTraceResult collisionRayTrace(IBlockState state, World world,
                                            BlockPos pos, Vec3d start,
                                            Vec3d end) {
        // Keep the selection box targetable even when another 1.12.2
        // material/coremod path treats AIR as non-raytraceable.
        return rayTrace(pos, start, end, LIGHT_AABB);
    }

    /** Light blocks do not produce the solid conjured-block footstep effect. */
    @Override
    public void onEntityWalk(World world, BlockPos pos, Entity entity) {
        // Intentionally empty; matches Hex's BlockConjuredLight override.
    }

    /** Light has no solid landing effect, matching Hex's light block. */
    @Override
    public boolean addLandingEffects(IBlockState state, WorldServer world,
                                     BlockPos pos, IBlockState landedState,
                                     EntityLivingBase entity,
                                     int numberOfParticles) {
        return true;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return null;
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }

    /** Conjured light follows the same temporary lifetime as conjured blocks. */
    @Override
    public boolean hasTileEntity(net.minecraft.block.state.IBlockState state) {
        return true;
    }

    @Override
    public net.minecraft.tileentity.TileEntity createTileEntity(
            net.minecraft.world.World world,
            net.minecraft.block.state.IBlockState state) {
        return new TileEntityConjured();
    }
}
