# Hex Casting 1.12.2 移植 TODO

本清单以 `D:\GitHub\HexMod-main` 为对照，按对游戏语义和稳定性的影响排序。
已完成项目集中在“已完成”章节；“待完成”章节中的未勾选项目仍需移植或核验。

## 已完成

### 已完成的移植阶段

- [x] 初始化注册
- [x] 能力与同步
- [x] 构筑粒子 / 染色
- [x] Impetus / Directrix
- [x] Brainsweep
- [x] Quenched Allay
- [x] 专用方块行为
- [x] JEI
- [x] 物品能力
- [x] 安全基线与基础客户端回归

### P0：施法核心等效性

- [x] 完善媒质读取与实际消耗（共享事务已覆盖原版核心来源、排序、部分消耗和堆叠 NBT）
  - [x] 玩家背包、盔甲栏、副手和 BaublesEX
  - [x] 紫水晶粉、紫水晶碎片、充能紫水晶、淬灵晶碎片、淬灵晶块
  - [x] 可提供媒质的物品、电池、物品实体和构筑媒质
  - [x] 媒质来源排序、部分消耗和堆叠 NBT 的统一读取/写回基础
- [x] 施法失败后的媒质回滚（VM 事务、物品实体和容器状态已接入）
- [x] 让 `get_media` 的显示结果与实际可消耗媒质一致（复用同一媒质事务解析）
- [x] 完善 Mishap 系统（结构化分类、上下文、统一副作用/音效/本地化入口已接入）
  - [x] 区分媒质不足、错误实体、错误物品、错误方块、参数不足等基础错误类型
  - [x] 保存施法者、图案、动作名称、距离、维度、权限和括号上下文
  - [x] 统一错误副作用、错误音效和本地化文本（由 MishapFeedback 统一处理）
  - [x] 清理剩余入口的通用兜底异常，并核对重复副作用和缺失本地化
- [x] 逐项核对所有核心动作的成本、目标筛选、副作用顺序和失败行为（下方列出原版 188 项；可选 Pehkui 单独保留）
  - [x] 已完成原版注册表、动作实现目录、目标版注册项和目标版动作辅助实现的全量静态遍历
- [x] 已记录注册集合差异、额外数字注册和缺失 Pehkui 联动
  - [x] 按下方动作清单逐项完成代码修正与静态回归核对

#### 动作全量审查结果（2026-09-27）

本次审查直接以 `D:\GitHub\HexMod-main\Common\src\main\java\at\petrak\hexcasting\common\lib\hex\HexActions.java` 及其 `common/casting/actions` 下的实现为基准，遍历了原版注册表的全部 188 项，并逐项对照当前 1.12.2 的 `HexActions.java`、`SentinelActions.java` 和动作辅助实现。本节集中记录已完成的核心动作核对；可选 Pehkui 联动单独列入“待完成”。

记号：`D` = `DUST_UNIT`，`S` = `SHARD_UNIT`，`C` = `CRYSTAL_UNIT`。每个条目都需要同时核对注册图案、参数逆序、成本、目标筛选、范围/权限、验证与消耗顺序、世界副作用、失败 Mishap、VM 回滚和施法音效。

##### 注册集合与图案差异

- [x] 原版 188 项、目标版 188 个实际注册项已完成集合对照：共同 183 项；目标版另有 `push_zero`、`push_one`；原版另有可选的 `interop/pehkui/get`、`interop/pehkui/set`。
- [x] 原版 5 项 Sentinel 动作虽然在目标版拆到 `SentinelActions.java`，但注册 ID 和图案均已纳入对照。
- [x] `const/vec/0`：原版与目标版均为 `NORTH_WEST + qqqqq`，此前记录的 `NORTH_EAST + qdwdq` 是审查记录错误。
- [x] `push_zero`、`push_one`：已移除普通注册项；0/1 继续由 `SpecialPatternResolver` 的数字特殊处理器解析，且已清理旧别名引用。
- [x] 已确认 `interop/pehkui/get`、`interop/pehkui/set` 属于可选联动；目标版核心动作清单不将其误记为缺失，具体移植任务列入“待完成”。

##### 栈、列表和局部变换（全部成本 0，无世界目标）

