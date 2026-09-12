# 移植资源与动作现状检查

生成日期：2026-09-12。该记录只保存本机当前源码与资源路径，作为下一批实现的事实基线。

## 工作树与最近提交

```text
（干净）
50db687 docs: add 1.12.2 porting audit
673bbe6 feat: add Inline payload serialization
36fb9bd feat: match Hex patterns in Inline text
d5d22bc feat: extend Inline and Paucal compatibility APIs
432f5b8 feat: integrate BaublesEX media slots

```

## 当前注册/动作线索

```text

```

## 当前物品注册线索

```text
src/main/java/at/petra_k/hexcasting/common/lib\HexItems.java:17:    public static final ItemHexFocus FOCUS = (ItemHexFocus) new ItemHexFocus()
src/main/java/at/petra_k/hexcasting/common/lib\HexItems.java:22:    public static final ItemHexStaff STAFF = (ItemHexStaff) new ItemHexStaff()
src/main/java/at/petra_k/hexcasting/common/lib\HexItems.java:27:    public static final ItemScryingLens SCRYING_LENS = (ItemScryingLens) new ItemScryingLens()
src/main/java/at/petra_k/hexcasting/common/lib\HexItems.java:32:    public static final ItemMediaBattery BATTERY = (ItemMediaBattery) new ItemMediaBattery()
src/main/java/at/petra_k/hexcasting/common/lib\HexItems.java:37:    public static final ItemPatternScroll PATTERN_SCROLL = (ItemPatternScroll) new ItemPatternScroll()
src/main/java/at/petra_k/hexcasting/common/lib\HexItems.java:46:    public static void registerItems(RegistryEvent.Register<Item> event) {
src/main/java/at/petra_k/hexcasting/common/lib\HexItems.java:47:        event.getRegistry().register(FOCUS);
src/main/java/at/petra_k/hexcasting/common/lib\HexItems.java:48:        event.getRegistry().register(STAFF);
src/main/java/at/petra_k/hexcasting/common/lib\HexItems.java:49:        event.getRegistry().register(SCRYING_LENS);
src/main/java/at/petra_k/hexcasting/common/lib\HexItems.java:50:        event.getRegistry().register(BATTERY);
src/main/java/at/petra_k/hexcasting/common/lib\HexItems.java:51:        event.getRegistry().register(PATTERN_SCROLL);
src/main/java/at/petra_k/hexcasting/common/lib\HexCreativeTab.java:12:            return new ItemStack(HexItems.FOCUS);
src/main/java/at/petra_k/hexcasting/common/item\ItemPatternScroll.java:116:            ItemStack scroll = new ItemStack(this);
src/main/java/at/petra_k/hexcasting/common/item\ItemPatternScroll.java:154:                // Invalid or stale NBT falls back to a registered action.
src/main/java/at/petra_k/hexcasting/common/item\ItemMediaBattery.java:53:        items.add(new ItemStack(this));
src/main/java/at/petra_k/hexcasting/common/item\ItemMediaBattery.java:60:        ItemStack battery = new ItemStack(this);
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActionRegistry.java:22:    public static HexAction register(ResourceLocation id, HexPattern pattern, HexAction action) {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:39:    public static final HexAction PUSH_ZERO = register(PUSH_ZERO_ID, PUSH_ZERO_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:44:    public static final HexAction PUSH_ONE = register(PUSH_ONE_ID, PUSH_ONE_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:50:    public static final HexAction DUPLICATE = register(DUPLICATE_ID, DUPLICATE_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:56:    public static final HexAction SWAP = register(SWAP_ID, SWAP_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:66:    public static final HexAction ADD = register(ADD_ID, ADD_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:72:    public static final HexAction NOT = register(NOT_ID, NOT_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:80:    public static final HexAction EMPTY_LIST = register(EMPTY_LIST_ID, EMPTY_LIST_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:88:    public static final HexAction SINGLETON = register(SINGLETON_ID, SINGLETON_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:98:    public static final HexAction SPLAT = register(SPLAT_ID, SPLAT_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:108:    public static final HexAction EQUALITY = register(EQUALITY_ID, EQUALITY_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:119:    public static final HexAction NOT_EQUALS = register(NOT_EQUALS_ID, NOT_EQUALS_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:129:    public static final HexAction TYPE_EQUALITY = register(TYPE_EQUALITY_ID, TYPE_EQUALITY_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:140:    public static final HexAction TYPE_NOT_EQUALS = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:151:    public static final HexAction COERCE_TO_BOOL = register(COERCE_TO_BOOL_ID, COERCE_TO_BOOL_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:158:    public static final HexAction BOOL_IF = register(BOOL_IF_ID, BOOL_IF_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:166:    public static final HexAction RANDOM = register(RANDOM_ID, RANDOM_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:174:    public static final HexAction PRINT = register(PRINT_ID, PRINT_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:182:    public static final HexAction GREATER = register(GREATER_ID, GREATER_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:189:    public static final HexAction LESS = register(LESS_ID, LESS_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:196:    public static final HexAction GREATER_EQ = register(GREATER_EQ_ID, GREATER_EQ_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:203:    public static final HexAction LESS_EQ = register(LESS_EQ_ID, LESS_EQ_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:211:    public static final HexAction APPEND = register(APPEND_ID, APPEND_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:224:    public static final HexAction UNAPPEND = register(UNAPPEND_ID, UNAPPEND_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:241:    public static final HexAction INDEX = register(INDEX_ID, INDEX_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:257:    public static final HexAction REVERSE = register(REVERSE_ID, REVERSE_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:269:    public static final HexAction LAST_N_LIST = register(LAST_N_LIST_ID, LAST_N_LIST_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:284:    public static final HexAction SUB = register(SUB_ID, SUB_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:292:    public static final HexAction MUL_DOT = register(MUL_DOT_ID, MUL_DOT_PATTERN, new OperationAction(2, HexArithmetics::multiply));
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:299:    public static final HexAction DIV_CROSS = register(DIV_CROSS_ID, DIV_CROSS_PATTERN, new OperationAction(2, HexArithmetics::divide));
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:306:    public static final HexAction ABS = register(ABS_ID, ABS_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:314:    public static final HexAction POW_PROJ = register(POW_PROJ_ID, POW_PROJ_PATTERN, new OperationAction(2, HexArithmetics::power));
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:321:    public static final HexAction FLOOR = register(FLOOR_ID, FLOOR_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:329:    public static final HexAction CEIL = register(CEIL_ID, CEIL_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:337:    public static final HexAction MODULO = register(MODULO_ID, MODULO_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:344:    public static final HexAction CONST_NULL = register(CONST_NULL_ID, CONST_NULL_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:353:    public static final HexAction CONST_TRUE = register(CONST_TRUE_ID, CONST_TRUE_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:362:    public static final HexAction CONST_FALSE = register(CONST_FALSE_ID, CONST_FALSE_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:371:    public static final HexAction CONST_DOUBLE_PI = register(CONST_DOUBLE_PI_ID, CONST_DOUBLE_PI_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:380:    public static final HexAction CONST_DOUBLE_TAU = register(CONST_DOUBLE_TAU_ID, CONST_DOUBLE_TAU_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:389:    public static final HexAction CONST_DOUBLE_E = register(CONST_DOUBLE_E_ID, CONST_DOUBLE_E_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:398:    public static final HexAction CONST_DOUBLE_PHI = register(CONST_DOUBLE_PHI_ID, CONST_DOUBLE_PHI_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:407:    public static final HexAction AND = register(AND_ID, AND_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:415:    public static final HexAction OR = register(OR_ID, OR_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:422:    public static final HexAction READ_LOCAL = register(READ_LOCAL_ID, READ_LOCAL_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:430:    public static final HexAction WRITE_LOCAL = register(WRITE_LOCAL_ID, WRITE_LOCAL_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:438:    public static final HexAction CONST_VEC_PX = register(CONST_VEC_PX_ID, CONST_VEC_PX_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:446:    public static final HexAction CONST_VEC_PY = register(CONST_VEC_PY_ID, CONST_VEC_PY_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:454:    public static final HexAction CONST_VEC_PZ = register(CONST_VEC_PZ_ID, CONST_VEC_PZ_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:462:    public static final HexAction CONST_VEC_NX = register(CONST_VEC_NX_ID, CONST_VEC_NX_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:470:    public static final HexAction CONST_VEC_NY = register(CONST_VEC_NY_ID, CONST_VEC_NY_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:478:    public static final HexAction CONST_VEC_NZ = register(CONST_VEC_NZ_ID, CONST_VEC_NZ_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:486:    public static final HexAction CONST_VEC_ZERO = register(CONST_VEC_ZERO_ID, CONST_VEC_ZERO_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:493:    public static final HexAction CONSTRUCT_VEC = register(CONSTRUCT_VEC_ID, CONSTRUCT_VEC_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:505:    public static final HexAction DECONSTRUCT_VEC = register(DECONSTRUCT_VEC_ID, DECONSTRUCT_VEC_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:516:    public static final HexAction INDEX_OF = register(INDEX_OF_ID, INDEX_OF_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:534:    public static final HexAction REMOVE_FROM = register(REMOVE_FROM_ID, REMOVE_FROM_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:552:    public static final HexAction SLICE = register(SLICE_ID, SLICE_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:572:    public static final HexAction REPLACE = register(REPLACE_ID, REPLACE_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:588:    public static final HexAction ROTATE = register(ROTATE_ID, ROTATE_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:602:    public static final HexAction ROTATE_REVERSE = register(ROTATE_REVERSE_ID, ROTATE_REVERSE_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:616:    public static final HexAction OVER = register(OVER_ID, OVER_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:629:    public static final HexAction STACK_LEN = register(STACK_LEN_ID, STACK_LEN_PATTERN, stack ->
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:636:    public static final HexAction TUCK = register(TUCK_ID, TUCK_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:649:    public static final HexAction TWO_DUP = register(TWO_DUP_ID, TWO_DUP_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:663:    public static final HexAction DUPLICATE_N = register(DUPLICATE_N_ID, DUPLICATE_N_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:676:    public static final HexAction FISHERMAN = register(FISHERMAN_ID, FISHERMAN_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:695:    public static final HexAction FISHERMAN_COPY = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:723:    public static final HexAction SWIZZLE = register(SWIZZLE_ID, SWIZZLE_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:753:    public static final HexAction UNIQUE = register(UNIQUE_ID, UNIQUE_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:769:    public static final HexAction FOR_EACH = register(FOR_EACH_ID, FOR_EACH_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:777:    public static final HexAction ESCAPE = register(ESCAPE_ID, ESCAPE_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:786:    public static final HexAction RUNTIME_ESCAPE = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:796:    public static final HexAction OPEN_PAREN = register(OPEN_PAREN_ID, OPEN_PAREN_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:805:    public static final HexAction CLOSE_PAREN = register(CLOSE_PAREN_ID, CLOSE_PAREN_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:814:    public static final HexAction OPEN_N_PARENS = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:824:    public static final HexAction CLOSE_ALL_PARENS = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:834:    public static final HexAction READ_INTO_PARENS = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:844:    public static final HexAction UNDO = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:854:    public static final HexAction HALT = register(HALT_ID, HALT_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:862:    public static final HexAction EVAL = register(EVAL_ID, EVAL_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:869:    public static final HexAction EVAL_CC = register(EVAL_CC_ID, EVAL_CC_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:877:    public static final HexAction SIN = register(SIN_ID,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:884:    public static final HexAction COS = register(COS_ID,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:891:    public static final HexAction TAN = register(TAN_ID,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:898:    public static final HexAction ARCSIN = register(ARCSIN_ID,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:905:    public static final HexAction ARCCOS = register(ARCCOS_ID,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:912:    public static final HexAction ARCTAN = register(ARCTAN_ID,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:919:    public static final HexAction ARCTAN2 = register(ARCTAN2_ID,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:926:    public static final HexAction LOG = register(LOG_ID,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:934:    public static final HexAction XOR = register(XOR_ID, XOR_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:942:    public static final HexAction CONSTRUCT = register(CONSTRUCT_ID, CONSTRUCT_PATTERN, stack -> {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:956:    public static final HexAction DECONSTRUCT = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1019:    public static final HexAction IGNITE = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1053:    public static final HexAction EXTINGUISH = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1087:    public static final HexAction ADD_MOTION = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1133:    public static final HexAction BEEP = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1184:    public static final HexAction SUMMON_RAIN = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1213:    public static final HexAction DISPEL_RAIN = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1242:    public static final HexAction EXPLODE = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1250:    public static final HexAction EXPLODE_FIRE = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1287:    public static final HexAction BREAK_BLOCK = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1318:    public static final HexAction RAYCAST = register(RAYCAST_ID, RAYCAST_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1355:    public static final HexAction RAYCAST_AXIS = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1394:    public static final HexAction RAYCAST_ENTITY = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1457:    public static final HexAction GET_ENTITY = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1465:    public static final HexAction GET_ENTITY_ANIMAL = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1474:    public static final HexAction GET_ENTITY_MONSTER = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1479:    public static final ResourceLocation GET_ENTITY_ITEM_ID =
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1481:    public static final HexPattern GET_ENTITY_ITEM_PATTERN =
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1483:    public static final HexAction GET_ENTITY_ITEM = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1484:        GET_ENTITY_ITEM_ID, GET_ENTITY_ITEM_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1492:    public static final HexAction GET_ENTITY_PLAYER = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1501:    public static final HexAction GET_ENTITY_LIVING = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1551:    public static final HexAction GET_CASTER = register(GET_CASTER_ID, GET_CASTER_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1573:    public static final HexAction ENTITY_HEIGHT = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1594:    public static final HexAction ENTITY_POS_EYE = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1615:    public static final HexAction ENTITY_POS_FOOT = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1637:    public static final HexAction GET_ENTITY_LOOK = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1658:    public static final HexAction GET_ENTITY_VELOCITY = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1680:    public static final HexAction COMPARE_ENTITY = register(
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1697:    public static final HexAction GET_MEDIA = register(GET_MEDIA_ID, GET_MEDIA_PATTERN,
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1762:    private static HexAction register(ResourceLocation id, HexPattern pattern, HexAction action) {
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexActions.java:1763:        return HexActionRegistry.register(id, pattern, action);
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexIotaTypes.java:27:        register(NullIota.TYPE);
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexIotaTypes.java:28:        register(DoubleIota.TYPE);
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexIotaTypes.java:29:        register(BooleanIota.TYPE);
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexIotaTypes.java:30:        register(GarbageIota.TYPE);
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexIotaTypes.java:31:        register(PatternIota.TYPE);
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexIotaTypes.java:32:        register(ListIota.TYPE);
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexIotaTypes.java:33:        register(Vec3Iota.TYPE);
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexIotaTypes.java:34:        register(EntityIota.TYPE);
src/main/java/at/petra_k/hexcasting/common/lib\hex\HexIotaTypes.java:40:    private static void register(IotaType<? extends Iota> type) {
```

