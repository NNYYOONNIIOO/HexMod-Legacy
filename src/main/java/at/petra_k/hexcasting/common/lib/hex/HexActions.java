package at.petra_k.hexcasting.common.lib.hex;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.eval.CastingException;
import at.petra_k.hexcasting.api.casting.eval.CastingStack;
import at.petra_k.hexcasting.api.casting.iota.BooleanIota;
import at.petra_k.hexcasting.api.casting.iota.DoubleIota;
import at.petra_k.hexcasting.api.casting.iota.Iota;
import at.petra_k.hexcasting.api.casting.iota.NullIota;
import at.petra_k.hexcasting.api.casting.iota.ListIota;
import at.petra_k.hexcasting.api.casting.iota.Vec3Iota;
import at.petra_k.hexcasting.api.casting.math.HexAngle;
import at.petra_k.hexcasting.api.casting.math.HexDir;
import at.petra_k.hexcasting.api.casting.math.HexPattern;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.Collections;

/**
 * First portable action slice of Hex Casting.
 *
 * <p>The action ids and registry shape are stable; more 1.20.1 actions can be
 * migrated into this table without changing the evaluator or stack API.</p>
 */
public final class HexActions {
    public static final ResourceLocation PUSH_ZERO_ID = new ResourceLocation(HexAPI.MOD_ID, "push_zero");
    public static final HexPattern PUSH_ZERO_PATTERN = pattern(HexDir.EAST);
    public static final HexAction PUSH_ZERO = register(PUSH_ZERO_ID, PUSH_ZERO_PATTERN, stack ->
        stack.push(new DoubleIota(0.0D)));

    public static final ResourceLocation PUSH_ONE_ID = new ResourceLocation(HexAPI.MOD_ID, "push_one");
    public static final HexPattern PUSH_ONE_PATTERN = pattern(HexDir.EAST, HexAngle.FORWARD);
    public static final HexAction PUSH_ONE = register(PUSH_ONE_ID, PUSH_ONE_PATTERN, stack ->
        stack.push(new DoubleIota(1.0D)));

    public static final ResourceLocation DUPLICATE_ID = new ResourceLocation(HexAPI.MOD_ID, "duplicate");
    public static final HexPattern DUPLICATE_PATTERN = pattern(HexDir.EAST, HexAngle.RIGHT);
    public static final HexAction DUPLICATE = register(DUPLICATE_ID, DUPLICATE_PATTERN, stack ->
        stack.push(stack.peek()));

    public static final ResourceLocation SWAP_ID = new ResourceLocation(HexAPI.MOD_ID, "swap");
    public static final HexPattern SWAP_PATTERN = pattern(HexDir.EAST, HexAngle.RIGHT_BACK);
    public static final HexAction SWAP = register(SWAP_ID, SWAP_PATTERN, stack -> {
        at.petra_k.hexcasting.api.casting.iota.Iota top = stack.pop();
        at.petra_k.hexcasting.api.casting.iota.Iota below = stack.pop();
        stack.push(top);
        stack.push(below);
    });

    public static final ResourceLocation ADD_ID = new ResourceLocation(HexAPI.MOD_ID, "add");
    public static final HexPattern ADD_PATTERN = pattern(HexDir.NORTH_EAST, "waaw");
    public static final HexAction ADD = register(ADD_ID, ADD_PATTERN, stack -> {
        double right = stack.pop(DoubleIota.class).getValue();
        double left = stack.pop(DoubleIota.class).getValue();
        stack.push(new DoubleIota(left + right));
    });

    public static final ResourceLocation NOT_ID = new ResourceLocation(HexAPI.MOD_ID, "not");
    public static final HexPattern NOT_PATTERN = pattern(HexDir.EAST, HexAngle.LEFT_BACK);
    public static final HexAction NOT = register(NOT_ID, NOT_PATTERN, stack ->
        stack.push(new BooleanIota(!stack.pop(BooleanIota.class).getValue())));

    /** Construct an empty list on the casting stack. */
    public static final ResourceLocation EMPTY_LIST_ID =
        new ResourceLocation(HexAPI.MOD_ID, "empty_list");
    public static final HexPattern EMPTY_LIST_PATTERN =
        pattern(HexDir.NORTH_EAST, "qqaeaae");
    public static final HexAction EMPTY_LIST = register(EMPTY_LIST_ID, EMPTY_LIST_PATTERN, stack ->
        stack.push(new ListIota(Collections.<Iota>emptyList())));