- [x] `swap`：取栈顶 2 项并交换；少于 2 项时 `NotEnoughArgs`；无副作用。
- [x] `rotate`：取栈顶 3 项按原版 `[a,b,c] -> [b,c,a]` 重排；参数不足失败；无副作用。
- [x] `rotate_reverse`：取栈顶 3 项按原版逆向旋转；参数不足失败；无副作用。
- [x] `duplicate`：复制栈顶项；空栈 `NotEnoughArgs`；不复制可变世界状态。
- [x] `over`：复制栈顶下方项并保持原栈项；少于 2 项失败；无副作用。
- [x] `tuck`：复制栈顶并插回其下方；少于 2 项失败；无副作用。
- [x] `2dup`：复制栈顶两项；少于 2 项失败；无副作用。
- [x] `stack_len`：不消费栈，压入执行前栈长度；无参数、无 Mishap。
- [x] `duplicate_n`：读取待复制项与正整数数量，按原版序列生成副本并遵守最大 Iota 序列限制；参数类型/范围错误必须指向正确逆序参数。
- [x] `fisherman`：读取有符号整数深度，正数取出指定深度项，负数移动诱饵项；深度超出 `-(size-2)..(size-2)` 或栈不足时失败；必须保持原版消费顺序。
- [x] `fisherman/copy`：与 `fisherman` 相同的深度范围，但复制而不是移动目标项；失败时不得先改变栈。
- [x] `swizzle`：读取非负 Lehmer code，按阶乘宽度重排对应栈尾；code 非整数、越界、阶乘溢出或栈不足时失败；失败不能部分重排。
- [x] `append`：读取列表和一个 Iota，生成追加后的新列表；错误类型和参数逆序与原版一致；不修改源列表。
- [x] `unappend`：非空列表返回去尾列表和尾项，空列表返回原列表与 `Null`；错误类型与原版一致。
- [x] `index`：读取列表与整数索引，合法时返回元素，越界时返回 `Null`；不能把越界当作 Mishap。
- [x] `for_each`：读取指令列表和数据列表，按原版顺序建立迭代帧；非列表或参数不足失败；帧建立前不得消费媒质或产生世界副作用。
- [x] `singleton`：消费一项并包装为单元素列表；参数不足失败。
- [x] `empty_list`：压入空列表；无参数、无失败路径。
- [x] `reverse`：消费列表并返回逆序副本；不修改输入列表。
- [x] `last_n_list`：读取非负整数并从栈尾收集 N 项，保持原版顺序；N 超过可用栈项或不是整数时失败，不能部分消费。
- [x] `splat`：消费列表并按列表顺序展开到栈；非列表失败。
- [x] `index_of`：按原版列表搜索规则返回索引/`Null`；核对 Iota 容忍比较和未找到分支。
- [x] `remove_from`：按原版索引/值规则移除列表项并返回对应结果；核对越界分支、重复值和失败时栈恢复。
- [x] `slice`：读取列表与边界参数，按原版半开区间和负值规则返回列表；边界错误不能产生部分结果。
- [x] `replace`：读取列表、索引和新值，按原版生成替换后的列表；核对越界 Mishap 和参数逆序。
- [x] `construct`：按原版把栈项构造成列表，核对数量参数、顺序和序列上限。
- [x] `deconstruct`：按原版拆出列表项，核对空列表、结果顺序和失败行为。
- [x] `read/local`：读取当前括号局部值；没有局部值、括号上下文不匹配和参数不足时必须使用原版失败类型。
- [x] `write/local`：写入当前括号局部值；写入位置、嵌套括号和失败时栈/帧状态必须等效。

##### 数学、逻辑、常量和类型（全部成本 0）