## 当前资源文件

```text
src/main/resources\pack.mcmeta
src/main/resources\mixins.hexcasting.json
src/main/resources\META-INF\licenses\PAUCAL-MIT-LICENSE.txt
src/main/resources\META-INF\licenses\PAUCAL-INLINE-NOTICE.txt
src/main/resources\META-INF\licenses\INLINE-LGPL-3.0-LICENSE.txt
src/main/resources\mcmod.info
src/main/resources\hexcasting.mixins.refmap.json
src/main/resources\assets\hexcasting\textures\gui\patchi_filler.png
src/main/resources\assets\hexcasting\textures\gui\patchi_book.png
src/main/resources\assets\hexcasting\textures\items\staff.png
src/main/resources\assets\hexcasting\textures\items\scrying_lens.png
src/main/resources\assets\hexcasting\textures\items\pattern_scroll.png
src/main/resources\assets\hexcasting\textures\items\patchouli_book.png
src/main/resources\assets\hexcasting\textures\items\focus.png
src/main/resources\assets\hexcasting\textures\items\battery.png
src/main/resources\assets\hexcasting\recipes\staff.json
src/main/resources\assets\hexcasting\recipes\scrying_lens.json
src/main/resources\assets\hexcasting\recipes\pattern_scroll.json
src/main/resources\assets\hexcasting\recipes\guide_book.json
src/main/resources\assets\hexcasting\recipes\focus.json
src/main/resources\assets\hexcasting\lang\zh_cn.lang
src/main/resources\assets\hexcasting\lang\en_us.lang
src/main/resources\assets\hexcasting\models\item\staff.json
src/main/resources\assets\hexcasting\models\item\scrying_lens.json
src/main/resources\assets\hexcasting\models\item\pattern_scroll.json
src/main/resources\assets\hexcasting\models\item\patchouli_book.json
src/main/resources\assets\hexcasting\models\item\focus.json
src/main/resources\assets\hexcasting\models\item\battery.json
src/main/resources\assets\hexcasting\patchouli_books\hexcasting\book.json
src/main/resources\assets\hexcasting\patchouli_books\hexcasting\zh_cn\entries\getting_started\first_pattern.json
src/main/resources\assets\hexcasting\patchouli_books\hexcasting\en_us\entries\getting_started\first_pattern.json
src/main/resources\assets\hexcasting\patchouli_books\hexcasting\zh_cn\categories\getting_started.json
src/main/resources\assets\hexcasting\patchouli_books\hexcasting\en_us\categories\getting_started.json
```

