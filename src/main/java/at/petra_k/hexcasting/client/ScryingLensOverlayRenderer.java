package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.api.misc.MediaConstants;
import at.petra_k.hexcasting.common.block.BlockImpetus;
import at.petra_k.hexcasting.common.block.TileEntityAkashicBookshelf;
import at.petra_k.hexcasting.common.block.TileEntityImpetus;
import at.petra_k.hexcasting.common.item.ItemScryingLens;
import at.petra_k.hexcasting.common.lib.HexItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The 1.12.2 client bridge for Hex's scrying-lens overlay.
 *
 * <p>The modern implementation renders a list of item-and-component pairs.
 * Forge 1.12 does not expose that widget, so this renderer keeps the same
 * data model locally and paints the item icons and wrapped text after the
 * vanilla HUD. This also avoids putting diagnostic text in the debug/HUD
 * left column, where it could be hidden by another overlay.</p>
 */
@Mod.EventBusSubscriber(modid = "hexcasting", value = Side.CLIENT)
public final class ScryingLensOverlayRenderer {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFB0B0B0;
    private static final int DARK_PURPLE = 0xFFB56BDA;

    private ScryingLensOverlayRenderer() {
    }

    @SubscribeEvent
    public static void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event == null || event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null || player.world == null
            || ItemScryingLens.getScrySight(player) <= 0.0D) {
            return;
        }

        RayTraceResult hit = minecraft.objectMouseOver;
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK
            || hit.getBlockPos() == null) {
            return;
        }

        BlockPos pos = hit.getBlockPos();
        IBlockState state = player.world.getBlockState(pos);
        List<OverlayLine> lines = collectLines(player, pos, state,
            hit.sideHit);
        if (lines.isEmpty()) {
            return;
        }

        renderLines(minecraft, lines);
    }

    private static List<OverlayLine> collectLines(EntityPlayer player,
                                                   BlockPos pos,
                                                   IBlockState state,
                                                   EnumFacing hitFace) {
        List<OverlayLine> lines = new ArrayList<>();
        Block block = state.getBlock();
        Item blockItem = Item.getItemFromBlock(block);
        add(lines, blockItem == null ? ItemStack.EMPTY : new ItemStack(blockItem),
            I18n.format("hexcasting.overlay.block", block.getLocalizedName()),
            DARK_PURPLE);

        if (block.canProvidePower(state)
            && !(block instanceof BlockRedstoneComparator)) {
            int signal = getMaxRedstonePower(player, pos);
            add(lines, new ItemStack(Items.REDSTONE),
                I18n.format("hexcasting.overlay.redstone", signal),
                redstoneColor(signal, 15));
        }

        if (block == Blocks.NOTEBLOCK) {
            addNoteBlock(lines, player, pos);
        } else if (block instanceof BlockRedstoneComparator) {
            addComparator(lines, player, pos, state);
        } else if (block == Blocks.GOLDEN_RAIL) {
            int power = getPoweredRailStrength(player.world, pos, state);
            add(lines, new ItemStack(Item.getItemFromBlock(Blocks.GOLDEN_RAIL)),
                String.valueOf(power), redstoneColor(power, 9));
        } else if (block instanceof BlockRedstoneRepeater) {
            add(lines, new ItemStack(Items.CLOCK),
                String.valueOf(state.getValue(BlockRedstoneRepeater.DELAY)),
                0xFFFFFF55);
        }

        TileEntity tile = player.world.getTileEntity(pos);
        if (tile instanceof TileEntityImpetus) {
            addImpetus(lines, (TileEntityImpetus) tile);
        } else if (tile instanceof TileEntityAkashicBookshelf) {
            TileEntityAkashicBookshelf shelf = (TileEntityAkashicBookshelf) tile;
            if (shelf.hasMapping() && shelf.getIota() != null) {
                add(lines, new ItemStack(Items.BOOK), shelf.getIota().display(), WHITE);
            }
        }

        for (Map.Entry<IProperty<?>, Comparable<?>> property
            : state.getProperties().entrySet()) {
            String name = property.getKey().getName();
            if (isUsefulProperty(name, block)) {
                add(lines, ItemStack.EMPTY,
                    I18n.format("hexcasting.overlay.property",
                        localizeProperty(name), String.valueOf(property.getValue())),
                    GRAY);
            }
        }
        return lines;
    }

    private static void addNoteBlock(List<OverlayLine> lines, EntityPlayer player,
                                     BlockPos pos) {
        TileEntity tile = player.world.getTileEntity(pos);
        if (!(tile instanceof TileEntityNote)) {
            return;
        }
        TileEntityNote noteTile = (TileEntityNote) tile;
        int note = noteTile.note & 0xFF;
        int instrument = getNoteInstrument(player, pos);
        add(lines, new ItemStack(Items.RECORD_CHIRP),
            I18n.format("hexcasting.overlay.note.instrument", instrument,
                localizeInstrument(instrument)), instrumentColor(instrument));
        add(lines, new ItemStack(Item.getItemFromBlock(Blocks.NOTEBLOCK)),
            I18n.format("hexcasting.overlay.note.note", note), noteColor(note));
    }

    /** Match vanilla 1.12's note-block material/instrument selection. */
    private static int getNoteInstrument(EntityPlayer player, BlockPos pos) {
        IBlockState below = player.world.getBlockState(pos.down());
        if (below.getMaterial() == Material.ROCK) {
                return 0;
        }
        if (below.getMaterial() == Material.SAND) {
                return 1;
        }
        if (below.getMaterial() == Material.GLASS) {
                return 2;
        }
        if (below.getMaterial() == Material.WOOD) {
                return 3;
        }
        if (below.getMaterial() == Material.CLAY) {
                return 4;
        }
        Block block = below.getBlock();
        if (block == Blocks.GOLD_BLOCK) {
            return 5;
        }
        if (block == Blocks.WOOL) {
            return 6;
        }
        if (block == Blocks.PACKED_ICE) {
            return 7;
        }
        if (block == Blocks.BONE_BLOCK) {
            return 8;
        }
        if (block == Blocks.IRON_BLOCK) {
            return 9;
        }
        return 0;
    }

    private static void addComparator(List<OverlayLine> lines, EntityPlayer player,
                                      BlockPos pos, IBlockState state) {
        int signal = state.getBlock().getComparatorInputOverride(state,
            player.world, pos);
        add(lines, new ItemStack(Items.REDSTONE), String.valueOf(signal),
            redstoneColor(signal, 15));
        boolean compare = state.getValue(BlockRedstoneComparator.MODE)
            == BlockRedstoneComparator.Mode.COMPARE;
        add(lines, new ItemStack(Item.getItemFromBlock(Blocks.REDSTONE_TORCH)),
            compare ? ">=" : "-",
            redstoneColor(compare ? 0 : 15, 15));
    }

    private static void addImpetus(List<OverlayLine> lines,
                                   TileEntityImpetus impetus) {
        long media = impetus.getMedia();
        String mediaText = media < 0L
            ? I18n.format("hexcasting.overlay.media.infinite")
            : I18n.format("hexcasting.tooltip.media",
                Math.max(0L, media / MediaConstants.DUST_UNIT));
        add(lines, new ItemStack(HexItems.EXTRA_ITEMS.get("amethyst_dust")),
            mediaText, WHITE);

        if (impetus.getTriggerMode() == BlockImpetus.TriggerMode.REDSTONE) {
            String name = impetus.getStoredPlayerName();
            add(lines, name == null
                    ? new ItemStack(Item.getItemFromBlock(Blocks.BARRIER))
                    : new ItemStack(Items.SKULL, 1, 3),
                name == null
                    ? I18n.format("hexcasting.tooltip.lens.impetus.redstone.bound.none")
                    : I18n.format("hexcasting.tooltip.lens.impetus.redstone.bound", name),
                WHITE);
        }
    }

    private static int getMaxRedstonePower(EntityPlayer player, BlockPos pos) {
        int signal = 0;
        for (EnumFacing facing : EnumFacing.values()) {
            signal = Math.max(signal, player.world.getRedstonePower(pos, facing));
        }
        return signal;
    }

    private static int getPoweredRailStrength(net.minecraft.world.World world,
                                              BlockPos pos,
                                              IBlockState state) {
        if (world.isBlockPowered(pos)) {
            return 9;
        }
        int positive = findPoweredRailSignal(world, pos, state, true, 0);
        int negative = findPoweredRailSignal(world, pos, state, false, 0);
        return Math.max(positive, negative);
    }

    /** Port of BlockRailPowered's recursive signal lookup. */
    private static int findPoweredRailSignal(net.minecraft.world.World world,
                                             BlockPos pos,
                                             IBlockState state,
                                             boolean travelPositive,
                                             int depth) {
        if (depth >= 8) {
            return 0;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        boolean descending = true;
        BlockRailBase.EnumRailDirection shape = state.getValue(BlockRailPowered.SHAPE);
        switch (shape) {
            case NORTH_SOUTH:
                z += travelPositive ? 1 : -1;
                break;
            case EAST_WEST:
                x += travelPositive ? -1 : 1;
                break;
            case ASCENDING_EAST:
                if (travelPositive) {
                    x--;
                } else {
                    x++;
                    y++;
                    descending = false;
                }
                shape = BlockRailBase.EnumRailDirection.EAST_WEST;
                break;
            case ASCENDING_WEST:
                if (travelPositive) {
                    x--;
                    y++;
                    descending = false;
                } else {
                    x++;
                }
                shape = BlockRailBase.EnumRailDirection.EAST_WEST;
                break;
            case ASCENDING_NORTH:
                if (travelPositive) {
                    z++;
                } else {
                    z--;
                    y++;
                    descending = false;
                }
                shape = BlockRailBase.EnumRailDirection.NORTH_SOUTH;
                break;
            case ASCENDING_SOUTH:
                if (travelPositive) {
                    z++;
                    y++;
                    descending = false;
                } else {
                    z--;
                }
                shape = BlockRailBase.EnumRailDirection.NORTH_SOUTH;
                break;
            default:
                return 0;
        }

        BlockPos next = new BlockPos(x, y, z);
        int power = getPowerFromRail(world, next, travelPositive, depth, shape);
        if (power > 0 || !descending) {
            return power;
        }
        return getPowerFromRail(world, next.down(), travelPositive, depth, shape);
    }

    private static int getPowerFromRail(net.minecraft.world.World world,
                                        BlockPos pos,
                                        boolean travelPositive,
                                        int depth,
                                        BlockRailBase.EnumRailDirection shape) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != Blocks.GOLDEN_RAIL) {
            return 0;
        }
        BlockRailBase.EnumRailDirection otherShape = state.getValue(
            BlockRailPowered.SHAPE);
        if (shape == BlockRailBase.EnumRailDirection.EAST_WEST
            && (otherShape == BlockRailBase.EnumRailDirection.NORTH_SOUTH
                || otherShape == BlockRailBase.EnumRailDirection.ASCENDING_NORTH
                || otherShape == BlockRailBase.EnumRailDirection.ASCENDING_SOUTH)) {
            return 0;
        }
        if (shape == BlockRailBase.EnumRailDirection.NORTH_SOUTH
            && (otherShape == BlockRailBase.EnumRailDirection.EAST_WEST
                || otherShape == BlockRailBase.EnumRailDirection.ASCENDING_EAST
                || otherShape == BlockRailBase.EnumRailDirection.ASCENDING_WEST)) {
            return 0;
        }
        if (!state.getValue(BlockRailPowered.POWERED)) {
            return 0;
        }
        return world.isBlockPowered(pos)
            ? 8 - depth
            : findPoweredRailSignal(world, pos, state, travelPositive, depth + 1);
    }

    private static boolean isUsefulProperty(String name, Block block) {
        if ("power".equals(name)) {
            return !(block instanceof BlockRedstoneComparator);
        }
        if ("powered".equals(name)) {
            return !(block instanceof BlockRailPowered)
                && !(block instanceof BlockRedstoneComparator);
        }
        if ("delay".equals(name)) {
            return !(block instanceof BlockRedstoneRepeater);
        }
        return "lit".equals(name) || "locked".equals(name)
            || "mode".equals(name);
    }

    private static String localizeProperty(String name) {
        String key = "hexcasting.overlay.property." + name;
        String translated = I18n.format(key);
        return key.equals(translated) ? name : translated;
    }

    private static String localizeInstrument(int instrument) {
        String key = "hexcasting.overlay.note.instrument." + instrument;
        String translated = I18n.format(key);
        return key.equals(translated) ? String.valueOf(instrument) : translated;
    }

    private static int noteColor(int note) {
        float phase = note / 24.0F;
        float red = Math.max(0.0F,
            (float) Math.sin((phase + 0.0F) * Math.PI * 2.0D) * 0.65F + 0.35F);
        float green = Math.max(0.0F,
            (float) Math.sin((phase + 0.33333334F) * Math.PI * 2.0D)
                * 0.65F + 0.35F);
        float blue = Math.max(0.0F,
            (float) Math.sin((phase + 0.6666667F) * Math.PI * 2.0D)
                * 0.65F + 0.35F);
        return 0xFF000000
            | ((int) (red * 255.0F) << 16)
            | ((int) (green * 255.0F) << 8)
            | (int) (blue * 255.0F);
    }

    private static int instrumentColor(int instrument) {
        int[] colors = {
            0xFF7F7F7F, 0xFFE0C890, 0xFFD8D8D8, 0xFF9A6A3A,
            0xFFB0A078, 0xFFD8A824, 0xFFB59B86, 0xFFA8D8E8,
            0xFFE8B0C0, 0xFFB0B8C0
        };
        return colors[Math.max(0, Math.min(colors.length - 1, instrument))];
    }

    private static int redstoneColor(int power, int max) {
        float ratio = Math.max(0.0F, Math.min(1.0F,
            power / (float) Math.max(1, max)));
        int red = 64 + (int) (191.0F * ratio);
        int green = 32 + (int) (32.0F * (1.0F - ratio));
        return 0xFF000000 | (red << 16) | (green << 8);
    }

    private static void add(List<OverlayLine> lines, ItemStack icon,
                            String text, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        lines.add(new OverlayLine(icon == null ? ItemStack.EMPTY : icon,
            text, color));
    }

    private static void renderLines(Minecraft minecraft,
                                    List<OverlayLine> lines) {
        FontRenderer font = minecraft.fontRenderer;
        RenderItem renderItem = minecraft.getRenderItem();
        net.minecraft.client.gui.ScaledResolution resolution =
            new net.minecraft.client.gui.ScaledResolution(minecraft);
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        int maxTextWidth = Math.max(40, width / 2 - 28);
        List<RenderedLine> rendered = new ArrayList<>();
        int totalHeight = 8;
        for (OverlayLine line : lines) {
            List<String> wrapped = font.listFormattedStringToWidth(line.text,
                maxTextWidth);
            if (wrapped.isEmpty()) {
                wrapped.add(line.text);
            }
            rendered.add(new RenderedLine(line, wrapped));
            totalHeight += Math.max(16, wrapped.size() * font.FONT_HEIGHT) + 6;
        }

        int x = width / 2 + 8;
        int y = Math.max(4, height / 2 - totalHeight);
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableLighting();
        for (RenderedLine renderedLine : rendered) {
            OverlayLine line = renderedLine.line;
            if (!line.icon.isEmpty()) {
                renderItem.renderItemAndEffectIntoGUI(line.icon, x, y);
            }
            int textX = line.icon.isEmpty() ? x : x + 20;
            int textY = y + 4;
            for (String wrapped : renderedLine.wrapped) {
                font.drawStringWithShadow(wrapped, textX, textY, line.color);
                textY += font.FONT_HEIGHT;
            }
            y += Math.max(16, renderedLine.wrapped.size() * font.FONT_HEIGHT) + 6;
        }
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    private static final class OverlayLine {
        private final ItemStack icon;
        private final String text;
        private final int color;

        private OverlayLine(ItemStack icon, String text, int color) {
            this.icon = icon;
            this.text = text;
            this.color = color;
        }
    }

    private static final class RenderedLine {
        private final OverlayLine line;
        private final List<String> wrapped;

        private RenderedLine(OverlayLine line, List<String> wrapped) {
            this.line = line;
            this.wrapped = wrapped;
        }
    }
}