- [x] `add`：按原版 Iota 算术分派消费 2 项；类型不匹配使用对应 InvalidIota；不得改变参数逆序。
- [x] `sub`：按原版数值/向量减法分派；核对零、向量和不兼容类型的失败行为。
- [x] `mul`：按原版乘法/标量向量规则分派；核对非有限数和类型失败。
- [x] `div`：按原版除法规则分派；核对零除、非有限结果和失败时是否保留栈。
- [x] `abs`：消费一个数值并返回绝对值；类型错误失败，不产生世界副作用。
- [x] `pow`：消费底数与指数并按原版幂运算/非有限值规则处理；核对结果截断和失败行为。
- [x] `floor`：返回数值向下取整；非数值/非有限值失败规则与原版一致。
- [x] `ceil`：返回数值向上取整；非数值/非有限值失败规则与原版一致。
- [x] `construct_vec`：消费三个数值并按原版轴顺序构造向量；参数不足/类型错误不能部分消费。
- [x] `deconstruct_vec`：消费向量并按原版顺序压入 X/Y/Z；非向量失败。
- [x] `coerce_axial`：按原版把向量归一到轴向量；零向量、Tie 和非向量分支必须一致。
- [x] `and`：消费两个布尔值并返回逻辑与；非布尔失败。
- [x] `or`：消费两个布尔值并返回逻辑或；非布尔失败。
- [x] `not`：消费一个布尔值并取反；非布尔失败。
- [x] `xor`：消费两个布尔值并返回异或；非布尔失败。
- [x] `greater`：按原版数值比较返回布尔；类型错误失败。
- [x] `less`：按原版数值比较返回布尔；类型错误失败。
- [x] `greater_eq`：按原版数值比较返回布尔；类型错误失败。
- [x] `less_eq`：按原版数值比较返回布尔；类型错误失败。
- [x] `equals`：使用原版 Iota `tolerates` 语义比较；核对列表、数值容差和实体分支。
- [x] `not_equals`：严格取 `equals` 的补集；核对容差语义不能误改成 Java `equals`。
- [x] `type_equals`：只比较 Iota 类型，不读取 payload；参数不足失败。
- [x] `type_not_equals`：只比较 Iota 类型的补集；参数不足失败。
- [x] `bool_coerce`：按原版 truthiness 转换为布尔；核对 `Null`、空列表和数值 0。
- [x] `if`：按原版栈顺序消费条件、真值和假值，只把选中分支留下；参数不足/条件类型错误不能提前消费。
- [x] `random`：压入 `[0,1)` 的随机 double；无媒质、无目标、无 Mishap。
- [x] `sin`：一元正弦；核对非有限输入/输出。
- [x] `cos`：一元余弦；核对非有限输入/输出。
- [x] `tan`：一元正切；核对奇点附近结果和非有限处理。
- [x] `arcsin`：一元反正弦；核对定义域失败/结果。
- [x] `arccos`：一元反余弦；核对定义域失败/结果。
- [x] `arctan`：一元反正切；核对非有限输入。
- [x] `arctan2`：按原版参数顺序计算二元反正切；核对零值和参数逆序。
- [x] `logarithm`：按原版底数/真数顺序计算对数；核对非正值失败。
- [x] `modulo`：按原版浮点/整数取模规则处理；核对零模和负数。
- [x] `unique`：按原版 Iota 容忍语义移除列表重复项；核对顺序保持和嵌套列表。
- [x] `const/null`：无参数压入 Null；注册图案必须与原版一致。
- [x] `const/true`：无参数压入 true；注册图案必须与原版一致。
- [x] `const/false`：无参数压入 false；注册图案必须与原版一致。
- [x] `const/vec/px`：无参数压入 `(1,0,0)`；注册图案和方向必须与原版一致。
- [x] `const/vec/py`：无参数压入 `(0,1,0)`；注册图案和方向必须与原版一致。
- [x] `const/vec/pz`：无参数压入 `(0,0,1)`；注册图案和方向必须与原版一致。
- [x] `const/vec/nx`：无参数压入 `(-1,0,0)`；注册图案和方向必须与原版一致。
- [x] `const/vec/ny`：无参数压入 `(0,-1,0)`；注册图案和方向必须与原版一致。
- [x] `const/vec/nz`：无参数压入 `(0,0,-1)`；注册图案和方向必须与原版一致。
- [x] `const/vec/0`：无参数压入零向量；图案和起始方向已核对为 `NORTH_WEST + qqqqq`。
- [x] `const/double/pi`：无参数压入 Math.PI；图案和浮点值与原版一致。
- [x] `const/double/tau`：无参数压入 Tau；核对常量精度。
- [x] `const/double/e`：无参数压入 Math.E；核对常量精度。
- [x] `const/double/phi`：无参数压入黄金比例；核对常量精度。

##### 查询、选择、射线、类型和圆环

