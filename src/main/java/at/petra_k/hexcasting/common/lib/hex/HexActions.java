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
import at.petra_k.hexcasting.api.casting.iota.EntityIota;
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
import at.petra_k.hexcasting.api.capability.IHexCastingData;
import at.petra_k.hexcasting.api.casting.eval.vm.CastingVM;
import at.petra_k.hexcasting.api.misc.MediaConstants;
import at.petra_k.hexcasting.common.casting.MediaInventoryHelper;

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

    /** Print the top Iota without consuming it. */
    public static final ResourceLocation PRINT_ID =
        new ResourceLocation(HexAPI.MOD_ID, "print");
    public static final HexPattern PRINT_PATTERN =
        pattern(HexDir.NORTH_EAST, "de");
    public static final HexAction PRINT = register(PRINT_ID, PRINT_PATTERN,
        new at.petra_k.hexcasting.common.casting.PrintAction());

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
            stack.push(list);
            stack.push(new NullIota());
            return;
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
        DoubleIota indexIota = stack.pop(DoubleIota.class);
        ListIota list = stack.pop(ListIota.class);
        int index = requireRoundedInteger(indexIota);
        if (index < 0 || index >= list.getItems().size()) {
            stack.push(new NullIota());
        } else {
            stack.push(list.getItems().get(index));
        }
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
        int index = -1;
        for (int i = 0; i < list.getItems().size(); i++) {
            if (Iota.tolerates(list.getItems().get(i), value)) {
                index = i;
                break;
            }
        }
        stack.push(new DoubleIota(index));
    });

    /** Remove a zero-based list element and return the shortened list. */
    public static final ResourceLocation REMOVE_FROM_ID =
        new ResourceLocation(HexAPI.MOD_ID, "remove_from");
    public static final HexPattern REMOVE_FROM_PATTERN =
        pattern(HexDir.SOUTH_WEST, "edqdewaqa");
    public static final HexAction REMOVE_FROM = register(REMOVE_FROM_ID, REMOVE_FROM_PATTERN, stack -> {
        DoubleIota indexIota = stack.pop(DoubleIota.class);
        ListIota list = stack.pop(ListIota.class);
        int index = requireRoundedInteger(indexIota);
        if (index < 0 || index >= list.getItems().size()) {
            stack.push(list);
            return;
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
        DoubleIota index1Iota = stack.pop(DoubleIota.class);
        DoubleIota index0Iota = stack.pop(DoubleIota.class);
        ListIota list = stack.pop(ListIota.class);
        int index0 = requireInteger(index0Iota, list.getItems().size());
        int index1 = requireInteger(index1Iota, list.getItems().size());
        if (index0 == index1) {
            stack.push(new ListIota(java.util.Collections.<Iota>emptyList()));
            return;
        }
        int start = Math.min(index0, index1);
        int end = Math.max(index0, index1);
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
            throw new CastingException("hexcasting.error.list_index_out_of_bounds");
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
    /**
     * Reorder the top N stack values using a factorial-number-system
     * (Lehmer-code) index. The code is consumed from the top of the stack;
     * values below the selected window are left untouched.
     */
    public static final ResourceLocation SWIZZLE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "swizzle");
    public static final HexPattern SWIZZLE_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qaawdde");
    public static final HexAction SWIZZLE = register(SWIZZLE_ID, SWIZZLE_PATTERN, stack -> {
        java.util.List<Iota> before = stack.snapshot();
        try {
            long code = requireNonNegativeLong(stack.pop(DoubleIota.class));
            java.util.ArrayList<Iota> values = new java.util.ArrayList<>(stack.snapshot());
            int width = swizzleWidth(code);
            if (width > values.size()) {
                throw new CastingException("hexcasting.error.swizzle_width");
            }
            int start = values.size() - width;
            java.util.ArrayList<Iota> selected = new java.util.ArrayList<>(
                values.subList(start, values.size()));
            java.util.ArrayList<Iota> reordered = decodeLehmer(selected, code);
            values.subList(start, values.size()).clear();
            values.addAll(reordered);
            stack.restore(values);
        } catch (CastingException exception) {
            stack.restore(before);
            throw exception;
        } catch (RuntimeException exception) {
            stack.restore(before);
            throw exception;
        }
    });

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
    public static final ResourceLocation CONSTRUCT_ID =
        new ResourceLocation(HexAPI.MOD_ID, "construct");
    public static final HexPattern CONSTRUCT_PATTERN =
        pattern(HexDir.SOUTH_EAST, "ddewedd");
    public static final HexAction CONSTRUCT = register(CONSTRUCT_ID, CONSTRUCT_PATTERN, stack -> {
        Iota value = stack.pop();
        ListIota list = stack.pop(ListIota.class);
        java.util.ArrayList<Iota> items = new java.util.ArrayList<>();
        items.add(value);
        items.addAll(list.getItems());
        stack.push(new ListIota(items));
    });

    /** Remove the first Iota from a non-empty list, returning tail then head. */
    public static final ResourceLocation DECONSTRUCT_ID =
        new ResourceLocation(HexAPI.MOD_ID, "deconstruct");
    public static final HexPattern DECONSTRUCT_PATTERN =
        pattern(HexDir.SOUTH_WEST, "aaqwqaa");
    public static final HexAction DECONSTRUCT = register(
        DECONSTRUCT_ID, DECONSTRUCT_PATTERN, stack -> {
        ListIota list = stack.pop(ListIota.class);
        if (list.getItems().isEmpty()) {
            stack.push(list);
            stack.push(new NullIota());
            return;
        }
        java.util.List<Iota> items = list.getItems();
        stack.push(new ListIota(new java.util.ArrayList<>(items.subList(1, items.size()))));
        stack.push(items.get(0));
    });
    private static long requireNonNegativeLong(DoubleIota value) throws CastingException {
        double raw = value.getValue();
        if (Double.isNaN(raw) || Double.isInfinite(raw) || raw != Math.rint(raw)
            || raw < 0.0D || raw > 9.007199254740991E15D) {
            throw new CastingException("hexcasting.error.non_negative_integer");
        }
        return (long) raw;
    }

    /** Return the number of factorial strides required by the code. */
    private static int swizzleWidth(long code) throws CastingException {
        long factorial = 1L;
        long multiplier = 1L;
        int width = 0;
        while (factorial <= code) {
            width++;
            if (width >= 20 || factorial > Long.MAX_VALUE / multiplier) {
                throw new CastingException("hexcasting.error.swizzle_too_large");
            }
            factorial *= multiplier;
            multiplier++;
        }
        return width;
    }

    /** Decode a Lehmer code into a permutation of the selected values. */
    private static java.util.ArrayList<Iota> decodeLehmer(
        java.util.List<Iota> selected, long code) {
        java.util.ArrayList<Iota> remaining = new java.util.ArrayList<>(selected);
        java.util.ArrayList<Iota> reordered = new java.util.ArrayList<>(selected.size());
        long factorial = 1L;
        for (int i = 2; i < selected.size(); i++) {
            factorial *= i;
        }
        for (int radix = selected.size(); radix > 0; radix--) {
            int index = (int) ((code / factorial) % radix);
            reordered.add(remaining.remove(index));
            if (radix > 1) {
                factorial /= (radix - 1L);
            }
        }
        return reordered;
    }

    /** Raycast from an origin along a direction, returning a block position or Null. */
    /** Break the block at a vector position and drop its contents. */
    /** Ignite an air block at a vector position. */
    public static final ResourceLocation IGNITE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "ignite");
    public static final HexPattern IGNITE_PATTERN =
        pattern(HexDir.SOUTH_EAST, "aaqawawa");
    public static final HexAction IGNITE = register(
        IGNITE_ID, IGNITE_PATTERN, new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.ignite_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                net.minecraft.entity.player.EntityPlayer player = vm.getPlayer();
                if (player == null) {
                    throw new CastingException("hexcasting.error.ignite_context");
                }
                Vec3Iota positionIota = stack.pop(Vec3Iota.class);
                net.minecraft.util.math.Vec3d position = positionIota.getValue();
                net.minecraft.util.math.BlockPos blockPos = new net.minecraft.util.math.BlockPos(
                    (int) Math.floor(position.x), (int) Math.floor(position.y),
                    (int) Math.floor(position.z));
                if (player.world.isAirBlock(blockPos)
                    && player.world.isBlockModifiable(player, blockPos)
                    && player.canPlayerEdit(blockPos, net.minecraft.util.EnumFacing.UP,
                        net.minecraft.item.ItemStack.EMPTY)) {
                    vm.consumeMedia(MediaConstants.DUST_UNIT);
                    player.world.setBlockState(blockPos,
                        net.minecraft.init.Blocks.FIRE.getDefaultState(), 3);
                }
            }
        });

    /** Extinguish a fire block at a vector position. */
    public static final ResourceLocation EXTINGUISH_ID =
        new ResourceLocation(HexAPI.MOD_ID, "extinguish");
    public static final HexPattern EXTINGUISH_PATTERN =
        pattern(HexDir.SOUTH_WEST, "ddedwdwd");
    public static final HexAction EXTINGUISH = register(
        EXTINGUISH_ID, EXTINGUISH_PATTERN, new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.extinguish_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                net.minecraft.entity.player.EntityPlayer player = vm.getPlayer();
                if (player == null) {
                    throw new CastingException("hexcasting.error.extinguish_context");
                }
                Vec3Iota positionIota = stack.pop(Vec3Iota.class);
                net.minecraft.util.math.Vec3d position = positionIota.getValue();
                net.minecraft.util.math.BlockPos blockPos = new net.minecraft.util.math.BlockPos(
                    (int) Math.floor(position.x), (int) Math.floor(position.y),
                    (int) Math.floor(position.z));
                if (player.world.getBlockState(blockPos).getBlock()
                    == net.minecraft.init.Blocks.FIRE
                    && player.world.isBlockModifiable(player, blockPos)
                    && player.canPlayerEdit(blockPos, net.minecraft.util.EnumFacing.UP,
                        net.minecraft.item.ItemStack.EMPTY)) {
                    vm.consumeMedia(MediaConstants.DUST_UNIT / 10L);
                    player.world.setBlockToAir(blockPos);
                }
            }
        });

    /** Add a motion vector to an entity's current velocity. */
    public static final ResourceLocation ADD_MOTION_ID =
        new ResourceLocation(HexAPI.MOD_ID, "add_motion");
    public static final HexPattern ADD_MOTION_PATTERN =
        pattern(HexDir.SOUTH_WEST, "awqqqwaqw");
    public static final HexAction ADD_MOTION = register(
        ADD_MOTION_ID, ADD_MOTION_PATTERN, new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.add_motion_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                if (vm.getPlayer() == null) {
                    throw new CastingException("hexcasting.error.add_motion_context");
                }
                Iota first = stack.pop();
                Iota second = stack.pop();
                Vec3Iota motionIota;
                EntityIota entityIota;
                if (first instanceof Vec3Iota && second instanceof EntityIota) {
                    motionIota = (Vec3Iota) first;
                    entityIota = (EntityIota) second;
                } else if (first instanceof EntityIota && second instanceof Vec3Iota) {
                    entityIota = (EntityIota) first;
                    motionIota = (Vec3Iota) second;
                } else {
                    throw new CastingException("hexcasting.error.add_motion_args");
                }
                net.minecraft.entity.Entity entity = resolveEntity(entityIota, vm);
                net.minecraft.util.math.Vec3d motion = motionIota.getValue();
                double motionLengthSquared = motion.x * motion.x
                    + motion.y * motion.y + motion.z * motion.z;
                double motionCost = Math.min(8192.0D, motionLengthSquared);
                vm.consumeMedia((long) (MediaConstants.DUST_UNIT * motionCost));
                if (motionLengthSquared > 8192.0D * 8192.0D) {
                    motion = motion.scale(8192.0D / Math.sqrt(motionLengthSquared));
                }
                entity.motionX += motion.x;
                entity.motionY += motion.y;
                entity.motionZ += motion.z;
                entity.velocityChanged = true;
            }
        });

    /** Play a note at a vector position using an instrument and note number. */
    public static final ResourceLocation BEEP_ID =
        new ResourceLocation(HexAPI.MOD_ID, "beep");
    public static final HexPattern BEEP_PATTERN =
        pattern(HexDir.WEST, "adaa");
    public static final HexAction BEEP = register(
        BEEP_ID, BEEP_PATTERN, new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.beep_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                net.minecraft.entity.player.EntityPlayer player = vm.getPlayer();
                if (player == null) {
                    throw new CastingException("hexcasting.error.beep_context");
                }
                DoubleIota noteIota = stack.pop(DoubleIota.class);
                DoubleIota instrumentIota = stack.pop(DoubleIota.class);
                Vec3Iota positionIota = stack.pop(Vec3Iota.class);
                int note = (int) Math.rint(noteIota.getValue());
                int instrument = (int) Math.rint(instrumentIota.getValue());
                if (note < 0 || note > 24 || instrument < 0 || instrument > 9
                    || Double.isNaN(noteIota.getValue()) || Double.isNaN(instrumentIota.getValue())) {
                    throw new CastingException("hexcasting.error.beep_args");
                }
                vm.consumeMedia(MediaConstants.DUST_UNIT / 10L);
                net.minecraft.util.SoundEvent sound;
                switch (instrument) {
                    case 1: sound = net.minecraft.init.SoundEvents.BLOCK_NOTE_BASS; break;
                    case 2: sound = net.minecraft.init.SoundEvents.BLOCK_NOTE_BASEDRUM; break;
                    case 3: sound = net.minecraft.init.SoundEvents.BLOCK_NOTE_SNARE; break;
                    case 4: sound = net.minecraft.init.SoundEvents.BLOCK_NOTE_HAT; break;
                    case 5: sound = net.minecraft.init.SoundEvents.BLOCK_NOTE_GUITAR; break;
                    case 6: sound = net.minecraft.init.SoundEvents.BLOCK_NOTE_FLUTE; break;
                    case 7: sound = net.minecraft.init.SoundEvents.BLOCK_NOTE_BELL; break;
                    case 8: sound = net.minecraft.init.SoundEvents.BLOCK_NOTE_CHIME; break;
                    case 9: sound = net.minecraft.init.SoundEvents.BLOCK_NOTE_XYLOPHONE; break;
                    default: sound = net.minecraft.init.SoundEvents.BLOCK_NOTE_HARP; break;
                }
                net.minecraft.util.math.Vec3d position = positionIota.getValue();
                net.minecraft.util.math.BlockPos blockPos = new net.minecraft.util.math.BlockPos(
                    (int) Math.floor(position.x), (int) Math.floor(position.y),
                    (int) Math.floor(position.z));
                float pitch = (float) Math.pow(2.0D, (note - 12) / 12.0D);
                player.world.playSound(null, blockPos, sound,
                    net.minecraft.util.SoundCategory.RECORDS, 3.0F, pitch);
            }
        });

    /** Summon rain in the current world. */
    public static final ResourceLocation SUMMON_RAIN_ID =
        new ResourceLocation(HexAPI.MOD_ID, "summon_rain");
    public static final HexPattern SUMMON_RAIN_PATTERN =
        pattern(HexDir.WEST, "wwweeewwweewdawdwad");
    public static final HexAction SUMMON_RAIN = register(
        SUMMON_RAIN_ID, SUMMON_RAIN_PATTERN, new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.weather_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                net.minecraft.entity.player.EntityPlayer player = vm.getPlayer();
                if (player == null) {
                    throw new CastingException("hexcasting.error.weather_context");
                }
                vm.consumeMedia(MediaConstants.CRYSTAL_UNIT);
                net.minecraft.world.storage.WorldInfo info = player.world.getWorldInfo();
                int rainTime = (30 + player.world.rand.nextInt(60)) * 20 * 60;
                info.setCleanWeatherTime(0);
                info.setRaining(true);
                info.setRainTime(rainTime);
                info.setThundering(player.world.rand.nextDouble() < 0.05D);
                info.setThunderTime(rainTime);
            }
        });

    /** Dispel rain in the current world. */
    public static final ResourceLocation DISPEL_RAIN_ID =
        new ResourceLocation(HexAPI.MOD_ID, "dispel_rain");
    public static final HexPattern DISPEL_RAIN_PATTERN =
        pattern(HexDir.EAST, "eeewwweeewwaqqddqdqd");
    public static final HexAction DISPEL_RAIN = register(
        DISPEL_RAIN_ID, DISPEL_RAIN_PATTERN, new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.weather_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                net.minecraft.entity.player.EntityPlayer player = vm.getPlayer();
                if (player == null) {
                    throw new CastingException("hexcasting.error.weather_context");
                }
                vm.consumeMedia(MediaConstants.SHARD_UNIT);
                net.minecraft.world.storage.WorldInfo info = player.world.getWorldInfo();
                int clearTime = (60 + player.world.rand.nextInt(120)) * 20 * 60;
                info.setCleanWeatherTime(clearTime);
                info.setRaining(false);
                info.setRainTime(0);
                info.setThundering(false);
                info.setThunderTime(0);
            }
        });

    /** Create a non-flaming explosion at a vector position. */
    public static final ResourceLocation EXPLODE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "explode");
    public static final HexPattern EXPLODE_PATTERN =
        pattern(HexDir.EAST, "aawaawaa");
    public static final HexAction EXPLODE = register(
        EXPLODE_ID, EXPLODE_PATTERN, explosionAction(false));

    /** Create a flaming explosion at a vector position. */
    public static final ResourceLocation EXPLODE_FIRE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "explode/fire");
    public static final HexPattern EXPLODE_FIRE_PATTERN =
        pattern(HexDir.EAST, "ddwddwdd");
    public static final HexAction EXPLODE_FIRE = register(
        EXPLODE_FIRE_ID, EXPLODE_FIRE_PATTERN, explosionAction(true));

    private static HexAction explosionAction(final boolean fire) {
        return new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.explode_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                net.minecraft.entity.player.EntityPlayer player = vm.getPlayer();
                if (player == null) {
                    throw new CastingException("hexcasting.error.explode_context");
                }
                DoubleIota strengthIota = stack.pop(DoubleIota.class);
                Vec3Iota positionIota = stack.pop(Vec3Iota.class);
                double strength = strengthIota.getValue();
                if (Double.isNaN(strength) || Double.isInfinite(strength)
                    || strength < 0.0D || strength > 10.0D) {
                    throw new CastingException("hexcasting.error.explode_args");
                }
                long mediaCost = (long) Math.ceil(MediaConstants.DUST_UNIT
                    * (3.0D * strength + (fire ? 1.0D : 0.125D)));
                vm.consumeMedia(mediaCost);
                net.minecraft.util.math.Vec3d position = positionIota.getValue();
                player.world.newExplosion(player, position.x, position.y, position.z,
                    (float) strength, fire, true);
            }
        };
    }

    public static final ResourceLocation BREAK_BLOCK_ID =
        new ResourceLocation(HexAPI.MOD_ID, "break_block");
    public static final HexPattern BREAK_BLOCK_PATTERN =
        pattern(HexDir.EAST, "qaqqqqq");
    public static final HexAction BREAK_BLOCK = register(
        BREAK_BLOCK_ID, BREAK_BLOCK_PATTERN, new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.break_block_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                net.minecraft.entity.player.EntityPlayer player = vm.getPlayer();
                if (player == null) {
                    throw new CastingException("hexcasting.error.break_block_context");
                }
                Vec3Iota positionIota = stack.pop(Vec3Iota.class);
                net.minecraft.util.math.Vec3d position = positionIota.getValue();
                net.minecraft.util.math.BlockPos blockPos = new net.minecraft.util.math.BlockPos(
                    (int) Math.floor(position.x), (int) Math.floor(position.y),
                    (int) Math.floor(position.z));
                if (player.world.isBlockModifiable(player, blockPos)
                    && player.canPlayerEdit(blockPos, net.minecraft.util.EnumFacing.UP,
                        net.minecraft.item.ItemStack.EMPTY)) {
                    vm.consumeMedia(MediaConstants.DUST_UNIT / 8L);
                    player.world.destroyBlock(blockPos, true);
                }
            }
        });

    public static final ResourceLocation RAYCAST_ID =
        new ResourceLocation(HexAPI.MOD_ID, "raycast");
    public static final HexPattern RAYCAST_PATTERN =
        pattern(HexDir.EAST, "wqaawdd");
    public static final HexAction RAYCAST = register(RAYCAST_ID, RAYCAST_PATTERN,
        new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.raycast_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                if (vm.getPlayer() == null) {
                    throw new CastingException("hexcasting.error.raycast_context");
                }
                Vec3Iota direction = stack.pop(Vec3Iota.class);
                Vec3Iota origin = stack.pop(Vec3Iota.class);
                net.minecraft.util.math.Vec3d start = origin.getValue();
                net.minecraft.util.math.Vec3d vector = direction.getValue();
                if (vector.lengthVector() == 0.0D) {
                    throw new CastingException("hexcasting.error.raycast_zero");
                }
                net.minecraft.util.math.Vec3d end = start.add(vector.normalize().scale(64.0D));
                net.minecraft.util.math.RayTraceResult hit = vm.getPlayer().world.rayTraceBlocks(
                    start, end, false, false, false);
                if (hit == null || hit.typeOfHit != net.minecraft.util.math.RayTraceResult.Type.BLOCK
                    || hit.getBlockPos() == null) {
                    stack.push(new NullIota());
                } else {
                    net.minecraft.util.math.BlockPos pos = hit.getBlockPos();
                    stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(
                        pos.getX(), pos.getY(), pos.getZ())));
                }
            }
        });
    /** Return the hit block face normal as a vector, or Null when no block is hit. */
    public static final ResourceLocation RAYCAST_AXIS_ID =
        new ResourceLocation(HexAPI.MOD_ID, "raycast/axis");
    public static final HexPattern RAYCAST_AXIS_PATTERN =
        pattern(HexDir.EAST, "weddwaa");
    public static final HexAction RAYCAST_AXIS = register(
        RAYCAST_AXIS_ID, RAYCAST_AXIS_PATTERN,
        new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.raycast_axis_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                if (vm.getPlayer() == null) {
                    throw new CastingException("hexcasting.error.raycast_axis_context");
                }
                Vec3Iota direction = stack.pop(Vec3Iota.class);
                Vec3Iota origin = stack.pop(Vec3Iota.class);
                net.minecraft.util.math.Vec3d start = origin.getValue();
                net.minecraft.util.math.Vec3d vector = direction.getValue();
                if (vector.lengthVector() == 0.0D) {
                    throw new CastingException("hexcasting.error.raycast_axis_zero");
                }
                net.minecraft.util.math.Vec3d end = start.add(
                    vector.normalize().scale(64.0D));
                net.minecraft.util.math.RayTraceResult hit = vm.getPlayer().world.rayTraceBlocks(
                    start, end, false, false, false);
                if (hit == null || hit.typeOfHit != net.minecraft.util.math.RayTraceResult.Type.BLOCK
                    || hit.sideHit == null) {
                    stack.push(new NullIota());
                } else {
                    net.minecraft.util.EnumFacing face = hit.sideHit;
                    stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(
                        face.getFrontOffsetX(), face.getFrontOffsetY(), face.getFrontOffsetZ())));
                }
            }
        });
    /** Raycast from an origin along a direction, returning the nearest entity or Null. */
    public static final ResourceLocation RAYCAST_ENTITY_ID =
        new ResourceLocation(HexAPI.MOD_ID, "raycast/entity");
    public static final HexPattern RAYCAST_ENTITY_PATTERN =
        pattern(HexDir.EAST, "weaqa");
    public static final HexAction RAYCAST_ENTITY = register(
        RAYCAST_ENTITY_ID, RAYCAST_ENTITY_PATTERN,
        new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.raycast_entity_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                net.minecraft.entity.player.EntityPlayer caster = vm.getPlayer();
                if (caster == null) {
                    throw new CastingException("hexcasting.error.raycast_entity_context");
                }
                Vec3Iota direction = stack.pop(Vec3Iota.class);
                Vec3Iota origin = stack.pop(Vec3Iota.class);
                net.minecraft.util.math.Vec3d start = origin.getValue();
                net.minecraft.util.math.Vec3d vector = direction.getValue();
                double length = vector.lengthVector();
                if (length == 0.0D) {
                    throw new CastingException("hexcasting.error.raycast_entity_zero");
                }
                net.minecraft.util.math.Vec3d end = start.add(
                    vector.scale(64.0D / length));
                net.minecraft.util.math.AxisAlignedBB search = new net.minecraft.util.math.AxisAlignedBB(
                    Math.min(start.x, end.x), Math.min(start.y, end.y), Math.min(start.z, end.z),
                    Math.max(start.x, end.x), Math.max(start.y, end.y), Math.max(start.z, end.z))
                    .grow(1.0D);
                java.util.List<net.minecraft.entity.Entity> candidates =
                    caster.world.getEntitiesWithinAABBExcludingEntity(caster, search);
                net.minecraft.entity.Entity nearest = null;
                double nearestDistance = Double.MAX_VALUE;
                for (net.minecraft.entity.Entity candidate : candidates) {
                    if (candidate == null || !candidate.canBeCollidedWith()) {
                        continue;
                    }
                    net.minecraft.util.math.AxisAlignedBB box = candidate.getEntityBoundingBox();
                    if (box == null) {
                        continue;
                    }
                    float border = candidate.getCollisionBorderSize();
                    if (border > 0.0F) {
                        box = box.grow(border);
                    }
                    net.minecraft.util.math.RayTraceResult intercept =
                        box.calculateIntercept(start, end);
                    if (intercept == null || intercept.hitVec == null) {
                        continue;
                    }
                    double distance = start.squareDistanceTo(intercept.hitVec);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = candidate;
                    }
                }
                stack.push(nearest == null ? new NullIota() : new EntityIota(nearest));
            }
        });
    /** Select the nearest entity centered on a position Iota. */
    public static final ResourceLocation GET_ENTITY_ID =
        new ResourceLocation(HexAPI.MOD_ID, "get_entity");
    public static final HexPattern GET_ENTITY_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqdaqa");
    public static final HexAction GET_ENTITY = register(
        GET_ENTITY_ID, GET_ENTITY_PATTERN, entityAtAction(entity -> true));

    /** Select the nearest animal centered on a position Iota. */
    public static final ResourceLocation GET_ENTITY_ANIMAL_ID =
        new ResourceLocation(HexAPI.MOD_ID, "get_entity/animal");
    public static final HexPattern GET_ENTITY_ANIMAL_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqdaqaawa");
    public static final HexAction GET_ENTITY_ANIMAL = register(
        GET_ENTITY_ANIMAL_ID, GET_ENTITY_ANIMAL_PATTERN,
        entityAtAction(entity -> entity instanceof net.minecraft.entity.passive.EntityAnimal));

    /** Select the nearest monster centered on a position Iota. */
    public static final ResourceLocation GET_ENTITY_MONSTER_ID =
        new ResourceLocation(HexAPI.MOD_ID, "get_entity/monster");
    public static final HexPattern GET_ENTITY_MONSTER_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqdaqaawq");
    public static final HexAction GET_ENTITY_MONSTER = register(
        GET_ENTITY_MONSTER_ID, GET_ENTITY_MONSTER_PATTERN,
        entityAtAction(entity -> entity instanceof net.minecraft.entity.monster.IMob));

    /** Select the nearest dropped item centered on a position Iota. */
    public static final ResourceLocation GET_ENTITY_ITEM_ID =
        new ResourceLocation(HexAPI.MOD_ID, "get_entity/item");
    public static final HexPattern GET_ENTITY_ITEM_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqdaqaaww");
    public static final HexAction GET_ENTITY_ITEM = register(
        GET_ENTITY_ITEM_ID, GET_ENTITY_ITEM_PATTERN,
        entityAtAction(entity -> entity instanceof net.minecraft.entity.item.EntityItem));

    /** Select the nearest player centered on a position Iota. */
    public static final ResourceLocation GET_ENTITY_PLAYER_ID =
        new ResourceLocation(HexAPI.MOD_ID, "get_entity/player");
    public static final HexPattern GET_ENTITY_PLAYER_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqdaqaawe");
    public static final HexAction GET_ENTITY_PLAYER = register(
        GET_ENTITY_PLAYER_ID, GET_ENTITY_PLAYER_PATTERN,
        entityAtAction(entity -> entity instanceof net.minecraft.entity.player.EntityPlayer));

    /** Select the nearest living entity centered on a position Iota. */
    public static final ResourceLocation GET_ENTITY_LIVING_ID =
        new ResourceLocation(HexAPI.MOD_ID, "get_entity/living");
    public static final HexPattern GET_ENTITY_LIVING_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqdaqaawd");
    public static final HexAction GET_ENTITY_LIVING = register(
        GET_ENTITY_LIVING_ID, GET_ENTITY_LIVING_PATTERN,
        entityAtAction(entity -> entity instanceof net.minecraft.entity.EntityLivingBase));

    private static HexAction entityAtAction(
        final java.util.function.Predicate<net.minecraft.entity.Entity> predicate) {
        return new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.get_entity_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                net.minecraft.entity.player.EntityPlayer player = vm.getPlayer();
                if (player == null) {
                    throw new CastingException("hexcasting.error.get_entity_context");
                }
                Vec3Iota positionIota = stack.pop(Vec3Iota.class);
                net.minecraft.util.math.Vec3d position = positionIota.getValue();
                net.minecraft.util.math.AxisAlignedBB area =
                    new net.minecraft.util.math.AxisAlignedBB(
                        position.x - 0.5D, position.y - 0.5D, position.z - 0.5D,
                        position.x + 0.5D, position.y + 0.5D, position.z + 0.5D);
                net.minecraft.entity.Entity nearest = null;
                double nearestDistance = Double.MAX_VALUE;
                for (net.minecraft.entity.Entity candidate : player.world.getEntitiesWithinAABB(
                    net.minecraft.entity.Entity.class, area)) {
                    if (candidate == null || candidate.isDead || !predicate.test(candidate)) {
                        continue;
                    }
                    double dx = candidate.posX - position.x;
                    double dy = candidate.posY - position.y;
                    double dz = candidate.posZ - position.z;
                    double distance = dx * dx + dy * dy + dz * dz;
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = candidate;
                    }
                }
                stack.push(nearest == null ? new NullIota() : new EntityIota(nearest));
            }
        };
    }

    /** Select all matching entities within a radius of a position Iota. */
    public static final ResourceLocation ZONE_ENTITY_ID =
        new ResourceLocation(HexAPI.MOD_ID, "zone_entity");
    public static final HexPattern ZONE_ENTITY_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqwded");
    public static final HexAction ZONE_ENTITY = register(
        ZONE_ENTITY_ID, ZONE_ENTITY_PATTERN, zoneEntitiesAction(entity -> true, false));

    public static final ResourceLocation ZONE_ENTITY_ANIMAL_ID =
        new ResourceLocation(HexAPI.MOD_ID, "zone_entity/animal");
    public static final HexPattern ZONE_ENTITY_ANIMAL_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqwdeddwa");
    public static final HexAction ZONE_ENTITY_ANIMAL = register(
        ZONE_ENTITY_ANIMAL_ID, ZONE_ENTITY_ANIMAL_PATTERN,
        zoneEntitiesAction(entity -> entity instanceof net.minecraft.entity.passive.EntityAnimal, false));

    public static final ResourceLocation ZONE_ENTITY_NOT_ANIMAL_ID =
        new ResourceLocation(HexAPI.MOD_ID, "zone_entity/not_animal");
    public static final HexPattern ZONE_ENTITY_NOT_ANIMAL_PATTERN =
        pattern(HexDir.NORTH_EAST, "eeeeewaqaawa");
    public static final HexAction ZONE_ENTITY_NOT_ANIMAL = register(
        ZONE_ENTITY_NOT_ANIMAL_ID, ZONE_ENTITY_NOT_ANIMAL_PATTERN,
        zoneEntitiesAction(entity -> entity instanceof net.minecraft.entity.passive.EntityAnimal, true));

    public static final ResourceLocation ZONE_ENTITY_MONSTER_ID =
        new ResourceLocation(HexAPI.MOD_ID, "zone_entity/monster");
    public static final HexPattern ZONE_ENTITY_MONSTER_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqwdeddwq");
    public static final HexAction ZONE_ENTITY_MONSTER = register(
        ZONE_ENTITY_MONSTER_ID, ZONE_ENTITY_MONSTER_PATTERN,
        zoneEntitiesAction(entity -> entity instanceof net.minecraft.entity.monster.IMob, false));

    public static final ResourceLocation ZONE_ENTITY_NOT_MONSTER_ID =
        new ResourceLocation(HexAPI.MOD_ID, "zone_entity/not_monster");
    public static final HexPattern ZONE_ENTITY_NOT_MONSTER_PATTERN =
        pattern(HexDir.NORTH_EAST, "eeeeewaqaawq");
    public static final HexAction ZONE_ENTITY_NOT_MONSTER = register(
        ZONE_ENTITY_NOT_MONSTER_ID, ZONE_ENTITY_NOT_MONSTER_PATTERN,
        zoneEntitiesAction(entity -> entity instanceof net.minecraft.entity.monster.IMob, true));

    public static final ResourceLocation ZONE_ENTITY_ITEM_ID =
        new ResourceLocation(HexAPI.MOD_ID, "zone_entity/item");
    public static final HexPattern ZONE_ENTITY_ITEM_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqwdeddww");
    public static final HexAction ZONE_ENTITY_ITEM = register(
        ZONE_ENTITY_ITEM_ID, ZONE_ENTITY_ITEM_PATTERN,
        zoneEntitiesAction(entity -> entity instanceof net.minecraft.entity.item.EntityItem, false));

    public static final ResourceLocation ZONE_ENTITY_NOT_ITEM_ID =
        new ResourceLocation(HexAPI.MOD_ID, "zone_entity/not_item");
    public static final HexPattern ZONE_ENTITY_NOT_ITEM_PATTERN =
        pattern(HexDir.NORTH_EAST, "eeeeewaqaaww");
    public static final HexAction ZONE_ENTITY_NOT_ITEM = register(
        ZONE_ENTITY_NOT_ITEM_ID, ZONE_ENTITY_NOT_ITEM_PATTERN,
        zoneEntitiesAction(entity -> entity instanceof net.minecraft.entity.item.EntityItem, true));

    public static final ResourceLocation ZONE_ENTITY_PLAYER_ID =
        new ResourceLocation(HexAPI.MOD_ID, "zone_entity/player");
    public static final HexPattern ZONE_ENTITY_PLAYER_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqwdeddwe");
    public static final HexAction ZONE_ENTITY_PLAYER = register(
        ZONE_ENTITY_PLAYER_ID, ZONE_ENTITY_PLAYER_PATTERN,
        zoneEntitiesAction(entity -> entity instanceof net.minecraft.entity.player.EntityPlayer, false));

    public static final ResourceLocation ZONE_ENTITY_NOT_PLAYER_ID =
        new ResourceLocation(HexAPI.MOD_ID, "zone_entity/not_player");
    public static final HexPattern ZONE_ENTITY_NOT_PLAYER_PATTERN =
        pattern(HexDir.NORTH_EAST, "eeeeewaqaawe");
    public static final HexAction ZONE_ENTITY_NOT_PLAYER = register(
        ZONE_ENTITY_NOT_PLAYER_ID, ZONE_ENTITY_NOT_PLAYER_PATTERN,
        zoneEntitiesAction(entity -> entity instanceof net.minecraft.entity.player.EntityPlayer, true));

    public static final ResourceLocation ZONE_ENTITY_LIVING_ID =
        new ResourceLocation(HexAPI.MOD_ID, "zone_entity/living");
    public static final HexPattern ZONE_ENTITY_LIVING_PATTERN =
        pattern(HexDir.SOUTH_EAST, "qqqqqwdeddwd");
    public static final HexAction ZONE_ENTITY_LIVING = register(
        ZONE_ENTITY_LIVING_ID, ZONE_ENTITY_LIVING_PATTERN,
        zoneEntitiesAction(entity -> entity instanceof net.minecraft.entity.EntityLivingBase, false));

    public static final ResourceLocation ZONE_ENTITY_NOT_LIVING_ID =
        new ResourceLocation(HexAPI.MOD_ID, "zone_entity/not_living");
    public static final HexPattern ZONE_ENTITY_NOT_LIVING_PATTERN =
        pattern(HexDir.NORTH_EAST, "eeeeewaqaawd");
    public static final HexAction ZONE_ENTITY_NOT_LIVING = register(
        ZONE_ENTITY_NOT_LIVING_ID, ZONE_ENTITY_NOT_LIVING_PATTERN,
        zoneEntitiesAction(entity -> entity instanceof net.minecraft.entity.EntityLivingBase, true));

    private static HexAction zoneEntitiesAction(
        final java.util.function.Predicate<net.minecraft.entity.Entity> predicate,
        final boolean invert) {
        return new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.zone_entity_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                net.minecraft.entity.player.EntityPlayer player = vm.getPlayer();
                if (player == null) {
                    throw new CastingException("hexcasting.error.zone_entity_context");
                }
                double radius = stack.pop(DoubleIota.class).getValue();
                if (Double.isNaN(radius) || Double.isInfinite(radius)
                    || radius < 0.0D || radius > 128.0D) {
                    throw new CastingException("hexcasting.error.zone_entity_radius");
                }
                net.minecraft.util.math.Vec3d position = stack.pop(Vec3Iota.class).getValue();
                net.minecraft.util.math.AxisAlignedBB area = new net.minecraft.util.math.AxisAlignedBB(
                    position.x - radius, position.y - radius, position.z - radius,
                    position.x + radius, position.y + radius, position.z + radius);
                java.util.List<net.minecraft.entity.Entity> candidates =
                    player.world.getEntitiesWithinAABB(net.minecraft.entity.Entity.class, area);
                java.util.ArrayList<Iota> matches = new java.util.ArrayList<>();
                for (net.minecraft.entity.Entity candidate : candidates) {
                    if (candidate == null || candidate.isDead) {
                        continue;
                    }
                    if (predicate.test(candidate) != invert) {
                        matches.add(new EntityIota(candidate));
                    }
                }
                stack.push(new ListIota(matches));
            }
        };
    }

    /** Push the entity that initiated the current cast. */
    public static final ResourceLocation GET_CASTER_ID =
        new ResourceLocation(HexAPI.MOD_ID, "get_caster");
    public static final HexPattern GET_CASTER_PATTERN =
        pattern(HexDir.NORTH_EAST, "qaq");
    public static final HexAction GET_CASTER = register(GET_CASTER_ID, GET_CASTER_PATTERN,
        new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
throw new CastingException("hexcasting.error.get_caster_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                net.minecraft.entity.player.EntityPlayer player = vm.getPlayer();
                if (player == null) {
throw new CastingException("hexcasting.error.get_caster_context");
                }
                stack.push(new EntityIota(player));
            }
        });

    /** Return the height of an entity Iota in blocks. */
    public static final ResourceLocation ENTITY_HEIGHT_ID =
        new ResourceLocation(HexAPI.MOD_ID, "entity_height");
    public static final HexPattern ENTITY_HEIGHT_PATTERN =
        pattern(HexDir.NORTH_EAST, "awq");
    public static final HexAction ENTITY_HEIGHT = register(
        ENTITY_HEIGHT_ID, ENTITY_HEIGHT_PATTERN,
        new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
throw new CastingException("hexcasting.error.entity_height_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
            EntityIota entityIota = stack.pop(EntityIota.class);
            net.minecraft.entity.Entity entity = resolveEntity(entityIota, vm);
            stack.push(new DoubleIota(entity.height));
            }
        });

    /** Return an entity's eye position as a vector Iota. */
    public static final ResourceLocation ENTITY_POS_EYE_ID =
        new ResourceLocation(HexAPI.MOD_ID, "entity_pos/eye");
    public static final HexPattern ENTITY_POS_EYE_PATTERN =
        pattern(HexDir.EAST, "aa");
    public static final HexAction ENTITY_POS_EYE = register(
        ENTITY_POS_EYE_ID, ENTITY_POS_EYE_PATTERN,
        new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
throw new CastingException("hexcasting.error.entity_pos_eye_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
            EntityIota entityIota = stack.pop(EntityIota.class);
            net.minecraft.entity.Entity entity = resolveEntity(entityIota, vm);
            stack.push(new Vec3Iota(entity.getPositionEyes(1.0F)));
            }
        });

    /** Return an entity's feet position as a vector Iota. */
    public static final ResourceLocation ENTITY_POS_FOOT_ID =
        new ResourceLocation(HexAPI.MOD_ID, "entity_pos/foot");
    public static final HexPattern ENTITY_POS_FOOT_PATTERN =
        pattern(HexDir.NORTH_EAST, "dd");
    public static final HexAction ENTITY_POS_FOOT = register(
        ENTITY_POS_FOOT_ID, ENTITY_POS_FOOT_PATTERN,
        new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
throw new CastingException("hexcasting.error.entity_pos_foot_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
            EntityIota entityIota = stack.pop(EntityIota.class);
            net.minecraft.entity.Entity entity = resolveEntity(entityIota, vm);
            stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(
                entity.posX, entity.posY, entity.posZ)));
            }
        });

    /** Return an entity's look direction as a vector Iota. */
    public static final ResourceLocation GET_ENTITY_LOOK_ID =
        new ResourceLocation(HexAPI.MOD_ID, "get_entity_look");
    public static final HexPattern GET_ENTITY_LOOK_PATTERN =
        pattern(HexDir.EAST, "wa");
    public static final HexAction GET_ENTITY_LOOK = register(
        GET_ENTITY_LOOK_ID, GET_ENTITY_LOOK_PATTERN,
        new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
throw new CastingException("hexcasting.error.entity_look_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
            EntityIota entityIota = stack.pop(EntityIota.class);
            net.minecraft.entity.Entity entity = resolveEntity(entityIota, vm);
            stack.push(new Vec3Iota(entity.getLookVec()));
            }
        });

    /** Return an entity's velocity as a vector Iota. */
    public static final ResourceLocation GET_ENTITY_VELOCITY_ID =
        new ResourceLocation(HexAPI.MOD_ID, "get_entity_velocity");
    public static final HexPattern GET_ENTITY_VELOCITY_PATTERN =
        pattern(HexDir.EAST, "wq");
    public static final HexAction GET_ENTITY_VELOCITY = register(
        GET_ENTITY_VELOCITY_ID, GET_ENTITY_VELOCITY_PATTERN,
        new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
throw new CastingException("hexcasting.error.entity_velocity_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
            EntityIota entityIota = stack.pop(EntityIota.class);
            net.minecraft.entity.Entity entity = resolveEntity(entityIota, vm);
            stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(
                entity.motionX, entity.motionY, entity.motionZ)));
            }
        });

    /** Compare two entity Iotas by entity type, matching the upstream action. */
    public static final ResourceLocation COMPARE_ENTITY_ID =
        new ResourceLocation(HexAPI.MOD_ID, "compare_entity");
    public static final HexPattern COMPARE_ENTITY_PATTERN =
        pattern(HexDir.NORTH_WEST, "aqaeqded");
    public static final HexAction COMPARE_ENTITY = register(
        COMPARE_ENTITY_ID, COMPARE_ENTITY_PATTERN, stack -> {
            EntityIota right = stack.pop(EntityIota.class);
            EntityIota left = stack.pop(EntityIota.class);
            net.minecraft.entity.Entity rightEntity = right.getEntity();
            net.minecraft.entity.Entity leftEntity = left.getEntity();
            if (rightEntity == null || leftEntity == null) {
                throw new CastingException("hexcasting.error.entity_unavailable");
            }
            stack.push(new BooleanIota(leftEntity.getClass() == rightEntity.getClass()));
        });

    /** Return available player media in dust units without consuming it. */
    public static final ResourceLocation GET_MEDIA_ID =
        new ResourceLocation(HexAPI.MOD_ID, "get_media");
    public static final HexPattern GET_MEDIA_PATTERN =
        pattern(HexDir.WEST, "dde");
    public static final HexAction GET_MEDIA = register(GET_MEDIA_ID, GET_MEDIA_PATTERN,
        new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
throw new CastingException("hexcasting.error.get_media_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                IHexCastingData data = vm.getCastingData();
                if (data == null) {
throw new CastingException("hexcasting.error.get_media_context");
                }
                long available = MediaInventoryHelper.getAvailableMedia(vm.getPlayer(), data);
                stack.push(new DoubleIota(
                    ((double) available) / (double) MediaConstants.DUST_UNIT
                ));
            }
        });

    /** Place a source water block at a position. */
    public static final ResourceLocation CREATE_WATER_ID =
        new ResourceLocation(HexAPI.MOD_ID, "create_water");
    public static final HexPattern CREATE_WATER_PATTERN =
        pattern(HexDir.SOUTH_EAST, "aqawqadaq");
    public static final HexAction CREATE_WATER = register(CREATE_WATER_ID, CREATE_WATER_PATTERN,
        fluidAction(net.minecraft.init.Blocks.WATER.getDefaultState()));

    /** Place a source lava block at a position. */
    public static final ResourceLocation CREATE_LAVA_ID =
        new ResourceLocation(HexAPI.MOD_ID, "create_lava");
    public static final HexPattern CREATE_LAVA_PATTERN =
        pattern(HexDir.EAST, "eaqawqadaqd");
    public static final HexAction CREATE_LAVA = register(CREATE_LAVA_ID, CREATE_LAVA_PATTERN,
        fluidAction(net.minecraft.init.Blocks.LAVA.getDefaultState()));

    /** Remove a water source block at a position. */
    public static final ResourceLocation DESTROY_WATER_ID =
        new ResourceLocation(HexAPI.MOD_ID, "destroy_water");
    public static final HexPattern DESTROY_WATER_PATTERN =
        pattern(HexDir.SOUTH_WEST, "dedwedade");
    public static final HexAction DESTROY_WATER = register(DESTROY_WATER_ID, DESTROY_WATER_PATTERN,
        new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.fluid_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                net.minecraft.entity.player.EntityPlayer player = vm.getPlayer();
                if (player == null) {
                    throw new CastingException("hexcasting.error.fluid_context");
                }
                net.minecraft.util.math.BlockPos position = blockPosition(stack.pop(Vec3Iota.class));
                if (!player.world.isRemote
                    && player.world.getBlockState(position).getBlock() == net.minecraft.init.Blocks.WATER) {
                    player.world.setBlockToAir(position);
                }
            }
        });

    private static HexAction fluidAction(final net.minecraft.block.state.IBlockState state) {
        return new HexAction() {
            @Override
            public void execute(CastingStack stack) throws CastingException {
                throw new CastingException("hexcasting.error.fluid_context");
            }

            @Override
            public void execute(CastingStack stack, CastingVM vm) throws CastingException {
                net.minecraft.entity.player.EntityPlayer player = vm.getPlayer();
                if (player == null) {
                    throw new CastingException("hexcasting.error.fluid_context");
                }
                net.minecraft.util.math.BlockPos position = blockPosition(stack.pop(Vec3Iota.class));
                if (!player.world.isRemote && player.world.isAirBlock(position)) {
                    player.world.setBlockState(position, state, 3);
                }
            }
        };
    }

    private static net.minecraft.util.math.BlockPos blockPosition(Vec3Iota vector)
        throws CastingException {
        net.minecraft.util.math.Vec3d value = vector.getValue();
        if (Double.isNaN(value.x) || Double.isNaN(value.y) || Double.isNaN(value.z)
            || Double.isInfinite(value.x) || Double.isInfinite(value.y)
            || Double.isInfinite(value.z)) {
            throw new CastingException("hexcasting.error.fluid_position");
        }
        return new net.minecraft.util.math.BlockPos(value.x, value.y, value.z);
    }

    /** Coerce a vector to the nearest signed axial unit vector. */
    public static final ResourceLocation COERCE_AXIAL_ID =
        new ResourceLocation(HexAPI.MOD_ID, "coerce_axial");
    public static final HexPattern COERCE_AXIAL_PATTERN =
        pattern(HexDir.EAST, "qeeeee");
    public static final HexAction COERCE_AXIAL = register(
        COERCE_AXIAL_ID, COERCE_AXIAL_PATTERN, stack -> {
            net.minecraft.util.math.Vec3d value = stack.pop(Vec3Iota.class).getValue();
            double x = Math.abs(value.x);
            double y = Math.abs(value.y);
            double z = Math.abs(value.z);
            if (x == 0.0D && y == 0.0D && z == 0.0D) {
                throw new CastingException("hexcasting.error.coerce_axial_zero");
            }
            if (x >= y && x >= z) {
                stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(
                    Math.copySign(1.0D, value.x), 0.0D, 0.0D)));
            } else if (y >= z) {
                stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(
                    0.0D, Math.copySign(1.0D, value.y), 0.0D)));
            } else {
                stack.push(new Vec3Iota(new net.minecraft.util.math.Vec3d(
                    0.0D, 0.0D, Math.copySign(1.0D, value.z))));
            }
        });

    /** Apply bonemeal to a sapling or growable block at a position. */
    public static final ResourceLocation EDIFY_ID =
        new ResourceLocation(HexAPI.MOD_ID, "edify");
    public static final HexPattern EDIFY_PATTERN =
        pattern(HexDir.NORTH_EAST, "wqaqwd");
    public static final HexAction EDIFY = register(EDIFY_ID, EDIFY_PATTERN, new HexAction() {
        @Override
        public void execute(CastingStack stack) throws CastingException {
            throw new CastingException("hexcasting.error.edify_context");
        }

        @Override
        public void execute(CastingStack stack, CastingVM vm) throws CastingException {
            net.minecraft.entity.player.EntityPlayer player = vm.getPlayer();
            if (player == null) {
                throw new CastingException("hexcasting.error.edify_context");
            }
            net.minecraft.util.math.BlockPos position = blockPosition(stack.pop(Vec3Iota.class));
            if (!player.world.isRemote) {
                net.minecraft.item.ItemStack boneMeal = new net.minecraft.item.ItemStack(
                    net.minecraft.init.Items.DYE, 1, 15);
                net.minecraft.item.ItemDye.applyBonemeal(
                    boneMeal, player.world, position, player, net.minecraft.util.EnumHand.MAIN_HAND);
            }
        }
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
            || REVERSE == null || LAST_N_LIST == null || RAYCAST == null || RAYCAST_AXIS == null
            || GET_CASTER == null || ENTITY_HEIGHT == null || ENTITY_POS_EYE == null
            || ENTITY_POS_FOOT == null || GET_ENTITY_LOOK == null || GET_ENTITY_VELOCITY == null
            || BREAK_BLOCK == null || EXPLODE == null || EXPLODE_FIRE == null || SUMMON_RAIN == null || DISPEL_RAIN == null || BEEP == null || ADD_MOTION == null || IGNITE == null || EXTINGUISH == null || RAYCAST_ENTITY == null || COMPARE_ENTITY == null || GET_MEDIA == null || HALT == null) {
            throw new IllegalStateException("Hex action registry failed to initialize");
        }
    }

    private static net.minecraft.entity.Entity resolveEntity(EntityIota entityIota, CastingVM vm)
        throws CastingException {
        net.minecraft.entity.Entity entity = entityIota.getEntity();
        if (entity == null && vm != null && vm.getPlayer() != null) {
            for (net.minecraft.entity.Entity candidate : vm.getPlayer().world.loadedEntityList) {
                if (entityIota.getUuid().equals(candidate.getUniqueID())) {
                    entity = candidate;
                    break;
                }
            }
        }
        if (entity == null) {
            throw new CastingException("hexcasting.error.entity_unavailable");
        }
        return entity;
    }

    private static HexAction register(ResourceLocation id, HexPattern pattern, HexAction action) {
        return HexActionRegistry.register(id, pattern, action);
    }

    private static int requireRoundedInteger(DoubleIota value) throws CastingException {
        double raw = value.getValue();
        if (Double.isNaN(raw) || Double.isInfinite(raw)) {
            throw new CastingException("hexcasting.error.expected_finite_integer");
        }
        long rounded = Math.round(raw);
        if (rounded <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (rounded >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) rounded;
    }

    private static int requireInteger(DoubleIota value, int maxInclusive) throws CastingException {
        double raw = value.getValue();
        if (Double.isNaN(raw) || Double.isInfinite(raw) || raw != Math.rint(raw)
            || raw < 0.0D || raw > maxInclusive || raw > Integer.MAX_VALUE) {
            throw new CastingException("hexcasting.error.bounded_integer");
        }
        return (int) raw;
    }

    private static int requireSignedInteger(DoubleIota value, int maxAbs) throws CastingException {
        double raw = value.getValue();
        if (Double.isNaN(raw) || Double.isInfinite(raw) || raw != Math.rint(raw)
            || raw < -maxAbs || raw > maxAbs) {
throw new CastingException("hexcasting.error.bounded_integer");
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
