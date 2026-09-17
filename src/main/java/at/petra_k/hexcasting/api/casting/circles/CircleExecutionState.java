package at.petra_k.hexcasting.api.casting.circles;

import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.common.block.BlockImpetus;
import at.petra_k.hexcasting.common.block.BlockCircleComponent;
import at.petra_k.hexcasting.common.block.TileEntityImpetus;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent state for one active spell circle.
 *
 * <p>The modern circle executor is a block-entity tick state.  This class is
 * the equivalent for 1.12.2: the control-flow position and the VM are saved
 * together, so a chunk unload, dimension save, or server restart cannot lose
 * the current slate or leave a half-energized ring behind.</p>
 */
public final class CircleExecutionState {
    public static final int MAX_CIRCLE_LENGTH = 1024;
    private static final String KEY_IMPETUS_POS = "impetus_pos";
    private static final String KEY_IMPETUS_DIR = "impetus_dir";
    private static final String KEY_REACHED_POSITIONS = "reached_positions";
    private static final String KEY_CURRENT_POS = "current_pos";
    private static final String KEY_ENTERED_FROM = "entered_from";
    private static final String KEY_IMAGE = "image";
    private static final String KEY_CASTER = "caster";
    private static final String KEY_REACHED_SLATE = "reached_slate";
    private static final String KEY_GREATER_CORNER = "greater_corner";
    private static final String KEY_LESSER_CORNER = "lesser_corner";

    private final BlockPos impetusPos;
    private final EnumFacing impetusDirection;
    private final Set<BlockPos> reachedPositions;
    private BlockPos currentPos;
    private EnumFacing enteredFrom;
    private CastingVM image;
    private UUID caster;
    private long reachedSlate;
    private BlockPos greaterCorner;
    private BlockPos lesserCorner;

    private CircleExecutionState(BlockPos impetusPos, EnumFacing impetusDirection,
                                 Set<BlockPos> reachedPositions,
                                 BlockPos currentPos, EnumFacing enteredFrom,
                                 CastingVM image, UUID caster, long reachedSlate,
                                 BlockPos greaterCorner, BlockPos lesserCorner) {
        this.impetusPos = impetusPos;
        this.impetusDirection = impetusDirection;
        this.reachedPositions = reachedPositions;
        this.currentPos = currentPos;
        this.enteredFrom = enteredFrom;
        this.image = image;
        this.caster = caster;
        this.reachedSlate = reachedSlate;
        this.greaterCorner = greaterCorner;
        this.lesserCorner = lesserCorner;
    }

    /** Find and validate the complete structural loop before starting it. */
    public static CreationResult createNew(TileEntityImpetus impetus,
                                            EntityPlayer caster) {
        if (impetus == null || impetus.getWorld() == null
            || impetus.getWorld().isRemote) {
            return CreationResult.failure(null);
        }
        if (impetus.getTriggerMode() == BlockImpetus.TriggerMode.EMPTY) {
            return CreationResult.failure(impetus.getPos());
        }

        World world = impetus.getWorld();
        BlockPos start = impetus.getPos();
        IBlockState impetusState = world.getBlockState(start);
        if (!(impetusState.getBlock() instanceof ICircleComponent)) {
            return CreationResult.failure(start);
        }

        EnumFacing startDirection = impetus.getStartDirection();
        Deque<ICircleComponent.Exit> todo = new ArrayDeque<>();
        todo.addLast(new ICircleComponent.Exit(
            start.offset(startDirection), startDirection));
        Set<BlockPos> seen = new LinkedHashSet<>();
        BlockPos greater = start;
        BlockPos lesser = start;
        BlockPos lastVisited = start;

        while (!todo.isEmpty()) {
            ICircleComponent.Exit next = todo.removeLast();
            BlockPos pos = next.getPosition();
            IBlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof ICircleComponent)) {
                continue;
            }
            ICircleComponent component = (ICircleComponent) state.getBlock();
            if (!component.canEnterFromDirection(next.getEnterDirection(),
                                                 pos, state, world)) {
                continue;
            }