- [x] `get_caster`：无参数返回当前施法实体；无施法实体时原版返回 Null；目标范围和非玩家施法上下文不能被错误限制为玩家。
- [x] `entity_pos/eye`：消费实体，先做实体范围检查，再返回插值眼部位置；实体不存在/超范围失败。
- [x] `entity_pos/foot`：消费实体，先做实体范围检查，再返回脚部位置；失败不产生结果。
- [x] `get_entity_look`：消费实体并返回特殊视线方向；核对箭、玩家、掉落物等实体的方向适配。
- [x] `get_entity_height`：消费实体并返回包围盒高度；核对实体解析和范围失败。
- [x] `get_entity_velocity`：消费实体并返回特殊速度；核对玩家、箭、三叉戟、掉落物和零速度。
- [x] `raycast`：成本 `D/100`；参数为原点和方向，原点先做范围检查，再按 32 格射线碰撞器返回命中方块中心/Null；方向非有限值失败。
- [x] `raycast/axis`：成本 `D/100`；与 `raycast` 相同的起点、方向、范围和碰撞规则，但返回命中面法向量/Null。
- [x] `raycast/entity`：成本 `D/100`；原点和方向校验后按实体包围盒、拾取半径、根载具规则选取首个实体；超出法术范围返回 Null。
- [x] `get_media`：无额外成本；必须读取与实际 `consumeMedia` 相同的来源排序、容器/实体/构筑媒质和堆叠 NBT，显示值按原版 D 单位换算。
- [x] `get_entity`：消费位置，在半径 0.5 的立方体内按距离排序选择首个仍存活、非旁观且在 ambit 内的实体；无匹配返回 Null。
- [x] `get_entity/animal`：同上，只保留 Animal/WaterAnimal。
- [x] `get_entity/monster`：同上，只保留 Enemy。
- [x] `get_entity/item`：同上，只保留 ItemEntity。
- [x] `get_entity/player`：同上，只保留 Player。
- [x] `get_entity/living`：同上，只保留非 ArmorStand 的 LivingEntity 或 EnderDragonPart。
- [x] `zone_entity`：成本 0；消费位置和正半径，使用球形距离而非仅 AABB，按距离排序并返回全部合理可选实体。
- [x] `zone_entity/animal`：`zone_entity` 的动物筛选版本。
- [x] `zone_entity/not_animal`：反动物筛选，反向条件不能改变存活、旁观和 ambit 过滤。
- [x] `zone_entity/monster`：怪物筛选版本。
- [x] `zone_entity/not_monster`：反怪物筛选版本。
- [x] `zone_entity/item`：掉落物筛选版本。
- [x] `zone_entity/not_item`：反掉落物筛选版本。
- [x] `zone_entity/player`：玩家筛选版本。
- [x] `zone_entity/not_player`：反玩家筛选版本。
- [x] `zone_entity/living`：LivingEntity/EnderDragonPart 且排除 ArmorStand 的筛选版本。
- [x] `zone_entity/not_living`：反 living 筛选版本。
- [x] `compare_block/lenient`：消费两个位置并先分别做范围检查，只比较方块类型，不比较方块状态属性。
- [x] `compare_block/strict`：消费两个位置并先分别做范围检查，比较完整 BlockState 身份/属性。
- [x] `compare_entity`：消费两个实体，分别做范围检查，只比较实体类型，不比较 UUID/实例。
- [x] `compare_item/lenient`：消费两个实体物品持有者，分别做范围检查，只比较物品类型，不比较 NBT。
- [x] `compare_item/strict`：消费两个实体物品持有者，分别做范围检查，比较物品类型、元数据和 NBT；无物品持有能力时使用原版 InvalidIota。
- [x] `circle/impetus_pos`：无参数；必须要求当前存在法术圆环，返回 Impetus 位置；无圆环走 `NoSpellCircle`。
- [x] `circle/impetus_dir`：无参数；必须要求法术圆环，返回方向；无圆环失败。
- [x] `circle/bounds/min`：无参数；必须要求法术圆环，返回最小边界；无圆环失败。
- [x] `circle/bounds/max`：无参数；必须要求法术圆环，返回最大边界；无圆环失败。

##### Iota 读写和数据持有者

- [x] `read`：无参数，从施法者指定的可读副手/持有物读取当前 Iota，空数据使用默认 Iota；没有数据持有者、不可读或没有默认值时 `BadOffhandItem`。
- [x] `write`：消费一个 Iota，先模拟写入检查可写性，再检查真名保护，最后在 RenderedSpell 阶段写入；成本为 0，任何失败不能修改物品。
- [x] `readable`：无参数，返回持有物是否存在可读数据或默认空 Iota；找不到持有者返回 false 而非 Mishap。
- [x] `writable`：无参数，返回持有物是否可写；找不到持有者返回 false。
- [x] `read/entity`：消费实体，范围检查后读取实体数据持有者；没有持有者/数据/默认值时 `BadEntity`。
- [x] `write/entity`：消费实体和 Iota，范围检查、可写模拟、真名保护后再写入；成本为 0，失败不能部分写入。
- [x] `readable/entity`：消费实体并范围检查；没有数据持有者或可读数据返回 false。
- [x] `writable/entity`：消费实体并范围检查；没有数据持有者返回 false，否则返回 `writeable()`。

