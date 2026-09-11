package at.petra_k.hexcasting.common.lib.hex;

import at.petra_k.hexcasting.api.HexAPI;
import at.petra_k.hexcasting.api.casting.action.HexAction;
import at.petra_k.hexcasting.api.casting.action.OperationAction;
import at.petra_k.hexcasting.common.casting.ForEachAction;
import at.petra_k.hexcasting.api.casting.action.StackOperationAction;
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
import at.petra_k.hexcasting.common.casting.HexArithmetics;
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
public static final HexPattern DUPLICATE_PATTERN =
        pattern(HexDir.EAST, "aadaa");
    public static final HexAction DUPLICATE = register(DUPLICATE_ID, DUPLICATE_PATTERN, stack ->
        stack.push(stack.peek()));

    public static final ResourceLocation SWAP_ID = new ResourceLocation(HexAPI.MOD_ID, "swap");
public static final HexPattern SWAP_PATTERN =
        pattern(HexDir.EAST, "aawdd");
    public static final HexAction SWAP = register(SWAP_ID, SWAP_PATTERN, stack -> {
        at.petra_k.hexcasting.api.casting.iota.Iota top = stack.pop();
        at.petra_k.hexcasting.api.casting.iota.Iota below = stack.pop();
        stack.push(top);
        stack.push(below);
    });

    public static final ResourceLocation ADD_ID = new ResourceLocation(HexAPI.MOD_ID, "add");
public static final HexPattern ADD_PATTERN =
        pattern(HexDir.NORTH_EAST, "waaw");
    public static final HexAction ADD = register(ADD_ID, ADD_PATTERN,
        new OperationAction(2, HexArithmetics::add));

    public static final ResourceLocation NOT_ID = new ResourceLocation(HexAPI.MOD_ID, "not");
public static final HexPattern NOT_PATTERN =
        pattern(HexDir.NORTH_WEST, "dw");
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
public static final HexPattern EQUALITY_PATTERN =
        pattern(HexDir.EAST, "ad");
    public static final HexAction EQUALITY = register(EQUALITY_ID, EQUALITY_PATTERN, stack -> {
        Iota right = stack.pop();
        Iota left = stack.pop();
        stack.push(new BooleanIota(Iota.tolerates(left, right)));
    });

    /** Tolerant value inequality, complementary to equals. */
    public static final ResourceLocation NOT_EQUALS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "not_equals");
    public static final HexPattern NOT_EQUALS_PATTERN =
        pattern(HexDir.EAST, "da");
    public static final HexAction NOT_EQUALS = register(NOT_EQUALS_ID, NOT_EQUALS_PATTERN, stack -> {
        Iota right = stack.pop();
        Iota left = stack.pop();
        stack.push(new BooleanIota(!Iota.tolerates(left, right)));
    });

    /** Compare only the kinds of two stack values, ignoring their payloads. */
    public static final ResourceLocation TYPE_EQUALITY_ID = new ResourceLocation(HexAPI.MOD_ID, "type_equals");
public static final HexPattern TYPE_EQUALITY_PATTERN =
        pattern(HexDir.EAST, "wawdw");
    public static final HexAction TYPE_EQUALITY = register(TYPE_EQUALITY_ID, TYPE_EQUALITY_PATTERN, stack -> {
        Iota right = stack.pop();
        Iota left = stack.pop();
        stack.push(new BooleanIota(left.getType() == right.getType()));
    });

    /** Inequality of Iota kinds, ignoring payloads. */
    public static final ResourceLocation TYPE_NOT_EQUALS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "type_not_equals");
    public static final HexPattern TYPE_NOT_EQUALS_PATTERN =
        pattern(HexDir.EAST, "wdwaw");
    public static final HexAction TYPE_NOT_EQUALS = register(
        TYPE_NOT_EQUALS_ID, TYPE_NOT_EQUALS_PATTERN, stack -> {
            Iota right = stack.pop();
            Iota left = stack.pop();
            stack.push(new BooleanIota(left.getType() != right.getType()));
        });

    /** Convert any Iota's truthiness to an explicit Boolean Iota. */
    public static final ResourceLocation COERCE_TO_BOOL_ID = new ResourceLocation(HexAPI.MOD_ID, "bool_coerce");