            if (!seen.add(pos)) {
                continue;
            }
            lastVisited = pos;
            greater = max(greater, pos);
            lesser = min(lesser, pos);
            if (seen.size() >= MAX_CIRCLE_LENGTH) {
                return CreationResult.failure(null);
            }
            for (EnumFacing direction : component.possibleExitDirections(
                pos, state, world)) {
                todo.addLast(component.exitPositionFromDirection(pos, direction));
            }
        }

        if (!seen.contains(start)) {
            return CreationResult.failure(lastVisited.equals(start)
                ? null : lastVisited);
        }

        Set<BlockPos> reached = new LinkedHashSet<>();
        reached.add(start);
        CastingVM vm = new CastingVM(new CastingStack());
        if (caster != null) {
            vm.setPlayer(caster);
            vm.setCastingData(caster.getCapability(
                HexCapabilities.CASTING_DATA, null));
        }
        vm.setCircleExecutionState(null);
        List<at.petra_k.hexcasting.api.casting.math.HexPattern> bound =
            impetus.getBoundPatterns();
        if (!bound.isEmpty()) {
            vm.enqueue(bound);
        }
        return CreationResult.success(new CircleExecutionState(
            start, startDirection, reached,
            start.offset(startDirection), startDirection, vm,
            caster == null ? null : caster.getUniqueID(), 0L,
            greater, lesser));
    }

    /** Advance one component. Return false when the loop has ended. */
    public boolean tick(TileEntityImpetus impetus) {
        World world = impetus.getWorld();
        if (world == null || world.isRemote) {
            return true;
        }

        EntityPlayer player = resolveCaster(world);
        image.setPlayer(player);
        image.setCastingData(player == null ? null : player.getCapability(
            HexCapabilities.CASTING_DATA, null));
        image.setCircleExecutionState(this);

        if (currentPos.equals(impetusPos)) {
            return false;
        }

        IBlockState state = world.getBlockState(currentPos);
        if (!(state.getBlock() instanceof ICircleComponent)) {
            return false;
        }
        ICircleComponent component = (ICircleComponent) state.getBlock();
        component.startEnergized(currentPos, state, world);
        reachedPositions.add(currentPos);
        reachedSlate++;

        ICircleComponent.ControlFlow flow = component.acceptControlFlow(
            image, enteredFrom, currentPos, world.getBlockState(currentPos), world);
        if (!(flow instanceof ICircleComponent.Continue)) {
            return false;
        }
        ICircleComponent.Continue continued = (ICircleComponent.Continue) flow;

        ICircleComponent.Exit found = null;
        for (ICircleComponent.Exit exit : continued.getExits()) {
            BlockPos nextPos = exit.getPosition();
            IBlockState nextState = world.getBlockState(nextPos);
            if (!(nextState.getBlock() instanceof ICircleComponent)) {
                continue;
            }
            ICircleComponent nextComponent = (ICircleComponent) nextState.getBlock();
            if (!nextComponent.canEnterFromDirection(exit.getEnterDirection(),
                                                     nextPos, nextState, world)) {
                continue;
            }
            if (found != null) {
                return false;
            }
            found = exit;
        }
        if (found == null) {
            return false;
        }

        currentPos = found.getPosition();
        enteredFrom = found.getEnterDirection();
        image = continued.getImage();
        image.resetOperationCounter();
        return true;
    }

    /** End the visual state on every component touched by this execution. */
    public void endExecution(TileEntityImpetus impetus) {
        World world = impetus.getWorld();
        if (world == null) {
            return;
        }
        for (BlockPos pos : reachedPositions) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof BlockCircleComponent) {
                ((BlockCircleComponent) state.getBlock()).endEnergized(
                    pos, state, world);
            }
        }
    }

    public BlockPos getImpetusPos() {
        return impetusPos;
    }

    public EnumFacing getImpetusDirection() {
        return impetusDirection;
    }

    public BlockPos getCurrentPos() {
        return currentPos;
    }

    public EnumFacing getEnteredFrom() {
        return enteredFrom;
    }

    public Set<BlockPos> getReachedPositions() {
        return Collections.unmodifiableSet(reachedPositions);
    }

    public BlockPos getGreaterCorner() {
        return greaterCorner;
    }

    public BlockPos getLesserCorner() {
        return lesserCorner;
    }

    public long getReachedSlate() {
        return reachedSlate;
    }

    public UUID getCaster() {
        return caster;
    }

    public NBTTagCompound serialize() {
        NBTTagCompound out = new NBTTagCompound();
        out.setTag(KEY_IMPETUS_POS, writePos(impetusPos));
        out.setByte(KEY_IMPETUS_DIR, (byte) impetusDirection.getIndex());
        NBTTagList reached = new NBTTagList();
        for (BlockPos pos : reachedPositions) {
            reached.appendTag(writePos(pos));
        }
        out.setTag(KEY_REACHED_POSITIONS, reached);
        out.setTag(KEY_CURRENT_POS, writePos(currentPos));
        out.setByte(KEY_ENTERED_FROM, (byte) enteredFrom.getIndex());
        out.setTag(KEY_IMAGE, image.serializeState());
        if (caster != null) {
            out.setString(KEY_CASTER, caster.toString());
        }
        out.setLong(KEY_REACHED_SLATE, reachedSlate);
        out.setTag(KEY_GREATER_CORNER, writePos(greaterCorner));
        out.setTag(KEY_LESSER_CORNER, writePos(lesserCorner));
        return out;
    }

    public static CircleExecutionState deserialize(NBTTagCompound nbt)
        throws CastingException {
        BlockPos impetusPos = readPos(nbt.getCompoundTag(KEY_IMPETUS_POS));
        EnumFacing impetusDirection = readFacing(nbt.getByte(KEY_IMPETUS_DIR));
        Set<BlockPos> reached = new LinkedHashSet<>();
        NBTTagList reachedTags = nbt.getTagList(KEY_REACHED_POSITIONS, 10);
        for (int i = 0; i < reachedTags.tagCount(); i++) {
            reached.add(readPos(reachedTags.getCompoundTagAt(i)));
        }
        BlockPos currentPos = readPos(nbt.getCompoundTag(KEY_CURRENT_POS));
        EnumFacing enteredFrom = readFacing(nbt.getByte(KEY_ENTERED_FROM));
        CastingVM image = CastingVM.deserializeState(nbt.getCompoundTag(KEY_IMAGE));
        UUID caster = null;
        if (nbt.hasKey(KEY_CASTER, 8)) {
            try {
                caster = UUID.fromString(nbt.getString(KEY_CASTER));
            } catch (IllegalArgumentException ignored) {
                caster = null;
            }
        }
        BlockPos greater = readPos(nbt.getCompoundTag(KEY_GREATER_CORNER));
        BlockPos lesser = readPos(nbt.getCompoundTag(KEY_LESSER_CORNER));
        return new CircleExecutionState(impetusPos, impetusDirection, reached,
            currentPos, enteredFrom, image, caster,
            Math.max(0L, nbt.getLong(KEY_REACHED_SLATE)), greater, lesser);
    }

    private EntityPlayer resolveCaster(World world) {
        if (caster == null) {
            return null;
        }
        return world.getPlayerEntityByUUID(caster);
    }

    private static BlockPos max(BlockPos a, BlockPos b) {
        return new BlockPos(Math.max(a.getX(), b.getX()),
            Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    private static BlockPos min(BlockPos a, BlockPos b) {
        return new BlockPos(Math.min(a.getX(), b.getX()),
            Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    private static NBTTagCompound writePos(BlockPos pos) {
        NBTTagCompound out = new NBTTagCompound();
        out.setInteger("x", pos.getX());
        out.setInteger("y", pos.getY());
        out.setInteger("z", pos.getZ());
        return out;
    }

    private static BlockPos readPos(NBTTagCompound tag) {
        return new BlockPos(tag.getInteger("x"), tag.getInteger("y"),
            tag.getInteger("z"));
    }

    private static EnumFacing readFacing(byte serialized) {
        int index = serialized & 7;
        return index < 6 ? EnumFacing.getFront(index) : EnumFacing.UP;
    }

    /** Result of the structural scan, with the last failed position if any. */
    public static final class CreationResult {
        private final CircleExecutionState state;
        private final BlockPos errorPosition;

        private CreationResult(CircleExecutionState state, BlockPos errorPosition) {
            this.state = state;
            this.errorPosition = errorPosition;
        }

        private static CreationResult success(CircleExecutionState state) {
            return new CreationResult(state, null);
        }

        private static CreationResult failure(BlockPos errorPosition) {
            return new CreationResult(null, errorPosition);
        }

        public boolean isSuccess() {
            return state != null;
        }

        public CircleExecutionState getState() {
            return state;
        }

        public BlockPos getErrorPosition() {
            return errorPosition;
        }
    }
}