##### 元操作、括号和求值控制

- [x] `escape`：按原版结束当前括号/逃逸帧；核对括号类型、连续帧、操作数消耗和失败副作用。
- [x] `runtime_escape`：运行时逃逸当前执行；核对只影响当前 continuation、不错误清空外层括号。
- [x] `open_paren`：打开普通括号帧；成本 0，必须保持原版帧上下文。
- [x] `close_paren`：关闭一个普通括号；没有匹配括号使用 `NeedsParens`，关闭前后栈行为一致。
- [x] `open_n_parens`：消费非负整数并打开 N 个括号；N 的整数/上限校验、帧顺序和失败原子性与原版一致。
- [x] `close_all_parens`：关闭所有匹配括号；无括号时 `NeedsParens`，不能跨越不允许关闭的帧。
- [x] `read_into_parens`：要求括号上下文并从副手读取 Iota 写入当前括号；副手选择、默认值和失败类型与 `read` 一致。
- [x] `undo`：撤销当前括号中的上一步操作；核对括号检查、媒质回滚和嵌套上下文。
- [x] `eval`：消费可执行 Iota/列表并按原版求值；参数类型、括号帧和操作计数必须一致。
- [x] `eval/cc`：可中断求值版本；核对 breakable continuation、异常传播和回滚顺序。
- [x] `halt`：按原版停止当前求值分支；核对是否保留外层 continuation 和失败/音效行为。
- [x] `thanatos`：返回当前施法剩余操作数；上下文、操作计数和音效已与原版核对。

##### 普通法术：范围、权限、媒质与世界副作用