    /** Wrap the top stack value in a one-element list. */
    public static final ResourceLocation SINGLETON_ID =
        new ResourceLocation(HexAPI.MOD_ID, "singleton");
    public static final HexPattern SINGLETON_PATTERN =
        pattern(HexDir.EAST, "adeeed");
    public static final HexAction SINGLETON = register(SINGLETON_ID, SINGLETON_PATTERN, stack -> {
        Iota value = stack.pop();
        stack.push(new ListIota(Collections.singletonList(value)));
    });

    /** Expand the top list back onto the casting stack in list order. */
    public static final ResourceLocation SPLAT_ID =
        new ResourceLocation(HexAPI.MOD_ID, "splat");
    public static final HexPattern SPLAT_PATTERN =
        pattern(HexDir.NORTH_WEST, "qwaeawq");
    public static final HexAction SPLAT = register(SPLAT_ID, SPLAT_PATTERN, stack -> {
        for (Iota value : stack.pop(ListIota.class).getItems()) {
            stack.push(value);
        }
    });

    /** Compare two stack values using their Iota value semantics. */
    public static final ResourceLocation EQUALITY_ID = new ResourceLocation(HexAPI.MOD_ID, "equals");
    public static final HexPattern EQUALITY_PATTERN = pattern(HexDir.EAST, "ad");
    public static final HexAction EQUALITY = register(EQUALITY_ID, EQUALITY_PATTERN, stack -> {
        Iota right = stack.pop();
        Iota left = stack.pop();
        stack.push(new BooleanIota(left.equals(right)));
    });

    /** Compare only the kinds of two stack values, ignoring their payloads. */
    public static final ResourceLocation TYPE_EQUALITY_ID = new ResourceLocation(HexAPI.MOD_ID, "type_equals");
    public static final HexPattern TYPE_EQUALITY_PATTERN = pattern(HexDir.EAST, "wawdw");
    public static final HexAction TYPE_EQUALITY = register(TYPE_EQUALITY_ID, TYPE_EQUALITY_PATTERN, stack -> {
        Iota right = stack.pop();
        Iota left = stack.pop();
        stack.push(new BooleanIota(left.getType() == right.getType()));
    });

    /** Convert any Iota's truthiness to an explicit Boolean Iota. */
    public static final ResourceLocation COERCE_TO_BOOL_ID = new ResourceLocation(HexAPI.MOD_ID, "bool_coerce");
    public static final HexPattern COERCE_TO_BOOL_PATTERN = pattern(HexDir.NORTH_EAST, "aw");
    public static final HexAction COERCE_TO_BOOL = register(COERCE_TO_BOOL_ID, COERCE_TO_BOOL_PATTERN, stack ->
        stack.push(new BooleanIota(stack.pop().isTruthy())));

    /** Select the true or false branch; stack order is condition, true value, false value. */
    public static final ResourceLocation BOOL_IF_ID = new ResourceLocation(HexAPI.MOD_ID, "if");
    public static final HexPattern BOOL_IF_PATTERN = pattern(HexDir.SOUTH_EAST, "awdd");
    public static final HexAction BOOL_IF = register(BOOL_IF_ID, BOOL_IF_PATTERN, stack -> {
        Iota falseValue = stack.pop();
        Iota trueValue = stack.pop();
        boolean condition = stack.pop(BooleanIota.class).getValue();
        stack.push(condition ? trueValue : falseValue);
    });

    /** Numeric comparison actions copied from the 1.20.1 pure stack semantics. */
    public static final ResourceLocation GREATER_ID =
        new ResourceLocation(HexAPI.MOD_ID, "greater");
    public static final HexPattern GREATER_PATTERN =
        pattern(HexDir.SOUTH_EAST, "e");
    public static final HexAction GREATER = register(GREATER_ID, GREATER_PATTERN, stack -> {
        double right = stack.pop(DoubleIota.class).getValue();
        double left = stack.pop(DoubleIota.class).getValue();
        stack.push(new BooleanIota(left > right));
    });

