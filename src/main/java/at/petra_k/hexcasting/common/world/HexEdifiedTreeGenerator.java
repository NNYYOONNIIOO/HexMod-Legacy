package at.petra_k.hexcasting.common.world;

import at.petra_k.hexcasting.common.lib.HexBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The 1.12.2 implementation of Hex's Akashic tree feature.
 *
 * <p>The modern implementation uses a configured FancyTrunkPlacer and a
 * FancyFoliagePlacer.  Those feature classes do not exist in 1.12.2, so this
 * generator keeps the same important properties in a small native generator:
 * a tall irregular crown, three leaf variants, and a 1:8 chance for a colour
 * accent in each trunk segment.</p>
 */
public final class HexEdifiedTreeGenerator extends WorldGenAbstractTree {
    private static final String[] LEAF_VARIANTS = new String[] {
        "amethyst_edified_leaves",
        "aventurine_edified_leaves",
        "citrine_edified_leaves"
    };

    private final IBlockState trunk;
    private final IBlockState accentTrunk;
    private final IBlockState leaves;
    private final int height;
    private final Random random;

    private HexEdifiedTreeGenerator(IBlockState trunk, IBlockState accentTrunk,
                                    IBlockState leaves, int height, Random random) {
        super(true);
        this.trunk = trunk;
        this.accentTrunk = accentTrunk;
        this.leaves = leaves;
        this.height = height;
        this.random = random;
    }