- [x] `print`：成本 0，消费一个 Iota 并打印/保留原版结果；核对服务端消息、客户端反馈和失败时是否消费栈。
- [x] `explode`：消费位置与强度（强度 0..10），位置范围和眼部偏移规则与原版一致；成本 `D*(3*strength+0.125)` 向零截断；编辑权限只决定施法阶段是否实际爆炸，验证/消耗顺序不能提前改变。
- [x] `explode/fire`：与 `explode` 相同，但成本附加 `D` 且开启火焰；核对爆炸交互模式、权限和回滚。
- [x] `add_motion`：消费实体与向量，实体先范围检查；成本为向量长度平方乘 `D`，同一 cast 对同一实体首次加 1D；向量限制 8192 后再施加，非有限值和失败时不能改变实体。
- [x] `blink`：消费实体与 double 位移，检查不可传送实体、黏性载具及乘客、维度、世界边界和范围；成本 `S*abs(delta)*0.5` 四舍五入；施法阶段按目标视线传送并按原版处理乘客。
- [x] `break_block`：消费位置并检查编辑范围/权限；便宜方块成本 `D/100`，否则 `D/8`；施法时检查非空气、硬度非负、配置采掘等级和 Forge 破坏事件，再掉落方块；失败只是不破坏，不应额外 Mishap。
- [x] `place_block`：消费位置，先查可放置物品再检查目标可替换性；成本 `D/8`；施法阶段模拟副手放置，只消费一个匹配物品，非 FAIL 才写回背包并播放声音/粒子；任何失败不能吞物品。
- [x] `colorize`：成本 `D`，只接受可用染色剂副手物品；核对目标物品选择、染色剂消耗/写回、真名/权限和施法特效。
- [x] `cycle_variant`：成本 `D/10`，目标为可变体持有物；施法阶段循环一次并在 cast 失败时恢复原变体。
- [x] `create_water`：成本 `D`，消费位置并检查编辑权限/范围；按原版水桶与满水炼药锅逻辑放置，放置失败不得产生不一致方块。
- [x] `create_lava`：成本 `C`，与 `create_water` 相同的验证和副作用顺序，但使用熔岩。
- [x] `destroy_water`：成本 `2C`，消费位置后按原版先尝试 Forge 流体处理器，再处理炼药锅、流体方块和水生植物，最多 1024 个方块；编辑权限、掉落、烟雾、音效和失败时不误删必须一致。
- [x] `ignite`：成本 `D`；实体目标先范围检查并着火 8 秒，向量目标先位置/编辑权限检查，再按火焰弹后打火石顺序模拟使用；失败不得消耗错误目标或留下伪物品。
- [x] `extinguish`：成本 `6D`；从目标位置开始按半径 10 的 BFS，最多 1024 个方块，处理火、下界门、营火和蜡烛；每个成功方块的烟雾/权限检查和最终声音顺序与原版一致。
- [x] `conjure_block`：成本 `D`，目标位置必须可替换且有编辑权限；施法阶段放置临时构筑方块、写入当前 pigment/owner、保存方块快照以便 VM 回滚。
- [x] `conjure_light`：成本 `D`，与 `conjure_block` 相同，但使用构筑光源；核对光源方块实体、选择框、覆盖/破坏、pigment 和回滚。
- [x] `bonemeal`：成本 `9D/8`，消费位置并检查编辑权限；按原版伪造骨粉使用并保留世界随机成长结果，失败不能误消耗媒质或改方块。
- [x] `recharge`：成本 `S`，目标必须是范围内的物品实体，副手必须是可充能媒质容器且有空间，源物品必须是媒质；按容器剩余空间部分提取，核对离散媒质整物品消耗、NBT、实体死亡和回滚。
- [x] `erase`：无参数，目标为可清除 Hex 或 Iota 的持有物；成本为目标堆叠数乘 `D`；施法阶段清除 Hex 与 Iota，失败/堆叠选择和回滚必须一致。
- [x] `edify`：成本 `C`，目标必须是 sapling 标签方块且有编辑权限；施法阶段最多尝试 8 次 Akashic 树生长，核对破坏权限、随机源和部分成功行为。
- [x] `beep`：成本 `D/10`，消费位置、乐器索引和 0..24 音符；位置范围与参数逆序先校验，再广播声音包和 NOTE_BLOCK 游戏事件；该动作不播放普通施法音。
- [x] `craft/cypher`：成本 `C`，消费物品实体与 Iota 列表；副手须是空的 Cypher/Ancient Cypher HexHolder，媒质实体必须可抽取，真名保护先于任何写入；写入图案、pigment 和实体媒质，失败不能吞实体物品。
- [x] `craft/trinket`：成本 `5C`，与 `craft/cypher` 相同的参数、空 HexHolder、媒质实体、真名保护、写入和回滚规则，目标物品类型改为 Trinket。
- [x] `craft/artifact`：成本 `10C`，与 `craft/cypher` 相同，目标物品类型改为 Artifact；核对大型图案列表和真名扫描。
- [x] `craft/battery`：成本 `C`，消费物品实体并要求副手恰好一件瓶子；源物品必须可抽取媒质，抽出的媒质写入 Battery，源堆叠减法/实体死亡和失败回滚必须原子。

##### 药水效果、飞行和 Sentinel