public static final HexPattern COERCE_TO_BOOL_PATTERN =
        pattern(HexDir.NORTH_EAST, "aw");
    public static final HexAction COERCE_TO_BOOL = register(COERCE_TO_BOOL_ID, COERCE_TO_BOOL_PATTERN, stack ->
        stack.push(new BooleanIota(stack.pop().isTruthy())));

    /** Select the true or false branch; stack order is condition, true value, false value. */
    public static final ResourceLocation BOOL_IF_ID = new ResourceLocation(HexAPI.MOD_ID, "if");
public static final HexPattern BOOL_IF_PATTERN =
        pattern(HexDir.SOUTH_EAST, "awdd");
    public static final HexAction BOOL_IF = register(BOOL_IF_ID, BOOL_IF_PATTERN,
        new at.petra_k.hexcasting.common.casting.BranchAction());

    /** Push a uniformly distributed double in the half-open interval [0, 1). */
    public static final ResourceLocation RANDOM_ID =
        new ResourceLocation(HexAPI.MOD_ID, "random");
    public static final HexPattern RANDOM_PATTERN =
        pattern(HexDir.NORTH_WEST, "eqqq");
    public static final HexAction RANDOM = register(RANDOM_ID, RANDOM_PATTERN,
        stack -> stack.push(new DoubleIota(Math.random())));

    /** Numeric comparison actions copied from the 1.20.1 pure stack semantics. */
    public static final ResourceLocation GREATER_ID =
        new ResourceLocation(HexAPI.MOD_ID, "greater");
    public static final HexPattern GREATER_PATTERN =
        pattern(HexDir.SOUTH_EAST, "e");
    public static final HexAction GREATER = register(GREATER_ID, GREATER_PATTERN,
        new OperationAction(2, at.petra_k.hexcasting.common.casting.IotaArithmetics::greater));

    public static final ResourceLocation LESS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "less");
    public static final HexPattern LESS_PATTERN =
        pattern(HexDir.SOUTH_WEST, "q");
    public static final HexAction LESS = register(LESS_ID, LESS_PATTERN,
        new OperationAction(2, at.petra_k.hexcasting.common.casting.IotaArithmetics::less));

    public static final ResourceLocation GREATER_EQ_ID =
        new ResourceLocation(HexAPI.MOD_ID, "greater_eq");
    public static final HexPattern GREATER_EQ_PATTERN =
        pattern(HexDir.SOUTH_EAST, "ee");
    public static final HexAction GREATER_EQ = register(GREATER_EQ_ID, GREATER_EQ_PATTERN,
        new OperationAction(2, at.petra_k.hexcasting.common.casting.IotaArithmetics::greaterEq));

    public static final ResourceLocation LESS_EQ_ID =
        new ResourceLocation(HexAPI.MOD_ID, "less_eq");
    public static final HexPattern LESS_EQ_PATTERN =
        pattern(HexDir.SOUTH_WEST, "qq");
    public static final HexAction LESS_EQ = register(LESS_EQ_ID, LESS_EQ_PATTERN,
        new OperationAction(2, at.petra_k.hexcasting.common.casting.IotaArithmetics::lessEq));

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
    public static final HexAction SUB = register(SUB_ID, SUB_PATTERN,
        new OperationAction(2, HexArithmetics::subtract));

    /** Ported arithmetic action from the 1.20.1 registry: mul. */
    public static final ResourceLocation MUL_DOT_ID =
        new ResourceLocation(HexAPI.MOD_ID, "mul");
    public static final HexPattern MUL_DOT_PATTERN =
        pattern(HexDir.SOUTH_EAST, "waqaw");
    public static final HexAction MUL_DOT = register(MUL_DOT_ID, MUL_DOT_PATTERN, new OperationAction(2, HexArithmetics::multiply));

    /** Ported arithmetic action from the 1.20.1 registry: div. */
    public static final ResourceLocation DIV_CROSS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "div");
    public static final HexPattern DIV_CROSS_PATTERN =
        pattern(HexDir.NORTH_EAST, "wdedw");
    public static final HexAction DIV_CROSS = register(DIV_CROSS_ID, DIV_CROSS_PATTERN, new OperationAction(2, HexArithmetics::divide));

    /** Ported arithmetic action from the 1.20.1 registry: abs. */
    public static final ResourceLocation ABS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "abs");
    public static final HexPattern ABS_PATTERN =
        pattern(HexDir.NORTH_EAST, "wqaqw");
    public static final HexAction ABS = register(ABS_ID, ABS_PATTERN,
        new OperationAction(1, HexArithmetics::absolute));

    /** Ported arithmetic action from the 1.20.1 registry: pow. */
    public static final ResourceLocation POW_PROJ_ID =
        new ResourceLocation(HexAPI.MOD_ID, "pow");
    public static final HexPattern POW_PROJ_PATTERN =
        pattern(HexDir.NORTH_WEST, "wedew");
    public static final HexAction POW_PROJ = register(POW_PROJ_ID, POW_PROJ_PATTERN, new OperationAction(2, HexArithmetics::power));

    /** Ported arithmetic action from the 1.20.1 registry: floor. */
    public static final ResourceLocation FLOOR_ID =
        new ResourceLocation(HexAPI.MOD_ID, "floor");
    public static final HexPattern FLOOR_PATTERN =
        pattern(HexDir.EAST, "ewq");
    public static final HexAction FLOOR = register(FLOOR_ID, FLOOR_PATTERN,
        new OperationAction(1, HexArithmetics::floor));

    /** Ported arithmetic action from the 1.20.1 registry: ceil. */
    public static final ResourceLocation CEIL_ID =
        new ResourceLocation(HexAPI.MOD_ID, "ceil");
    public static final HexPattern CEIL_PATTERN =
        pattern(HexDir.EAST, "qwe");
    public static final HexAction CEIL = register(CEIL_ID, CEIL_PATTERN,
        new OperationAction(1, HexArithmetics::ceil));

    /** Ported arithmetic action from the 1.20.1 registry: modulo. */
    public static final ResourceLocation MODULO_ID =
        new ResourceLocation(HexAPI.MOD_ID, "modulo");
    public static final HexPattern MODULO_PATTERN =
        pattern(HexDir.NORTH_EAST, "addwaad");
    public static final HexAction MODULO = register(MODULO_ID, MODULO_PATTERN,
        new OperationAction(2, HexArithmetics::modulo));
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
    public static final HexAction AND = register(AND_ID, AND_PATTERN,
        new OperationAction(2, at.petra_k.hexcasting.common.casting.IotaArithmetics::and));

    /** Ported pure action from the 1.20.1 registry: or. */
    public static final ResourceLocation OR_ID =
        new ResourceLocation(HexAPI.MOD_ID, "or");
    public static final HexPattern OR_PATTERN =
        pattern(HexDir.SOUTH_EAST, "waw");
    public static final HexAction OR = register(OR_ID, OR_PATTERN,
        new OperationAction(2, at.petra_k.hexcasting.common.casting.IotaArithmetics::or));
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
    /** Duplicate the second-from-top value above the top value (OpTwiddling [1, 0, 1]). */
    public static final ResourceLocation TUCK_ID =
        new ResourceLocation(HexAPI.MOD_ID, "tuck");
    public static final HexPattern TUCK_PATTERN =
        pattern(HexDir.EAST, "ddqaa");
    public static final HexAction TUCK = register(TUCK_ID, TUCK_PATTERN, stack -> {
        Iota top = stack.pop();
        Iota belowTop = stack.pop();
        stack.push(top);
        stack.push(belowTop);
        stack.push(top);
    });

    /** Duplicate the top two values (OpTwiddling [0, 1, 0, 1]). */
    public static final ResourceLocation TWO_DUP_ID =
        new ResourceLocation(HexAPI.MOD_ID, "2dup");
    public static final HexPattern TWO_DUP_PATTERN =
        pattern(HexDir.EAST, "aadadaaw");
    public static final HexAction TWO_DUP = register(TWO_DUP_ID, TWO_DUP_PATTERN, stack -> {
        Iota top = stack.pop();
        Iota belowTop = stack.pop();
        stack.push(belowTop);
        stack.push(top);
        stack.push(belowTop);
        stack.push(top);
    });

    /** Duplicate the top stack value N times, consuming the value and count. */
    public static final ResourceLocation DUPLICATE_N_ID =
        new ResourceLocation(HexAPI.MOD_ID, "duplicate_n");
    public static final HexPattern DUPLICATE_N_PATTERN =
        pattern(HexDir.EAST, "aadaadaa");
    public static final HexAction DUPLICATE_N = register(DUPLICATE_N_ID, DUPLICATE_N_PATTERN, stack -> {
        int count = requireInteger(stack.pop(DoubleIota.class), Iota.MAX_SERIALIZATION_TOTAL);
        Iota value = stack.pop();
        for (int i = 0; i < count; i++) {
            stack.push(value);
        }
    });

    /** Move a stack value by the signed depth encoded on top of the stack. */
    public static final ResourceLocation FISHERMAN_ID =
        new ResourceLocation(HexAPI.MOD_ID, "fisherman");
    public static final HexPattern FISHERMAN_PATTERN =
        pattern(HexDir.WEST, "ddad");
    public static final HexAction FISHERMAN = register(FISHERMAN_ID, FISHERMAN_PATTERN, stack -> {
        DoubleIota depthIota = stack.pop(DoubleIota.class);
        int maxDepth = stack.size() - 1;
        int depth = requireSignedInteger(depthIota, maxDepth);
        java.util.ArrayList<Iota> values = new java.util.ArrayList<>(stack.snapshot());
        if (depth >= 0) {
            Iota fish = values.remove(values.size() - 1 - depth);
            values.add(fish);
        } else {
            Iota lure = values.remove(values.size() - 1);
            values.add(values.size() + depth, lure);
        }
        stack.restore(values);
    });
    /** Copy a value at signed depth without removing the original. */
    public static final ResourceLocation FISHERMAN_COPY_ID =
        new ResourceLocation(HexAPI.MOD_ID, "fisherman/copy");
    public static final HexPattern FISHERMAN_COPY_PATTERN =
        pattern(HexDir.EAST, "aada");
    public static final HexAction FISHERMAN_COPY = register(
        FISHERMAN_COPY_ID, FISHERMAN_COPY_PATTERN, stack -> {
            DoubleIota depthIota = stack.pop(DoubleIota.class);
            int maxDepth = stack.size() - 1;
            int depth = requireSignedInteger(depthIota, maxDepth);
            java.util.List<Iota> values = stack.snapshot();
            if (depth >= 0) {
                stack.push(values.get(values.size() - 1 - depth));
            } else {
                Iota lure = values.get(values.size() - 1);
                java.util.ArrayList<Iota> reordered = new java.util.ArrayList<>(values);
                // Match the 1.20.1 implementation: depth -1 inserts directly
                // below the lure, while the minimum depth inserts at index 0.
                reordered.add(reordered.size() - 1 + depth, lure);
                stack.restore(reordered);
            }
        });

   /** Execute a code list once for every value in a data list. */
    /** Remove duplicate Iotas while preserving their first-occurrence order. */
    public static final ResourceLocation UNIQUE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "unique");
    public static final HexPattern UNIQUE_PATTERN =
        pattern(HexDir.NORTH_EAST, "aweaqa");
    public static final HexAction UNIQUE = register(UNIQUE_ID, UNIQUE_PATTERN,
        new OperationAction(1, arguments -> {
            ListIota list = (ListIota) arguments.get(0);
            java.util.ArrayList<Iota> unique = new java.util.ArrayList<>();
            for (Iota value : list.getItems()) {
                if (!containsTolerant(unique, value)) {
                    unique.add(value);
                }
            }
            return new ListIota(unique);
        }));
    /** Execute a code list once for every value in a data list. */
    public static final ResourceLocation FOR_EACH_ID =
        new ResourceLocation(HexAPI.MOD_ID, "for_each");
    public static final HexPattern FOR_EACH_PATTERN =
        pattern(HexDir.NORTH_WEST, "qaeaqeqedqde");
    public static final HexAction FOR_EACH = register(FOR_EACH_ID, FOR_EACH_PATTERN,
        new at.petra_k.hexcasting.common.casting.ForEachAction());

    /** Mark the next Iota as literal instead of executing it. */
    public static final ResourceLocation ESCAPE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "escape");
    public static final HexPattern ESCAPE_PATTERN =
        pattern(HexDir.WEST, "qqqaw");
    public static final HexAction ESCAPE = register(ESCAPE_ID, ESCAPE_PATTERN,
        new at.petra_k.hexcasting.common.casting.ParenControlAction(
            at.petra_k.hexcasting.common.casting.ParenControlAction.Kind.ESCAPE));

    /** Runtime escape: make the next evaluated Iota a literal value. */
    public static final ResourceLocation RUNTIME_ESCAPE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "runtime_escape");
    public static final HexPattern RUNTIME_ESCAPE_PATTERN =
        pattern(HexDir.SOUTH_EAST, "wdeee");
    public static final HexAction RUNTIME_ESCAPE = register(
        RUNTIME_ESCAPE_ID, RUNTIME_ESCAPE_PATTERN,
        new at.petra_k.hexcasting.common.casting.ParenControlAction(
            at.petra_k.hexcasting.common.casting.ParenControlAction.Kind.RUNTIME_ESCAPE));

    /** Begin capturing executable Iotas into a list. */
    public static final ResourceLocation OPEN_PAREN_ID =
        new ResourceLocation(HexAPI.MOD_ID, "open_paren");
    public static final HexPattern OPEN_PAREN_PATTERN =
        pattern(HexDir.WEST, "qqq");
    public static final HexAction OPEN_PAREN = register(OPEN_PAREN_ID, OPEN_PAREN_PATTERN,
        new at.petra_k.hexcasting.common.casting.ParenControlAction(
            at.petra_k.hexcasting.common.casting.ParenControlAction.Kind.OPEN));

    /** Finish a parenthesized list and place it on the enclosing context. */
    public static final ResourceLocation CLOSE_PAREN_ID =
        new ResourceLocation(HexAPI.MOD_ID, "close_paren");
    public static final HexPattern CLOSE_PAREN_PATTERN =
        pattern(HexDir.EAST, "eee");
    public static final HexAction CLOSE_PAREN = register(CLOSE_PAREN_ID, CLOSE_PAREN_PATTERN,
        new at.petra_k.hexcasting.common.casting.ParenControlAction(
            at.petra_k.hexcasting.common.casting.ParenControlAction.Kind.CLOSE));

    /** Open N nested parenthesis capture frames using the stack count. */
    public static final ResourceLocation OPEN_N_PARENS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "open_n_parens");
    public static final HexPattern OPEN_N_PARENS_PATTERN =
        pattern(HexDir.WEST, "qdaqadq");
    public static final HexAction OPEN_N_PARENS = register(
        OPEN_N_PARENS_ID, OPEN_N_PARENS_PATTERN,
        new at.petra_k.hexcasting.common.casting.ParenControlAction(
            at.petra_k.hexcasting.common.casting.ParenControlAction.Kind.OPEN_N));

    /** Close every currently open parenthesis frame. */
    public static final ResourceLocation CLOSE_ALL_PARENS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "close_all_parens");
    public static final HexPattern CLOSE_ALL_PARENS_PATTERN =
        pattern(HexDir.EAST, "eadedae");
    public static final HexAction CLOSE_ALL_PARENS = register(
        CLOSE_ALL_PARENS_ID, CLOSE_ALL_PARENS_PATTERN,
        new at.petra_k.hexcasting.common.casting.ParenControlAction(
            at.petra_k.hexcasting.common.casting.ParenControlAction.Kind.CLOSE_ALL));

    /** Move one stack value into the current parenthesized code list. */
    public static final ResourceLocation READ_INTO_PARENS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "read_into_parens");
    public static final HexPattern READ_INTO_PARENS_PATTERN =
        pattern(HexDir.EAST, "aqqqqqwded");
    public static final HexAction READ_INTO_PARENS = register(
        READ_INTO_PARENS_ID, READ_INTO_PARENS_PATTERN,
        new at.petra_k.hexcasting.common.casting.ParenControlAction(
            at.petra_k.hexcasting.common.casting.ParenControlAction.Kind.READ_INTO));

    /** Undo the latest captured value while building a parenthesized list. */
    public static final ResourceLocation UNDO_ID =
        new ResourceLocation(HexAPI.MOD_ID, "undo");
    public static final HexPattern UNDO_PATTERN =
        pattern(HexDir.EAST, "eeedw");
    public static final HexAction UNDO = register(
        UNDO_ID, UNDO_PATTERN,
        new at.petra_k.hexcasting.common.casting.ParenControlAction(
            at.petra_k.hexcasting.common.casting.ParenControlAction.Kind.UNDO));

    /** Halt the active cast and discard remaining continuation frames. */
    public static final ResourceLocation HALT_ID =
        new ResourceLocation(HexAPI.MOD_ID, "halt");
    public static final HexPattern HALT_PATTERN =
        pattern(HexDir.SOUTH_WEST, "aqdee");
    public static final HexAction HALT = register(HALT_ID, HALT_PATTERN,
        new at.petra_k.hexcasting.common.casting.HaltAction());

    /** Evaluate a list of executable Iotas in the active VM. */
    public static final ResourceLocation EVAL_ID =
        new ResourceLocation(HexAPI.MOD_ID, "eval");
    public static final HexPattern EVAL_PATTERN =
        pattern(HexDir.SOUTH_EAST, "deaqq");
    public static final HexAction EVAL = register(EVAL_ID, EVAL_PATTERN,
        new at.petra_k.hexcasting.common.casting.EvalAction());
    /** Evaluate a code list with an isolated, breakable continuation boundary. */
    public static final ResourceLocation EVAL_CC_ID =
        new ResourceLocation(HexAPI.MOD_ID, "eval/cc");
    public static final HexPattern EVAL_CC_PATTERN =
        pattern(HexDir.NORTH_WEST, "qwaqde");
    public static final HexAction EVAL_CC = register(EVAL_CC_ID, EVAL_CC_PATTERN,
        new at.petra_k.hexcasting.common.casting.EvalBreakableAction());

    /** Transcendental numeric operators migrated from the 1.20.1 action table. */
    public static final ResourceLocation SIN_ID =
        new ResourceLocation(HexAPI.MOD_ID, "sin");
    public static final HexPattern SIN_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqaa");
    public static final HexAction SIN = register(SIN_ID,
        SIN_PATTERN, new OperationAction(1, HexArithmetics::sine));

    public static final ResourceLocation COS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "cos");
    public static final HexPattern COS_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqad");
    public static final HexAction COS = register(COS_ID,
        COS_PATTERN, new OperationAction(1, HexArithmetics::cosine));

    public static final ResourceLocation TAN_ID =
        new ResourceLocation(HexAPI.MOD_ID, "tan");
    public static final HexPattern TAN_PATTERN =
        pattern(HexDir.SOUTH_WEST, "wqqqqqadq");
    public static final HexAction TAN = register(TAN_ID,
        TAN_PATTERN, new OperationAction(1, HexArithmetics::tangent));

    public static final ResourceLocation ARCSIN_ID =
        new ResourceLocation(HexAPI.MOD_ID, "arcsin");
    public static final HexPattern ARCSIN_PATTERN =
        pattern(HexDir.SOUTH_EAST, "ddeeeee");
    public static final HexAction ARCSIN = register(ARCSIN_ID,
        ARCSIN_PATTERN, new OperationAction(1, HexArithmetics::arcsine));

    public static final ResourceLocation ARCCOS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "arccos");
    public static final HexPattern ARCCOS_PATTERN =
        pattern(HexDir.NORTH_EAST, "adeeeee");
    public static final HexAction ARCCOS = register(ARCCOS_ID,
        ARCCOS_PATTERN, new OperationAction(1, HexArithmetics::arccosine));

    public static final ResourceLocation ARCTAN_ID =
        new ResourceLocation(HexAPI.MOD_ID, "arctan");
    public static final HexPattern ARCTAN_PATTERN =
        pattern(HexDir.NORTH_EAST, "eadeeeeew");
    public static final HexAction ARCTAN = register(ARCTAN_ID,
        ARCTAN_PATTERN, new OperationAction(1, HexArithmetics::arctangent));

    public static final ResourceLocation ARCTAN2_ID =
        new ResourceLocation(HexAPI.MOD_ID, "arctan2");
    public static final HexPattern ARCTAN2_PATTERN =
        pattern(HexDir.WEST, "deadeeeeewd");
    public static final HexAction ARCTAN2 = register(ARCTAN2_ID,
        ARCTAN2_PATTERN, new OperationAction(2, HexArithmetics::arctangent2));

    public static final ResourceLocation LOG_ID =
        new ResourceLocation(HexAPI.MOD_ID, "logarithm");
    public static final HexPattern LOG_PATTERN =
        pattern(HexDir.NORTH_WEST, "eqaqe");
    public static final HexAction LOG = register(LOG_ID,
        LOG_PATTERN, new OperationAction(2, HexArithmetics::logarithm));

    /** Symmetric difference for two lists. */
    public static final ResourceLocation XOR_ID =
        new ResourceLocation(HexAPI.MOD_ID, "xor");
    public static final HexPattern XOR_PATTERN =
        pattern(HexDir.NORTH_WEST, "dwa");
    public static final HexAction XOR = register(XOR_ID, XOR_PATTERN,
        new OperationAction(2, at.petra_k.hexcasting.common.casting.IotaArithmetics::xor));

    /** Prefix an Iota to a list. The list is the lower stack argument. */
    public static final ResourceLocation CONS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "cons");
    public static final HexPattern CONS_PATTERN =
        pattern(HexDir.SOUTH_EAST, "ddewedd");
    public static final HexAction CONS = register(CONS_ID, CONS_PATTERN, stack -> {
        Iota value = stack.pop();
        ListIota list = stack.pop(ListIota.class);
        java.util.ArrayList<Iota> items = new java.util.ArrayList<>();
        items.add(value);
        items.addAll(list.getItems());
        stack.push(new ListIota(items));
    });

    /** Remove the first Iota from a non-empty list, returning tail then head. */
    public static final ResourceLocation UNCONS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "uncons");
    public static final HexPattern UNCONS_PATTERN =
        pattern(HexDir.SOUTH_WEST, "aaqwqaa");
    public static final HexAction UNCONS = register(UNCONS_ID, UNCONS_PATTERN, stack -> {
        ListIota list = stack.pop(ListIota.class);
        if (list.getItems().isEmpty()) {
            throw new CastingException("Cannot uncons an empty list");
        }
        java.util.List<Iota> items = list.getItems();
        stack.push(new ListIota(new java.util.ArrayList<>(items.subList(1, items.size()))));
        stack.push(items.get(0));
    });
    private HexActions() {
    }

    private static boolean containsTolerant(java.util.List<Iota> values, Iota needle) {
        for (Iota value : values) {
            if (Iota.tolerates(value, needle)) {
                return true;
            }
        }
        return false;
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

    private static int requireSignedInteger(DoubleIota value, int maxAbs) throws CastingException {
        double raw = value.getValue();
        if (Double.isNaN(raw) || Double.isInfinite(raw) || raw != Math.rint(raw)
            || raw < -maxAbs || raw > maxAbs) {
            throw new CastingException("Expected an integer in [" + (-maxAbs) + ", " + maxAbs
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