    /** Try one complete tree placement.  No world blocks are changed on a failed pre-check. */
    public static boolean growTree(World world, Random random, BlockPos position) {
        if (world == null || random == null || position == null
            || position.getY() < 1 || !world.isAreaLoaded(position, 4)) {
            return false;
        }

        Block soil = world.getBlockState(position.down()).getBlock();
        IBlockState soilState = world.getBlockState(position.down());
        if (!soil.canSustainPlant(soilState, world, position.down(), EnumFacing.UP,
            (BlockSapling) Blocks.SAPLING)) {
            return false;
        }

        Block trunkBlock = HexBlocks.BLOCKS.get("edified_log");
        if (trunkBlock == null) {
            return false;
        }

        String leafId = LEAF_VARIANTS[random.nextInt(LEAF_VARIANTS.length)];
        Block leavesBlock = HexBlocks.BLOCKS.get(leafId);
        Block accentBlock = HexBlocks.BLOCKS.get(
            "edified_log_" + leafId.substring(0, leafId.indexOf('_')));
        if (leavesBlock == null || accentBlock == null) {
            return false;
        }

        int height = 5 + random.nextInt(6) + random.nextInt(4);
        IBlockState trunkState = trunkBlock.getDefaultState()
            .withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.Y);
        IBlockState accentState = accentBlock.getDefaultState()
            .withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.Y);
        IBlockState leavesState = leavesBlock.getDefaultState();

        return new HexEdifiedTreeGenerator(
            trunkState, accentState, leavesState, height, random)
            .generate(world, random, position);
    }

    @Override
    public boolean generate(World world, Random rand, BlockPos position) {
        if (position.getY() + height + 2 >= world.getHeight()) {
            return false;
        }

        IBlockState soil = world.getBlockState(position.down());
        if (!soil.getBlock().canSustainPlant(soil, world, position.down(), EnumFacing.UP,
            (BlockSapling) Blocks.SAPLING)) {
            return false;
        }

        /*
         * FancyTrunkPlacer in modern Hex does not use one fixed vertical
         * column.  Build the complete plan first so that every random tree
         * variant can be checked before it changes the world.
         */
        int variant = rand.nextInt(4);
        int[] pathX = new int[height + 1];
        int[] pathZ = new int[height + 1];
        buildTrunkPath(pathX, pathZ, variant, rand);

        Map<BlockPos, EnumFacing.Axis> logs = new HashMap<>();
        Set<BlockPos> foliage = new HashSet<>();
        for (int y = 0; y <= height; y++) {
            BlockPos logPos = position.add(pathX[y], y, pathZ[y]);
            logs.put(logPos, logAxis(pathX, pathZ, y));
        }

        // Each shape gets a different number and distribution of horizontal
        // branches.  The 1.20.1 feature's random branch angles are expressed
        // here as the four cardinal axes available to 1.12.2 log states.
        int branchCount = 3 + rand.nextInt(3);
        int branchFloor = Math.max(2, height / 3);
        int branchRange = Math.max(1, height - branchFloor - 1);
        EnumFacing[] directions = new EnumFacing[] {
            EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST
        };
        for (int branch = 0; branch < branchCount; branch++) {
            int branchY = branchFloor + rand.nextInt(branchRange);
            EnumFacing direction = directions[rand.nextInt(directions.length)];
            int length = 1 + rand.nextInt(variant == 2 ? 3 : 2);
            for (int step = 1; step <= length; step++) {
                BlockPos branchPos = position.add(
                    pathX[branchY] + direction.getFrontOffsetX() * step,
                    branchY + (step == length && variant == 3 ? 1 : 0),
                    pathZ[branchY] + direction.getFrontOffsetZ() * step);
                logs.put(branchPos, direction.getAxis());
            }
        }

        int top = height;
        int crownOffsetX = variant == 1 ? 1 : variant == 2 ? -1 : 0;
        int crownOffsetZ = variant == 3 ? 1 : 0;
        for (int y = top - 4; y <= top + 1; y++) {
            int layer = y - (top - 4);
            int radius = foliageRadius(variant, layer);
            int pathIndex = Math.max(0, Math.min(height, y));
            int centerX = pathX[pathIndex] + crownOffsetX;
            int centerZ = pathZ[pathIndex] + crownOffsetZ;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int distance = Math.abs(x) + Math.abs(z);
                    if (distance > radius + 1
                        || (Math.abs(x) == radius && Math.abs(z) == radius)
                        || (variant == 3 && layer > 0 && rand.nextInt(7) == 0)) {
                        continue;
                    }
                    foliage.add(position.add(centerX + x, y, centerZ + z));
                }
            }
        }
        foliage.removeAll(logs.keySet());

        // Reserve only the planned positions rather than a large cube. This
        // keeps twisted trees usable in forests and still makes failure atomic.
        for (BlockPos logPos : logs.keySet()) {
            if (!isReplaceable(world, logPos)) {
                return false;
            }
        }
        for (BlockPos leafPos : foliage) {
            if (!isReplaceable(world, leafPos)) {
                return false;
            }
        }

        soil.getBlock().onPlantGrow(soil, world, position.down(), position);
        for (Map.Entry<BlockPos, EnumFacing.Axis> entry : logs.entrySet()) {
            putLog(world, entry.getKey(), entry.getValue());
        }
        for (BlockPos leafPos : foliage) {
            IBlockState state = world.getBlockState(leafPos);
            if (state.getBlock().isAir(state, world, leafPos)
                || state.getBlock().isLeaves(state, world, leafPos)) {
                setBlockAndNotifyAdequately(world, leafPos, leaves);
            }
        }
        return true;
    }

    /** Random-walk the trunk, with four profiles corresponding to visible variants. */
    private static void buildTrunkPath(int[] pathX, int[] pathZ,
                                       int variant, Random random) {
        int targetX = random.nextInt(3) - 1;
        int targetZ = random.nextInt(3) - 1;
        for (int y = 1; y < pathX.length; y++) {
            int x = pathX[y - 1];
            int z = pathZ[y - 1];
            if (variant == 0) {
                // Mostly straight, with an occasional one-block kink.
                if (random.nextInt(6) == 0) {
                    x += random.nextInt(3) - 1;
                    z += random.nextInt(3) - 1;
                }
            } else if (variant == 1) {
                // A gradual lean toward one side of the crown.
                x += Integer.compare(targetX, x) * (random.nextBoolean() ? 1 : 0);
                z += Integer.compare(targetZ, z) * (random.nextBoolean() ? 1 : 0);
            } else if (variant == 2) {
                // An S bend: reverse the target after the midpoint.
                int sign = y < pathX.length / 2 ? 1 : -1;
                x += Integer.compare(targetX * sign, x) * (random.nextBoolean() ? 1 : 0);
                z += Integer.compare(targetZ * sign, z) * (random.nextBoolean() ? 1 : 0);
            } else {
                // A visibly irregular random walk, bounded so the crown stays connected.
                if (random.nextBoolean()) {
                    x += random.nextInt(3) - 1;
                }
                if (random.nextBoolean()) {
                    z += random.nextInt(3) - 1;
                }
            }
            pathX[y] = Math.max(-1, Math.min(1, x));
            pathZ[y] = Math.max(-1, Math.min(1, z));
        }
    }

    private static EnumFacing.Axis logAxis(int[] pathX, int[] pathZ, int y) {
        if (y > 0 && pathX[y] != pathX[y - 1]) {
            return EnumFacing.Axis.X;
        }
        if (y > 0 && pathZ[y] != pathZ[y - 1]) {
            return EnumFacing.Axis.Z;
        }
        return EnumFacing.Axis.Y;
    }

    private static int foliageRadius(int variant, int layer) {
        if (variant == 1 && (layer == 1 || layer == 2)) {
            return 3;
        }
        if (variant == 2 && layer == 0) {
            return 1;
        }
        if (variant == 3 && layer == 4) {
            return 3;
        }
        return layer <= 2 ? 2 : 1;
    }

    private void putLog(World world, BlockPos position, EnumFacing.Axis axis) {
        IBlockState state = random.nextInt(9) == 0 ? accentTrunk : trunk;
        setBlockAndNotifyAdequately(world, position, state
            .withProperty(BlockRotatedPillar.AXIS, axis));
    }
}