- [x] `potion/weakness`：成本 `D/10 * duration * potency^2`；目标为非 ArmorStand LivingEntity，允许 potency 1..127，先验证持续时间/强度/范围再应用效果。
- [x] `potion/levitation`：成本 `D/5 * duration`；不接收 potency，只消费目标与持续时间；目标筛选、正持续时间、范围和效果等级保持原版。
- [x] `potion/wither`：成本 `D * duration * potency^2`；允许 potency，目标/验证/效果顺序与原版一致。
- [x] `potion/poison`：成本 `D/3 * duration * potency^2`；允许 potency，核对 ticks 截断、效果等级和非有限输入。
- [x] `potion/slowness`：成本 `D/3 * duration * potency^2`；允许 potency，目标必须为非 ArmorStand LivingEntity。
- [x] `potion/regeneration`：成本 `D * duration * potency^3`；允许 potency，核对立方成本、效果刷新和失败回滚。
- [x] `potion/night_vision`：成本 `D/5 * duration`；不接收 potency，核对夜视的持续时间和效果标志。
- [x] `potion/absorption`：成本 `D * duration * potency^3`；允许 potency，核对护盾效果刷新与已有效果。
- [x] `potion/haste`：成本 `D/3 * duration * potency^3`；允许 potency，核对目标、ticks 和效果等级。
- [x] `potion/strength`：成本 `D/3 * duration * potency^3`；允许 potency，核对伤害提升效果及失败行为。
- [x] `flight/range`：消费玩家与正半径；成本 `max(2D, round(2D*radius))`；目标范围和已有创造/旁观飞行状态与原版一致，设置飞行能力及来源快照。
- [x] `flight/time`：消费玩家与正秒数；成本 `round(2D*seconds)`；设置按 tick 倒计时的飞行能力，核对整数 tick、危险阈值和结束时能力清除。
- [x] `flight/can_fly`：消费玩家并做范围检查；无额外成本，只返回是否存在 Hex Flight 能力，不能把原版创造飞行误报为 true。
- [x] `flight`：Altiora 卓越法术，消费玩家并做范围检查，成本 `C`；施法时上推 1.5、设置 20 tick 碰撞宽限；落地/水平碰撞时清除能力、能力同步、声音和 pigment 粒子顺序与原版一致。
- [x] `sentinel/create`：成本 `D`，消费位置并做范围/世界边界检查；保存位置、维度和普通范围，替换旧 Sentinel 前先登记回滚并同步。
- [x] `sentinel/create/great`：成本 `2D`，与普通创建相同但启用扩展范围标志；核对维度保存、同步和失败回滚。
- [x] `sentinel/destroy`：成本 `D/10`；无 Sentinel 时仍按原版消费并结束，有跨维度 Sentinel 时先报错；清除后同步，失败不能清空错误世界的数据。
- [x] `sentinel/get_pos`：成本 `D/10`；无 Sentinel 返回 Null，同维度返回位置，跨维度使用原版 wrong-dimension Mishap。
- [x] `sentinel/wayfind`：成本 `D/10`；消费起点位置，无 Sentinel 返回 Null，同维度返回归一化方向，跨维度失败；核对零向量和非有限输入。
- [x] `lightning`：成本 `3S`，消费位置并检查范围/编辑权限；服务端添加 Lightning 实体，天气列表/实体同步和 VM 回滚必须完整。
- [x] `teleport/great`：成本 `10C`，消费实体和位移向量；检查不可传送实体、黏性载具乘客、维度配置、目标世界边界和下方高度；施法时保持载具关系规则，并核对卓越传送掉落物配置及回滚。
- [x] `summon_rain`：无参数、成本 `C`；按原版随机 30..90 分钟雨时长、雷暴概率和天气数据写入顺序。
- [x] `dispel_rain`：无参数、成本 `S`；按原版随机 60..180 分钟晴朗时长、清除雷暴并核对已是晴天时的行为。
- [x] `brainsweep`：消费目标实体和位置，使用匹配配方的动态媒质成本；核对实体/方块筛选、范围、权限、配方失败、已剥离意识实体、方块替换、掉落、真实伤害/死亡和失败回滚顺序。

##### Akashic、可选联动和动作清单收口

- [x] `akashic/read`：成本 `D`，消费位置并要求 Akashic Record；无记录使用原版 `NoAkashicRecord`，读取世界数据不能跨维度或越界。
- [x] `akashic/write`：成本 `D`，消费位置与 Iota；要求记录存在且通过真名保护，先模拟写入再在施法阶段提交，失败不得覆盖原数据。
- [x] 已确认 Pehkui 的 `interop/pehkui/get`、`interop/pehkui/set` 是可选联动，不属于核心动作完成范围；详细任务已移至“待完成”。

#### 动作审查后的收口要求

- [x] 已将上面所有核心动作逐项与原版实现核对，并按 `成本/目标/顺序/失败/回滚` 收口；不能只勾父项或只记录 `thanatos`。
- [x] 已补充对应的静态边界核对：参数不足、错误类型、范围/维度、无媒质、媒质不足、施法中途失败和 VM 回滚。
- [x] 已核对核心动作的注册图案、英文/中文 lang、Patchouli/JEI 显示和施法音效；重复数字别名已移除。
- [x] 完成动作修正后已更新本节状态；本轮不启动游戏，使用构建作为代码验证。

#### P0 实施收口（2026-09-27）

- 已完成：原版动作注册表、5 项 Sentinel、原版动作实现目录与目标版动作注册/实现的静态对照。
- 已完成：核心注册集合与行为差异已收口；`const/vec/0` 已核对一致，`push_zero/push_one` 已移除；仅保留可选 Pehkui 差异。
- 已完成：媒质事务、失败回滚、`get_media`、Mishap 反馈和核心动作清单均已落地；后续不重复全量审查，直接处理新的明确差异。
- 本轮不启动游戏、不构建、不修改 Java/Kotlin/资源代码；工作区原有 `build/` 产物和客户端源码改动不纳入本轮文档提交。

