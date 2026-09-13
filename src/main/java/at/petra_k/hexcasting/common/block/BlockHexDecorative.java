package at.petra_k.hexcasting.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Shared 1.12.2 fallback for Hex decorative blocks that do not need a tile
 * entity or custom interaction.  The registry still keeps their individual
 * names and models, while their vanilla-facing material, tool and sound
 * behavior follows the block family instead of treating every block as rock.
 */
public final class BlockHexDecorative extends Block {
    private final String blockId;
    private final boolean lightSource;
    private final boolean translucent;

    public BlockHexDecorative(String id) {
        super(materialFor(id));
        this.blockId = id == null ? "" : id;
        this.lightSource = isLightSource(blockId);
        this.translucent = isTranslucent(blockId);
        setHardness(hardnessFor(id));
        setResistance(resistanceFor(id));
        setSoundType(soundFor(id));
        if (lightSource) {
            setLightLevel(1.0F);
            setLightOpacity(0);
        }
        if (translucent) {
            setLightLevel(4.0F / 15.0F);
            setLightOpacity(0);
        }
        if (isWood(id)) {
            setHarvestLevel("axe", 0);
        } else if (isStone(id) || isMetal(id)) {
            setHarvestLevel("pickaxe", 0);
        }
    }

    public boolean isLightSource() {
        return lightSource;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return !translucent && super.isOpaqueCube(state);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return !translucent && super.isFullCube(state);
    }

    @Override
    public boolean isFlammable(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return isWood(blockId) || isPaper(blockId);
    }

    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
        if (isPaper(blockId)) {
            return 100;
        }
        return isWood(blockId) ? 20 : 0;
    }

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        if (isPaper(blockId)) {
            return 60;
        }
        return isWood(blockId) ? 5 : 0;
    }

    private static Material materialFor(String id) {
        if (isWood(id)) {
            return Material.WOOD;
        }
        if (isLightSource(id) || isMetal(id)) {
            return Material.IRON;
        }
        if (id.contains("scroll") || id.contains("paper")) {
            return Material.CLOTH;
        }
        return Material.ROCK;
    }

    private static SoundType soundFor(String id) {
        if (isWood(id)) {
            return SoundType.WOOD;
        }
        if (isLightSource(id) || isMetal(id)) {
            return SoundType.METAL;
        }
        if (id.contains("scroll") || id.contains("paper")) {
            return SoundType.CLOTH;
        }
        return SoundType.STONE;
    }

    private static float hardnessFor(String id) {
        if (isLightSource(id)) {
            return 0.8F;
        }
        if (isWood(id)) {
            return 2.0F;
        }
        if (id.contains("slate") || id.contains("amethyst") || id.contains("quenched")) {
            return 3.0F;
        }
        return 1.5F;
    }

    private static float resistanceFor(String id) {
        if (id.contains("slate") || id.contains("amethyst") || id.contains("quenched")) {
            return 12.0F;
        }
        return 6.0F;
    }

    private static boolean isLightSource(String id) {
        return id.endsWith("lantern") || id.endsWith("sconce");
    }

    private static boolean isTranslucent(String id) {
        return id.startsWith("quenched_allay");
    }

    private static boolean isPaper(String id) {
        return id.contains("scroll") || id.contains("paper");
    }

    private static boolean isMetal(String id) {
        return id.contains("connector") || id.contains("lantern") || id.contains("sconce");
    }

    private static boolean isStone(String id) {
        return id.contains("slate") || id.contains("amethyst") || id.contains("quenched")
            || id.contains("bricks") || id.contains("tiles") || id.contains("pillar");
    }

    private static boolean isWood(String id) {
        return id.startsWith("edified_") || id.startsWith("stripped_edified")
            || id.contains("_log") || id.contains("_wood") || id.contains("_planks")
            || id.contains("_fence") || id.contains("_door") || id.contains("_trapdoor")
            || id.contains("_stairs") || id.contains("_slab") || id.contains("_button")
            || id.contains("_pressure_plate") || id.contains("_panel")
            || id.contains("_gate");
    }
}