## 模型与纹理文件

```text
src/main/resources\assets\hexcasting\textures\items\staff.png
src/main/resources\assets\hexcasting\textures\items\scrying_lens.png
src/main/resources\assets\hexcasting\textures\items\pattern_scroll.png
src/main/resources\assets\hexcasting\textures\items\patchouli_book.png
src/main/resources\assets\hexcasting\textures\items\focus.png
src/main/resources\assets\hexcasting\textures\items\battery.png
src/main/resources\assets\hexcasting\textures\gui\patchi_filler.png
src/main/resources\assets\hexcasting\textures\gui\patchi_book.png
src/main/resources\assets\hexcasting\models\item\staff.json
src/main/resources\assets\hexcasting\models\item\scrying_lens.json
src/main/resources\assets\hexcasting\models\item\pattern_scroll.json
src/main/resources\assets\hexcasting\models\item\patchouli_book.json
src/main/resources\assets\hexcasting\models\item\focus.json
src/main/resources\assets\hexcasting\models\item\battery.json
```

## 本地化与 Patchouli 文件

```text
src/main/resources\assets\hexcasting\lang\zh_cn.lang
src/main/resources\assets\hexcasting\lang\en_us.lang
src/main/resources\assets\hexcasting\textures\items\patchouli_book.png
src/main/resources\assets\hexcasting\textures\gui\patchi_book.png
src/main/resources\assets\hexcasting\models\item\patchouli_book.json
src/main/resources\assets\hexcasting\recipes\guide_book.json
src/main/resources\assets\hexcasting\patchouli_books\hexcasting\book.json
src/main/resources\assets\hexcasting\patchouli_books\hexcasting\zh_cn\entries\getting_started\first_pattern.json
src/main/resources\assets\hexcasting\patchouli_books\hexcasting\zh_cn\categories\getting_started.json
src/main/resources\assets\hexcasting\patchouli_books\hexcasting\en_us\entries\getting_started\first_pattern.json
src/main/resources\assets\hexcasting\patchouli_books\hexcasting\en_us\categories\getting_started.json
```