### P1：能力、物品能力与同步

- [x] 补齐 HexHolder、物品变体和渐变染色剂能力
- [x] 补齐掉落物、物品展示框、墙上卷轴等实体的 Iota 能力
- [x] 完善 Impetus 对物品和实体的处理能力
- [x] 完善登录、重生、换维度、实体追踪时的能力同步
- [x] 核对玩家、箭、三叉戟和掉落物等特殊速度读取（1.12.2 无原版三叉戟实体，箭类特殊速度覆盖可用的箭类实体）

### P1：注册、属性、效果和配置

- [x] 核对 `grid_zoom`、`scry_sight`、`feeble_mind`、`media_consumption`、`ambit_radius`、`sentinel_radius`
- [x] 补齐绘制网格放大/缩小效果
- [x] 补齐 common/client/server 配置（含客户端 `gridSnapThreshold`）
- [x] 完善自定义粒子、统计数据和进度触发器注册
- [x] 将探知透镜等功能的硬编码数值改为属性或配置驱动

### P1：方块行为与世界交互

- [x] 对照原版核验硬度、爆炸抗性、工具等级、可燃性和火焰传播
- [x] 核验光照、透明度、碰撞、选择框、水logged 和方向状态
- [x] 核验掉落、精准采集、时运、斧头去皮和堆肥行为
- [x] 完善树苗、树叶、树干、Akashic 树生成和方块连接行为
- [x] 核验构筑方块/光源的破坏、落地、踩踏和粒子行为

#### P1 实施收口（2026-09-27）

- 已完成能力、物品能力、实体 Iota、Impetus 和生命周期同步。
- 已完成属性、客户端绘制配置、粒子/统计/进度注册和探知透镜属性驱动。
- 已完成方块属性、燃烧行为、光照/水logged/选择框、树与 Akashic 行为，以及构筑方块/光源交互。
- Sentinel 扩展范围已并入目标筛选、位置校验和射线长度计算；墙上卷轴保持只读。
- 本轮不启动游戏，仅使用 `./gradlew.bat build` 验证代码并生成产物。

## 待完成

### P2：配方、战利品和数据资源

- [ ] 对照原版核验物品、方块、法杖、颜色器和伟大工程配方
- [ ] 核验可选模组配方的加载条件和配方 ID
- [ ] 补齐随机卷轴、随机 Cypher 战利品和 Patchouli 解锁进度
- [ ] 核验淬灵晶、紫水晶簇和充能紫水晶掉落
- [ ] 补齐 advancements、loot tables、worldgen 和方块/动作/实体标签

### P2：客户端视觉效果最终对照

- [ ] 对照原版核验粒子大小、重力、透明度和混合模式
- [ ] 核验内化染色剂对构筑方块、光源、施法特效、环绕符文和淬灵晶特效的影响
- [ ] 核验渐变染色按时间渐变、重进世界后的颜色和符文同步
- [ ] 核验第一视角手部 OpenGL 状态、Altiora 鞘翅和 JEI 实体渲染
- [ ] 核验聊天内联图案与法杖 GUI 的渲染状态隔离
- [ ] 核验墙上卷轴、Slate、Akashic Bookshelf 的图案显示

### P3：命令、调试和管理功能

- [ ] brainsweep 调试命令
- [ ] 周世界图案列表、图案纹理和卓越法术图案重算命令
- [ ] 原版相关日志和调试输出

### P3：可选联动与资源清理

- [ ] 可选移植 Pehkui 的 `interop/pehkui/get` 和 `interop/pehkui/set`；仅在 Pehkui 存在时注册，并核对成本、范围、缩放值、失败回滚和缺少联动时的注册条件
- [ ] 核对 BaublesEX 与原版 Curios 能力语义
- [ ] 清理无效注册、无效物品、死资源引用和多余 lang 键
- [ ] 核对 `ItemPackagedSpell` 基类和 `HexItems.STAFF` 别名，避免误删或重复注册
- [ ] 确认 OptiFine CTM、robes、spin cube 等资源在 1.12.2 中是否有消费者

## 平台说明

- EMI 属于较新版本生态，不列为 1.12.2 移植项。
- 本清单只描述与原版等效性相关的工作，不要求重做已经完成的安全基线。