    public static final ResourceLocation LESS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "less");
    public static final HexPattern LESS_PATTERN =
        pattern(HexDir.SOUTH_WEST, "q");
    public static final HexAction LESS = register(LESS_ID, LESS_PATTERN, stack -> {
        double right = stack.pop(DoubleIota.class).getValue();
        double left = stack.pop(DoubleIota.class).getValue();
        stack.push(new BooleanIota(left < right));
    });

    public static final ResourceLocation GREATER_EQ_ID =
        new ResourceLocation(HexAPI.MOD_ID, "greater_eq");
    public static final HexPattern GREATER_EQ_PATTERN =
        pattern(HexDir.SOUTH_EAST, "ee");
    public static final HexAction GREATER_EQ = register(GREATER_EQ_ID, GREATER_EQ_PATTERN, stack -> {
        double right = stack.pop(DoubleIota.class).getValue();
        double left = stack.pop(DoubleIota.class).getValue();
        stack.push(new BooleanIota(left >= right));
    });

    public static final ResourceLocation LESS_EQ_ID =
        new ResourceLocation(HexAPI.MOD_ID, "less_eq");
    public static final HexPattern LESS_EQ_PATTERN =
        pattern(HexDir.SOUTH_WEST, "qq");
    public static final HexAction LESS_EQ = register(LESS_EQ_ID, LESS_EQ_PATTERN, stack -> {
        double right = stack.pop(DoubleIota.class).getValue();
        double left = stack.pop(DoubleIota.class).getValue();
        stack.push(new BooleanIota(left <= right));
    });

    /** Append one Iota to the end of a list. */
    public static final ResourceLocation APPEND_ID =
        new ResourceLocation(HexAPI.MOD_ID, "append");
    public static final HexPattern APPEND_PATTERN =
        pattern(HexDir.SOUTH_WEST, "edqde");
    public static final HexAction APPEND = register(APPEND_ID, APPEND_PATTERN, stack -> {
        Iota value = stack.pop();
        ListIota list = stack.pop(ListIota.class);
        java.util.ArrayList<Iota> items = new java.util.ArrayList<>(list.getItems());
        items.add(value);
        stack.push(new ListIota(items));
    });

    /** Remove the last element from a list and return the shortened list and element. */
    public static final ResourceLocation UNAPPEND_ID =
        new ResourceLocation(HexAPI.MOD_ID, "unappend");
    public static final HexPattern UNAPPEND_PATTERN =
        pattern(HexDir.NORTH_WEST, "qaeaq");
    public static final HexAction UNAPPEND = register(UNAPPEND_ID, UNAPPEND_PATTERN, stack -> {
        ListIota list = stack.pop(ListIota.class);
        if (list.getItems().isEmpty()) {
            throw new CastingException("Cannot unappend an empty list");
        }
        java.util.List<Iota> items = list.getItems();
        stack.push(new ListIota(items.subList(0, items.size() - 1)));
        stack.push(items.get(items.size() - 1));
    });

    /** Read a zero-based integer index from a list. */
    public static final ResourceLocation INDEX_ID =
        new ResourceLocation(HexAPI.MOD_ID, "index");
    public static final HexPattern INDEX_PATTERN =
        pattern(HexDir.NORTH_WEST, "deeed");
    public static final HexAction INDEX = register(INDEX_ID, INDEX_PATTERN, stack -> {
        int index = requireInteger(stack.pop(DoubleIota.class),
            stack.peek() instanceof ListIota ? ((ListIota) stack.peek()).getItems().size() - 1 : -1);
        ListIota list = stack.pop(ListIota.class);
        stack.push(list.getItems().get(index));
    });

    /** Reverse the contents of a list without mutating the source Iota. */
    public static final ResourceLocation REVERSE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "reverse");
    public static final HexPattern REVERSE_PATTERN =
        pattern(HexDir.EAST, "qqqaede");
    public static final HexAction REVERSE = register(REVERSE_ID, REVERSE_PATTERN, stack -> {
        ListIota list = stack.pop(ListIota.class);
        java.util.ArrayList<Iota> items = new java.util.ArrayList<>(list.getItems());
        java.util.Collections.reverse(items);
        stack.push(new ListIota(items));
    });

    /** Consume a count and collect that many preceding stack values into a list. */
    public static final ResourceLocation LAST_N_LIST_ID =
        new ResourceLocation(HexAPI.MOD_ID, "last_n_list");
    public static final HexPattern LAST_N_LIST_PATTERN =
        pattern(HexDir.SOUTH_WEST, "ewdqdwe");
    public static final HexAction LAST_N_LIST = register(LAST_N_LIST_ID, LAST_N_LIST_PATTERN, stack -> {
        int count = requireInteger(stack.pop(DoubleIota.class), stack.size());
        java.util.ArrayList<Iota> items = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            items.add(stack.pop());
        }
        java.util.Collections.reverse(items);
        stack.push(new ListIota(items));
    });

    /** Ported arithmetic action from the 1.20.1 registry: sub. */
    public static final ResourceLocation SUB_ID =
        new ResourceLocation(HexAPI.MOD_ID, "sub");
    public static final HexPattern SUB_PATTERN =
        pattern(HexDir.NORTH_WEST, "wddw");
    public static final HexAction SUB = register(SUB_ID, SUB_PATTERN, stack -> {
        double right = stack.pop(DoubleIota.class).getValue();
        double left = stack.pop(DoubleIota.class).getValue();
        stack.push(new DoubleIota(left - right));
    });

    /** Ported arithmetic action from the 1.20.1 registry: mul. */
    public static final ResourceLocation MUL_DOT_ID =
        new ResourceLocation(HexAPI.MOD_ID, "mul");
    public static final HexPattern MUL_DOT_PATTERN =
        pattern(HexDir.SOUTH_EAST, "waqaw");
    public static final HexAction MUL_DOT = register(MUL_DOT_ID, MUL_DOT_PATTERN, stack -> {
        double right = stack.pop(DoubleIota.class).getValue();
        double left = stack.pop(DoubleIota.class).getValue();
        stack.push(new DoubleIota(left * right));
    });

    /** Ported arithmetic action from the 1.20.1 registry: div. */
    public static final ResourceLocation DIV_CROSS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "div");
    public static final HexPattern DIV_CROSS_PATTERN =
        pattern(HexDir.NORTH_EAST, "wdedw");
    public static final HexAction DIV_CROSS = register(DIV_CROSS_ID, DIV_CROSS_PATTERN, stack -> {
        double right = stack.pop(DoubleIota.class).getValue();
        double left = stack.pop(DoubleIota.class).getValue();
        stack.push(new DoubleIota(left / right));
    });

    /** Ported arithmetic action from the 1.20.1 registry: abs. */
    public static final ResourceLocation ABS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "abs");
    public static final HexPattern ABS_PATTERN =
        pattern(HexDir.NORTH_EAST, "wqaqw");
    public static final HexAction ABS = register(ABS_ID, ABS_PATTERN, stack -> {
        stack.push(new DoubleIota(Math.abs(stack.pop(DoubleIota.class).getValue())));
    });

    /** Ported arithmetic action from the 1.20.1 registry: pow. */
    public static final ResourceLocation POW_PROJ_ID =
        new ResourceLocation(HexAPI.MOD_ID, "pow");
    public static final HexPattern POW_PROJ_PATTERN =
        pattern(HexDir.NORTH_WEST, "wedew");
    public static final HexAction POW_PROJ = register(POW_PROJ_ID, POW_PROJ_PATTERN, stack -> {
        double exponent = stack.pop(DoubleIota.class).getValue();
        double base = stack.pop(DoubleIota.class).getValue();
        stack.push(new DoubleIota(Math.pow(base, exponent)));
    });

    /** Ported arithmetic action from the 1.20.1 registry: floor. */
    public static final ResourceLocation FLOOR_ID =
        new ResourceLocation(HexAPI.MOD_ID, "floor");
    public static final HexPattern FLOOR_PATTERN =
        pattern(HexDir.EAST, "ewq");
    public static final HexAction FLOOR = register(FLOOR_ID, FLOOR_PATTERN, stack -> {
        stack.push(new DoubleIota(Math.floor(stack.pop(DoubleIota.class).getValue())));
    });

    /** Ported arithmetic action from the 1.20.1 registry: ceil. */
    public static final ResourceLocation CEIL_ID =
        new ResourceLocation(HexAPI.MOD_ID, "ceil");
    public static final HexPattern CEIL_PATTERN =
        pattern(HexDir.EAST, "qwe");
    public static final HexAction CEIL = register(CEIL_ID, CEIL_PATTERN, stack -> {
        stack.push(new DoubleIota(Math.ceil(stack.pop(DoubleIota.class).getValue())));
    });

    /** Ported arithmetic action from the 1.20.1 registry: modulo. */
    public static final ResourceLocation MODULO_ID =
        new ResourceLocation(HexAPI.MOD_ID, "modulo");
    public static final HexPattern MODULO_PATTERN =
        pattern(HexDir.NORTH_EAST, "addwaad");
    public static final HexAction MODULO = register(MODULO_ID, MODULO_PATTERN, stack -> {
        double right = stack.pop(DoubleIota.class).getValue();
        double left = stack.pop(DoubleIota.class).getValue();
        stack.push(new DoubleIota(left % right));
    });
    /** Ported pure action from the 1.20.1 registry: const/null. */
    public static final ResourceLocation CONST_NULL_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/null");
    public static final HexPattern CONST_NULL_PATTERN =
        pattern(HexDir.EAST, "d");
    public static final HexAction CONST_NULL = register(CONST_NULL_ID, CONST_NULL_PATTERN, stack -> {
        stack.push(new NullIota());
    });

    /** Ported pure action from the 1.20.1 registry: const/true. */
    public static final ResourceLocation CONST_TRUE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/true");
    public static final HexPattern CONST_TRUE_PATTERN =
        pattern(HexDir.SOUTH_EAST, "aqae");
    public static final HexAction CONST_TRUE = register(CONST_TRUE_ID, CONST_TRUE_PATTERN, stack -> {
        stack.push(new BooleanIota(true));
    });

    /** Ported pure action from the 1.20.1 registry: const/false. */
    public static final ResourceLocation CONST_FALSE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/false");
    public static final HexPattern CONST_FALSE_PATTERN =
        pattern(HexDir.NORTH_EAST, "dedq");
    public static final HexAction CONST_FALSE = register(CONST_FALSE_ID, CONST_FALSE_PATTERN, stack -> {
        stack.push(new BooleanIota(false));
    });

    /** Ported pure action from the 1.20.1 registry: const/double/pi. */
    public static final ResourceLocation CONST_DOUBLE_PI_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/double/pi");
    public static final HexPattern CONST_DOUBLE_PI_PATTERN =
        pattern(HexDir.NORTH_EAST, "qdwdq");
    public static final HexAction CONST_DOUBLE_PI = register(CONST_DOUBLE_PI_ID, CONST_DOUBLE_PI_PATTERN, stack -> {
        stack.push(new DoubleIota(Math.PI));
    });

    /** Ported pure action from the 1.20.1 registry: const/double/tau. */
    public static final ResourceLocation CONST_DOUBLE_TAU_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/double/tau");
    public static final HexPattern CONST_DOUBLE_TAU_PATTERN =
        pattern(HexDir.NORTH_WEST, "eawae");
    public static final HexAction CONST_DOUBLE_TAU = register(CONST_DOUBLE_TAU_ID, CONST_DOUBLE_TAU_PATTERN, stack -> {
        stack.push(new DoubleIota(Math.PI * 2.0D));
    });

    /** Ported pure action from the 1.20.1 registry: const/double/e. */
    public static final ResourceLocation CONST_DOUBLE_E_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/double/e");
    public static final HexPattern CONST_DOUBLE_E_PATTERN =
        pattern(HexDir.EAST, "aaq");
    public static final HexAction CONST_DOUBLE_E = register(CONST_DOUBLE_E_ID, CONST_DOUBLE_E_PATTERN, stack -> {
        stack.push(new DoubleIota(Math.E));
    });

    /** Ported pure action from the 1.20.1 registry: const/double/phi. */
    public static final ResourceLocation CONST_DOUBLE_PHI_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/double/phi");
    public static final HexPattern CONST_DOUBLE_PHI_PATTERN =
        pattern(HexDir.NORTH_EAST, "wdded");
    public static final HexAction CONST_DOUBLE_PHI = register(CONST_DOUBLE_PHI_ID, CONST_DOUBLE_PHI_PATTERN, stack -> {
        stack.push(new DoubleIota((1.0D + Math.sqrt(5.0D)) / 2.0D));
    });

    /** Ported pure action from the 1.20.1 registry: and. */
    public static final ResourceLocation AND_ID =
        new ResourceLocation(HexAPI.MOD_ID, "and");
    public static final HexPattern AND_PATTERN =
        pattern(HexDir.NORTH_EAST, "wdw");
    public static final HexAction AND = register(AND_ID, AND_PATTERN, stack -> {
        boolean right = stack.pop(BooleanIota.class).getValue();
        boolean left = stack.pop(BooleanIota.class).getValue();
        stack.push(new BooleanIota(left && right));
    });

    /** Ported pure action from the 1.20.1 registry: or. */
    public static final ResourceLocation OR_ID =
        new ResourceLocation(HexAPI.MOD_ID, "or");
    public static final HexPattern OR_PATTERN =
        pattern(HexDir.SOUTH_EAST, "waw");
    public static final HexAction OR = register(OR_ID, OR_PATTERN, stack -> {
        boolean right = stack.pop(BooleanIota.class).getValue();
        boolean left = stack.pop(BooleanIota.class).getValue();
        stack.push(new BooleanIota(left || right));
    });
    /** Read the spell-local value, defaulting to the Null Iota. */
    public static final ResourceLocation READ_LOCAL_ID =
        new ResourceLocation(HexAPI.MOD_ID, "read/local");
    public static final HexPattern READ_LOCAL_PATTERN =
        pattern(HexDir.NORTH_EAST, "qeewdweddw");
    public static final HexAction READ_LOCAL = register(READ_LOCAL_ID, READ_LOCAL_PATTERN, stack ->
        stack.push(stack.readLocal()));

    /** Pop an Iota and store it in the spell-local slot. */
    public static final ResourceLocation WRITE_LOCAL_ID =
        new ResourceLocation(HexAPI.MOD_ID, "write/local");
    public static final HexPattern WRITE_LOCAL_PATTERN =
        pattern(HexDir.NORTH_WEST, "eqqwawqaaw");
    public static final HexAction WRITE_LOCAL = register(WRITE_LOCAL_ID, WRITE_LOCAL_PATTERN, stack ->
        stack.writeLocal(stack.pop()));

    /** Ported vector constant from the 1.20.1 registry: const/vec/px. */
    public static final ResourceLocation CONST_VEC_PX_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/vec/px");
    public static final HexPattern CONST_VEC_PX_PATTERN =
        pattern(HexDir.NORTH_WEST, "qqqqqea");
    public static final HexAction CONST_VEC_PX = register(CONST_VEC_PX_ID, CONST_VEC_PX_PATTERN, stack ->
        stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(1.0D, 0.0D, 0.0D))));

    /** Ported vector constant from the 1.20.1 registry: const/vec/py. */
    public static final ResourceLocation CONST_VEC_PY_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/vec/py");
    public static final HexPattern CONST_VEC_PY_PATTERN =
        pattern(HexDir.NORTH_WEST, "qqqqqew");
    public static final HexAction CONST_VEC_PY = register(CONST_VEC_PY_ID, CONST_VEC_PY_PATTERN, stack ->
        stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(0.0D, 1.0D, 0.0D))));

    /** Ported vector constant from the 1.20.1 registry: const/vec/pz. */
    public static final ResourceLocation CONST_VEC_PZ_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/vec/pz");
    public static final HexPattern CONST_VEC_PZ_PATTERN =
        pattern(HexDir.NORTH_WEST, "qqqqqed");
    public static final HexAction CONST_VEC_PZ = register(CONST_VEC_PZ_ID, CONST_VEC_PZ_PATTERN, stack ->
        stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(0.0D, 0.0D, 1.0D))));

    /** Ported vector constant from the 1.20.1 registry: const/vec/nx. */
    public static final ResourceLocation CONST_VEC_NX_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/vec/nx");
    public static final HexPattern CONST_VEC_NX_PATTERN =
        pattern(HexDir.SOUTH_WEST, "eeeeeqa");
    public static final HexAction CONST_VEC_NX = register(CONST_VEC_NX_ID, CONST_VEC_NX_PATTERN, stack ->
        stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(-1.0D, 0.0D, 0.0D))));

    /** Ported vector constant from the 1.20.1 registry: const/vec/ny. */
    public static final ResourceLocation CONST_VEC_NY_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/vec/ny");
    public static final HexPattern CONST_VEC_NY_PATTERN =
        pattern(HexDir.SOUTH_WEST, "eeeeeqw");
    public static final HexAction CONST_VEC_NY = register(CONST_VEC_NY_ID, CONST_VEC_NY_PATTERN, stack ->
        stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(0.0D, -1.0D, 0.0D))));

    /** Ported vector constant from the 1.20.1 registry: const/vec/nz. */
    public static final ResourceLocation CONST_VEC_NZ_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/vec/nz");
    public static final HexPattern CONST_VEC_NZ_PATTERN =
        pattern(HexDir.SOUTH_WEST, "eeeeeqd");
    public static final HexAction CONST_VEC_NZ = register(CONST_VEC_NZ_ID, CONST_VEC_NZ_PATTERN, stack ->
        stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(0.0D, 0.0D, -1.0D))));

    /** Ported vector constant from the 1.20.1 registry: const/vec/0. */
    public static final ResourceLocation CONST_VEC_ZERO_ID =
        new ResourceLocation(HexAPI.MOD_ID, "const/vec/0");
    public static final HexPattern CONST_VEC_ZERO_PATTERN =
        pattern(HexDir.NORTH_WEST, "qqqqq");
    public static final HexAction CONST_VEC_ZERO = register(CONST_VEC_ZERO_ID, CONST_VEC_ZERO_PATTERN, stack ->
        stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(0.0D, 0.0D, 0.0D))));
    /** Construct a vector from x, y, and z numeric stack values. */
    public static final ResourceLocation CONSTRUCT_VEC_ID =
        new ResourceLocation(HexAPI.MOD_ID, "construct_vec");
    public static final HexPattern CONSTRUCT_VEC_PATTERN =
        pattern(HexDir.EAST, "eqqqqq");
    public static final HexAction CONSTRUCT_VEC = register(CONSTRUCT_VEC_ID, CONSTRUCT_VEC_PATTERN, stack -> {
        double z = stack.pop(DoubleIota.class).getValue();
        double y = stack.pop(DoubleIota.class).getValue();
        double x = stack.pop(DoubleIota.class).getValue();
        stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(x, y, z)));
    });

    /** Expand a vector into x, y, and z numeric stack values. */
    public static final ResourceLocation DECONSTRUCT_VEC_ID =
        new ResourceLocation(HexAPI.MOD_ID, "deconstruct_vec");
    public static final HexPattern DECONSTRUCT_VEC_PATTERN =
        pattern(HexDir.EAST, "qeeeee");
    public static final HexAction DECONSTRUCT_VEC = register(DECONSTRUCT_VEC_ID, DECONSTRUCT_VEC_PATTERN, stack -> {
        net.minecraft.util.math.Vec3d value = stack.pop(Vec3Iota.class).getValue();
        stack.push(new DoubleIota(value.x));
        stack.push(new DoubleIota(value.y));
        stack.push(new DoubleIota(value.z));
    });
    /** Find the first equal Iota in a list, or -1 when absent. */
    public static final ResourceLocation INDEX_OF_ID =
        new ResourceLocation(HexAPI.MOD_ID, "index_of");
    public static final HexPattern INDEX_OF_PATTERN =
        pattern(HexDir.EAST, "dedqde");
    public static final HexAction INDEX_OF = register(INDEX_OF_ID, INDEX_OF_PATTERN, stack -> {
        Iota value = stack.pop();
        ListIota list = stack.pop(ListIota.class);
        int index = list.getItems().indexOf(value);
        stack.push(new DoubleIota(index));
    });

    /** Remove a zero-based list element and return the shortened list. */
    public static final ResourceLocation REMOVE_FROM_ID =
        new ResourceLocation(HexAPI.MOD_ID, "remove_from");
    public static final HexPattern REMOVE_FROM_PATTERN =
        pattern(HexDir.SOUTH_WEST, "edqdewaqa");
    public static final HexAction REMOVE_FROM = register(REMOVE_FROM_ID, REMOVE_FROM_PATTERN, stack -> {
        int index = requireInteger(stack.pop(DoubleIota.class), Integer.MAX_VALUE);
        ListIota list = stack.pop(ListIota.class);
        if (index >= list.getItems().size()) {
            throw new CastingException("List index out of bounds: " + index);
        }
        java.util.ArrayList<Iota> items = new java.util.ArrayList<>(list.getItems());
        items.remove(index);
        stack.push(new ListIota(items));
    });

    /** Return the half-open slice [start, end) of a list. */
    public static final ResourceLocation SLICE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "slice");
    public static final HexPattern SLICE_PATTERN =
        pattern(HexDir.NORTH_WEST, "qaeaqwded");
    public static final HexAction SLICE = register(SLICE_ID, SLICE_PATTERN, stack -> {
        int end = requireInteger(stack.pop(DoubleIota.class), Integer.MAX_VALUE);
        int start = requireInteger(stack.pop(DoubleIota.class), end);
        ListIota list = stack.pop(ListIota.class);
        if (end > list.getItems().size() || start > end) {
            throw new CastingException("Invalid list slice [" + start + ", " + end + ")");
        }
        stack.push(new ListIota(list.getItems().subList(start, end)));
    });

    /** Replace a zero-based list element and return the new list. */
    public static final ResourceLocation REPLACE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "replace");
    public static final HexPattern REPLACE_PATTERN =
        pattern(HexDir.NORTH_WEST, "wqaeaqw");
    public static final HexAction REPLACE = register(REPLACE_ID, REPLACE_PATTERN, stack -> {
        Iota value = stack.pop();
        int index = requireInteger(stack.pop(DoubleIota.class), Integer.MAX_VALUE);
        ListIota list = stack.pop(ListIota.class);
        if (index >= list.getItems().size()) {
            throw new CastingException("List index out of bounds: " + index);
        }
        java.util.ArrayList<Iota> items = new java.util.ArrayList<>(list.getItems());
        items.set(index, value);
        stack.push(new ListIota(items));
    });
    /** Rotate the top three stack values using OpTwiddling lookup [1, 2, 0]. */
    public static final ResourceLocation ROTATE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "rotate");
    public static final HexPattern ROTATE_PATTERN =
        pattern(HexDir.EAST, "aaeaa");
    public static final HexAction ROTATE = register(ROTATE_ID, ROTATE_PATTERN, stack -> {
        Iota top = stack.pop();
        Iota middle = stack.pop();
        Iota bottom = stack.pop();
        stack.push(middle);
        stack.push(top);
        stack.push(bottom);
    });

    /** Reverse-rotate the top three stack values using lookup [2, 0, 1]. */
    public static final ResourceLocation ROTATE_REVERSE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "rotate_reverse");
    public static final HexPattern ROTATE_REVERSE_PATTERN =
        pattern(HexDir.NORTH_EAST, "ddqdd");
    public static final HexAction ROTATE_REVERSE = register(ROTATE_REVERSE_ID, ROTATE_REVERSE_PATTERN, stack -> {
        Iota top = stack.pop();
        Iota middle = stack.pop();
        Iota bottom = stack.pop();
        stack.push(top);
        stack.push(bottom);
        stack.push(middle);
    });

    /** Duplicate the second-from-top Iota using OpTwiddling lookup [0, 1, 0]. */
    public static final ResourceLocation OVER_ID =
        new ResourceLocation(HexAPI.MOD_ID, "over");
    public static final HexPattern OVER_PATTERN =
        pattern(HexDir.EAST, "aaedd");
    public static final HexAction OVER = register(OVER_ID, OVER_PATTERN, stack -> {
        Iota top = stack.pop();
        Iota belowTop = stack.pop();
        stack.push(belowTop);
        stack.push(top);
        stack.push(belowTop);
    });

    /** Push the current number of Iotas on the stack. */
    public static final ResourceLocation STACK_LEN_ID =
        new ResourceLocation(HexAPI.MOD_ID, "stack_len");
    public static final HexPattern STACK_LEN_PATTERN =
        pattern(HexDir.NORTH_WEST, "qwaeawqaeaqa");
    public static final HexAction STACK_LEN = register(STACK_LEN_ID, STACK_LEN_PATTERN, stack ->
        stack.push(new DoubleIota(stack.size())));
    private HexActions() {
    }

    public static void touch() {
        // Referencing a static field forces class initialization and registry population.
        if (PUSH_ZERO == null || PUSH_ONE == null || DUPLICATE == null
            || SWAP == null || ADD == null || NOT == null
            || EMPTY_LIST == null || SINGLETON == null || SPLAT == null
            || EQUALITY == null || TYPE_EQUALITY == null || COERCE_TO_BOOL == null
            || BOOL_IF == null || GREATER == null || LESS == null || GREATER_EQ == null
            || LESS_EQ == null || APPEND == null || UNAPPEND == null || INDEX == null
            || REVERSE == null || LAST_N_LIST == null) {
            throw new IllegalStateException("Hex action registry failed to initialize");
        }
    }

    private static HexAction register(ResourceLocation id, HexPattern pattern, HexAction action) {
        return HexActionRegistry.register(id, pattern, action);
    }

    private static int requireInteger(DoubleIota value, int maxInclusive) throws CastingException {
        double raw = value.getValue();
        if (Double.isNaN(raw) || Double.isInfinite(raw) || raw != Math.rint(raw)
            || raw < 0.0D || raw > maxInclusive || raw > Integer.MAX_VALUE) {
            throw new CastingException("Expected an integer in [0, " + maxInclusive
                + "] but found " + raw);
        }
        return (int) raw;
    }

    private static HexPattern pattern(HexDir start, HexAngle... angles) {
        return new HexPattern(start, Arrays.asList(angles));
    }

    /**
     * Parse the same keyboard notation used by Hex Casting's 1.20.1 source.
     * The six keys form the relative-turn wheel: w/e/d/s/a/q.
     */
    private static HexPattern pattern(HexDir start, String angleChars) {
        HexAngle[] angles = new HexAngle[angleChars.length()];
        for (int i = 0; i < angleChars.length(); i++) {
            switch (angleChars.charAt(i)) {
                case 'w':
                    angles[i] = HexAngle.FORWARD;
                    break;
                case 'e':
                    angles[i] = HexAngle.RIGHT;
                    break;
                case 'd':
                    angles[i] = HexAngle.RIGHT_BACK;
                    break;
                case 's':
                    angles[i] = HexAngle.BACK;
                    break;
                case 'a':
                    angles[i] = HexAngle.LEFT_BACK;
                    break;
                case 'q':
                    angles[i] = HexAngle.LEFT;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown Hex angle character: "
                        + angleChars.charAt(i));
            }
        }
        return pattern(start, angles);
    }
}