## 上游候选路径

```text
D:/GitHub/HexMod-main\doc\src\hexdoc_hexcasting\utils\pattern.py
D:/GitHub/HexMod-main\Forge\src\main\java\at\petrak\hexcasting\forge\mixin\ForgeAccessorBuiltInRegistries.java
D:/GitHub/HexMod-main\Forge\src\main\java\at\petrak\hexcasting\forge\lib\ForgeHexArgumentTypeRegistry.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\xplat\IClientXplatAbstractions.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\xplat\DummyXplatAbstractions.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\server\ScrungledPatternsSave.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\recipe\HexRecipeStuffRegistry.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\patchouli\LookupPatternComponent.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\msgs\MsgNewSpiralPatternsS2C.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\msgs\MsgNewSpellPatternS2C.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\msgs\MsgNewSpellPatternC2S.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\msgs\MsgClearSpiralPatternsS2C.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\jei\PatternDrawable.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\misc\PatternTooltip.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\inline\InlinePatternRenderer.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\inline\InlinePatternData.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\inline\HexPatternMatcher.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\loot\AddPerWorldPatternToScrollFunc.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\emi\PatternRendererEMI.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\datagen\tag\HexActionTagProvider.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\lib\hex\HexEvalSounds.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\lib\hex\HexActions.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\command\PatternResLocArgument.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\command\ListPerWorldPatternsCommand.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\PatternRegistryManifest.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\stack\SpecialHandlerMask.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\client\render\PatternTextureManager.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\client\render\PatternRenderer.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\client\gui\PatternTooltipComponent.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\api\client\ScryingLensOverlayRegistry.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\api\casting\eval\CastingEnvironment.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\api\casting\eval\vm\ContinuationFrame.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\api\casting\eval\env\PlayerBasedCastEnv.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\api\casting\eval\env\CircleCastEnv.java
D:/GitHub/HexMod-main\Forge\src\main\resources\META-INF\services\at.petrak.hexcasting.xplat.IXplatAbstractions
D:/GitHub/HexMod-main\Forge\src\main\resources\META-INF\services\at.petrak.hexcasting.xplat.IClientXplatAbstractions
D:/GitHub/HexMod-main\doc\src\hexdoc_hexcasting\utils\pattern.py
D:/GitHub/HexMod-main\doc\src\hexdoc_hexcasting\_templates\pages\hexcasting\pattern.html.jinja
D:/GitHub/HexMod-main\doc\src\hexdoc_hexcasting\_templates\pages\hexcasting\manual_pattern.html.jinja
D:/GitHub/HexMod-main\Forge\src\main\resources\data\hexcasting\tags\hexcasting\action\requires_enlightenment_in_parens.json
D:/GitHub/HexMod-main\Forge\src\main\resources\data\hexcasting\tags\hexcasting\action\cannot_modify_cost.json
D:/GitHub/HexMod-main\Common\src\test\resources\META-INF\services\at.petrak.hexcasting.xplat.IXplatAbstractions
D:/GitHub/HexMod-main\Fabric\src\main\resources\META-INF\services\at.petrak.hexcasting.xplat.IXplatAbstractions
D:/GitHub/HexMod-main\Fabric\src\main\resources\META-INF\services\at.petrak.hexcasting.xplat.IClientXplatAbstractions
D:/GitHub/HexMod-main\Forge\src\main\java\at\petrak\hexcasting\forge\mixin\ForgeAccessorBuiltInRegistries.java
D:/GitHub/HexMod-main\Forge\src\main\java\at\petrak\hexcasting\forge\lib\ForgeHexArgumentTypeRegistry.java
D:/GitHub/HexMod-main\Forge\src\generated\resources\data\hexcasting\tags\hexcasting\action\requires_enlightenment.json
D:/GitHub/HexMod-main\Forge\src\generated\resources\data\hexcasting\tags\hexcasting\action\per_world_pattern.json
D:/GitHub/HexMod-main\Forge\src\generated\resources\data\hexcasting\tags\hexcasting\action\can_start_enlighten.json
D:/GitHub/HexMod-main\Fabric\src\main\resources\data\hexcasting\tags\action\requires_enlightenment_in_parens.json
D:/GitHub/HexMod-main\Fabric\src\main\resources\data\hexcasting\tags\action\cannot_modify_cost.json
D:/GitHub/HexMod-main\Fabric\src\generated\resources\data\hexcasting\tags\action\requires_enlightenment.json
D:/GitHub/HexMod-main\Fabric\src\generated\resources\data\hexcasting\tags\action\per_world_pattern.json
D:/GitHub/HexMod-main\Fabric\src\generated\resources\data\hexcasting\tags\action\can_start_enlighten.json
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\xplat\IXplatAbstractions.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\xplat\IClientXplatAbstractions.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\xplat\DummyXplatAbstractions.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\server\ScrungledPatternsSave.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\datagen\tag\HexActionTagProvider.java
D:/GitHub/HexMod-main\Fabric\src\main\java\at\petrak\hexcasting\fabric\cc\CCPatterns.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\utils\PatternEntry.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\utils\PatternDrawingUtil.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\recipe\HexRecipeStuffRegistry.java
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\categories\patterns.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\templates\pattern.json
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\patchouli\PatternProcessor.java
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\templates\manual_pattern_nosig.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\templates\manual_pattern.json
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\patchouli\ManualPatternComponent.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\patchouli\LookupPatternComponent.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\patchouli\AbstractPatternComponent.java
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\categories\patterns\spells.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\categories\patterns\great_spells.json
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\jei\PatternDrawable.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\command\RecalcPatternsCommand.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\command\PatternTexturesCommand.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\command\PatternResLocArgument.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\command\ListPerWorldPatternsCommand.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\msgs\MsgNewSpiralPatternsS2C.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\msgs\MsgNewSpellPatternS2C.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\msgs\MsgNewSpellPatternC2S.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\msgs\MsgClearSpiralPatternsS2C.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\client\gui\PatternTooltipComponent.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\inline\InlinePatternRenderer.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\inline\InlinePatternData.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\inline\HexPatternOverlayRenderer.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\inline\HexPatternMatcher.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\client\render\WorldlyPatternRenderHelpers.java
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\types.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\stackmanip.json
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\interop\emi\PatternRendererEMI.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\client\render\HexPatternLike.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\client\render\PATTERN_RENDER_LORE.md
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\client\render\PatternTextureManager.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\client\render\PatternSettings.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\client\render\PatternRenderer.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\client\render\PatternColors.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\client\render\HexPatternPoints.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\misc\PatternTooltip.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\PatternRegistryManifest.java
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\spells\sentinels.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\spells\nadirs.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\spells\itempicking.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\spells\hexcasting.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\spells\flight.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\spells\cyclevariant.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\spells\colorize.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\spells\blockworks.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\spells\basic.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\sets.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\readwrite.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\readers_guide.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\patterns_as_iotas.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\numbers.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\meta.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\math.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\logic.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\lists.json
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\client\PatternShapeMatcher.java
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\great_spells\zeniths.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\great_spells\weather_manip.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\great_spells\teleport.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\great_spells\make_battery.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\great_spells\greater_sentinel.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\great_spells\create_lava.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\great_spells\brainsweep.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\great_spells\altiora.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\entities.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\consts.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\circle.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\basics.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\akashic_patterns.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\advanced_math.json
D:/GitHub/HexMod-main\Common\src\main\resources\assets\hexcasting\patchouli_books\thehexbook\en_us\entries\patterns\advanced_escaping.json
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\loot\AddPerWorldPatternToScrollFunc.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\lib\hex\HexEvalSounds.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\lib\hex\HexActions.java
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\types\OpTypeEquality.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\types\OpItemEquality.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\types\OpEntityEquality.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\types\OpBlockEquality.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\local\OpPushLocal.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\local\OpPeekLocal.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\environment\OpGetMedia.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\stack\SpecialHandlerMask.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\stack\OpTwiddling.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\stack\OpStackSize.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\stack\OpFishermanButItCopies.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\stack\OpFisherman.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\stack\OpDuplicateN.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\stack\OpAlwinfyHasAscendedToABeingOfPureMath.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\lists\OpSplat.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\lists\OpSingleton.kt
D:/GitHub/HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\casting\actions\lists\OpLastNToList.kt
```

## 结论

- 客户端日志没有显示 HexCasting 自身的缺失模型或纹理异常；当前日志中可见的错误主要来自 Realms、JEI 旧书签和 CraftTweaker 外部脚本。
- 上游动作源码需要从实际目录重新对照，不能继续假定旧的包路径。
- 如果资源列表只包含 HexCasting 自己的 assets，Inline/Paucal 兼容 API 没有必要凭空创建空的 assets 目录；它们当前是源码兼容层，不代表完整资源移植。
- 下一轮实现应优先把物品注册名、模型文件和 lang 键做机器可比对，再对动作执行语义做逐项移植。

