# 1.12.2 资源覆盖检查

生成日期：2026-09-12。检查基线：2508076 docs: record resource and action audit; 50db687 docs: add 1.12.2 porting audit; 673bbe6 feat: add Inline payload serialization

## 检查结果

- 注册源码候选物品 ID：6
- `assets/hexcasting/models/item` 模型文件：0
- 缺失模型（本次补齐）：6
- `en_us.lang` 键：0
- `zh_cn.lang` 键：0
- 从 `_cn.flatten.json5` 补齐的中文键：0

## 本次补齐的模型

- battery
- focus
- patchouli_book
- pattern_scroll
- scrying_lens
- staff

## 本次补齐的中文键

无。

## 说明

- 该检查只处理资源文件，不把本地化文本写进 Java/Kotlin 代码。
- 模型使用现有 1.12.2 item 模型的纹理目录风格；如果某个纹理仍显示 missingno，需要在客户端新启动日志中进一步核对实际 PNG 文件名。
- 现有客户端日志中的 Realms、JEI 旧书签和 CraftTweaker 报错不属于 HexCasting 资源加载错误。

