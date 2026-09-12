# 1.12.2 移植审计

生成日期：2026-09-12。当前审计基线为提交 673bbe6（Inline payload 序列化）。本文件记录本机源码、资源目录和客户端日志的静态检查结果；它不把能编译当作运行时完整。

## 已验证基线

- 构建命令固定为 gradlew.bat build --offline。
- 1.12.2 核心 Hex、可编程法杖 MVP、Patchouli 指南书、物品模型和纹理、本地化、BaublesEX 媒质槽位、基础 JEI、Paucal 和 Inline 兼容层已经在此前提交中落地。
- 当前仍需在客户端实际启动后确认纹理、模型、图案渲染、指南书唯一性和法杖施法闭环。

## 本次静态检查证据

以下内容由本机工作区和 D:/pcl2/.minecraft/versions/1.12.2-Forge_14.23.5.2864/logs/latest.log 直接采集：

----- BEGIN EVIDENCE -----
===STATUS===
673bbe6 feat: add Inline payload serialization
36fb9bd feat: match Hex patterns in Inline text
d5d22bc feat: extend Inline and Paucal compatibility APIs
===ACTION_EVIDENCE===
HexActions.java not found
===UPSTREAM_CANDIDATES===
===LOG_MATCHES===
41:[10:50:22] [main/INFO] [FML]: FML appears to be missing any signature data. This is expected, don't worry.
78:[10:50:24] [Client thread/INFO] [Config]: [OptiFine] Maximum texture size: 32768x32768
86:[10:50:24] [Client thread/INFO] [net.optifine.shaders.SMCLog]: [Shaders] GL_MAX_TEXTURE_IMAGE_UNITS: 32
125:[10:50:25] [Client thread/WARN] [FML]: Mod baubles is missing the required element 'version' and a version.properties file could not be found. Falling back to metadata version 2.3.7
126:[10:50:25] [Client thread/WARN] [FML]: Mod patchouli is missing the required element 'version' and a version.properties file could not be found. Falling back to metadata version 1.0-23.6
274:[10:50:27] [Client thread/INFO] [FML]: Attempting connection with missing mods [minecraft, mcp, cleanroom, cleanmix, mixinbooter, configanytime, kirino_engine, kirino_ecs, kirino_gl, FML, forge, cleanroom-relauncher, scalar, hexcasting, fugue, baubles, ctgui, crafttweaker, crafttweakerjei, forgelin_continuous, forgelin, jei, patchouli, theoneprobe, tiab] at CLIENT
275:[10:50:27] [Client thread/INFO] [FML]: Attempting connection with missing mods [minecraft, mcp, cleanroom, cleanmix, mixinbooter, configanytime, kirino_engine, kirino_ecs, kirino_gl, FML, forge, cleanroom-relauncher, scalar, hexcasting, fugue, baubles, ctgui, crafttweaker, crafttweakerjei, forgelin_continuous, forgelin, jei, patchouli, theoneprobe, tiab] at SERVER
313:[10:50:30] [Client thread/INFO] [Config]: [OptiFine] *** Reloading textures ***
325:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] Multitexture: false
326:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] Multipass connected textures: false
327:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/0_glass_white/glass_pane_white.properties
328:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/0_glass_white/glass_white.properties
329:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/10_glass_purple/glass_pane_purple.properties
330:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/10_glass_purple/glass_purple.properties
331:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/11_glass_blue/glass_blue.properties
332:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/11_glass_blue/glass_pane_blue.properties
333:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/12_glass_brown/glass_brown.properties
334:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/12_glass_brown/glass_pane_brown.properties
335:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/13_glass_green/glass_green.properties
336:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/13_glass_green/glass_pane_green.properties
337:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/14_glass_red/glass_pane_red.properties
338:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/14_glass_red/glass_red.properties
339:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/15_glass_black/glass_black.properties
340:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/15_glass_black/glass_pane_black.properties
341:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/1_glass_orange/glass_orange.properties
342:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/1_glass_orange/glass_pane_orange.properties
343:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/2_glass_magenta/glass_magenta.properties
344:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/2_glass_magenta/glass_pane_magenta.properties
345:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/3_glass_light_blue/glass_light_blue.properties
346:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/3_glass_light_blue/glass_pane_light_blue.properties
347:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/4_glass_yellow/glass_pane_yellow.properties
348:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/4_glass_yellow/glass_yellow.properties
349:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/5_glass_lime/glass_lime.properties
350:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/5_glass_lime/glass_pane_lime.properties
351:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/6_glass_pink/glass_pane_pink.properties
352:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/6_glass_pink/glass_pink.properties
353:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/7_glass_gray/glass_gray.properties
354:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/7_glass_gray/glass_pane_gray.properties
355:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/8_glass_silver/glass_pane_silver.properties
356:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/8_glass_silver/glass_silver.properties
357:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/9_glass_cyan/glass_cyan.properties
358:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/9_glass_cyan/glass_pane_cyan.properties
359:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/bookshelf.properties
360:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/glass.properties
361:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/glasspane.properties
362:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] ConnectedTextures: mcpatcher/ctm/default/sandstone.properties
363:[10:50:32] [Client thread/INFO] [Config]: [OptiFine] Multipass connected textures: false
368:[10:50:33] [Client thread/INFO] [net.minecraft.client.renderer.texture.TextureMap]: Created: 1024x512 textures-atlas
372:[10:50:34] [Client thread/INFO] [jei]: Created: 128x256 textures-atlas
373:[10:50:34] [Client thread/ERROR] [tiab]: test
388:[10:50:34] [Client thread/WARN] [jei]: Failed to load bookmarked ItemStack from json string, the item no longer exists:
390:[10:50:34] [Client thread/WARN] [jei]: Failed to load bookmarked ItemStack from json string, the item no longer exists:
392:[10:50:34] [Client thread/WARN] [jei]: Failed to load bookmarked ItemStack from json string, the item no longer exists:
394:[10:50:34] [Client thread/WARN] [jei]: Failed to load bookmarked ItemStack from json string, the item no longer exists:
409:[10:50:34] [Client thread/ERROR] [net.minecraft.realms.RealmsBridge]: Failed to load Realms module
410:java.lang.NoSuchMethodException: com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen.<init>(net.minecraft.realms.RealmsScreen)
427:[10:50:35] [Client thread/INFO] [Config]: [OptiFine] *** Reloading custom textures ***
428:[10:51:19] [Client thread/ERROR] [net.minecraft.realms.RealmsBridge]: Failed to load Realms module
429:java.lang.NoSuchMethodException: com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen.<init>(net.minecraft.realms.RealmsScreen)
469:[10:51:23] [Client thread/INFO] [net.minecraft.client.gui.GuiNewChat]: [CHAT] §cERROR: mythicbotany_crt_example.zs:27: Could not resolve <botania : rune>
470:[10:51:23] [Client thread/INFO] [net.minecraft.client.gui.GuiNewChat]: [CHAT] §cERROR: mythicbotany_crt_example.zs:28: Could not resolve <mythicbotany : helheim_rune>
471:[10:51:23] [Client thread/INFO] [net.minecraft.client.gui.GuiNewChat]: [CHAT] §cERROR: mythicbotany_crt_example.zs:5 > No such member: mythicbotany
472:[10:51:23] [Client thread/INFO] [net.minecraft.client.gui.GuiNewChat]: [CHAT] §cERROR: mythicbotany_crt_example.zs:12 > No such member: mythicbotany
473:[10:51:23] [Client thread/INFO] [net.minecraft.client.gui.GuiNewChat]: [CHAT] §cERROR: mythicbotany_crt_example.zs:13 > Cannot cast ZenTypeNative: crafttweaker.item.IItemStack to ZenTypeNative: crafttweaker.data.IData
474:[10:51:23] [Client thread/INFO] [net.minecraft.client.gui.GuiNewChat]: [CHAT] §cERROR: mythicbotany_crt_example.zs:13 > Cannot cast ZenTypeNative: crafttweaker.item.IItemStack to ZenTypeNative: crafttweaker.data.IData
475:[10:51:23] [Client thread/INFO] [net.minecraft.client.gui.GuiNewChat]: [CHAT] §cERROR: mythicbotany_crt_example.zs:13 > Cannot cast ZenTypeNative: crafttweaker.item.IItemStack to ZenTypeNative: crafttweaker.data.IData
476:[10:51:23] [Client thread/INFO] [net.minecraft.client.gui.GuiNewChat]: [CHAT] §cERROR: mythicbotany_crt_example.zs:13 > Cannot cast ZenTypeNative: crafttweaker.item.IItemStack to ZenTypeNative: crafttweaker.data.IData
477:[10:51:23] [Client thread/INFO] [net.minecraft.client.gui.GuiNewChat]: [CHAT] §cERROR: mythicbotany_crt_example.zs:23 > No such member: mythicbotany
478:[10:51:23] [Client thread/INFO] [net.minecraft.client.gui.GuiNewChat]: [CHAT] §cERROR: mythicbotany_crt_example.zs:25 > No such member: mythicbotany
479:[10:51:23] [Client thread/INFO] [net.minecraft.client.gui.GuiNewChat]: [CHAT] §cERROR: mythicbotany_crt_example.zs:25 > Cannot cast ZenTypeNative: crafttweaker.item.IItemStack to ZenTypeNative: crafttweaker.data.IData
480:[10:51:23] [Client thread/INFO] [net.minecraft.client.gui.GuiNewChat]: [CHAT] §cERROR: [crafttweaker]: Error executing {[0:crafttweaker]: mythicbotany_crt_example.zs}: Bad type on operand stack\nException Details:\n  Location:\n    Mythicbotany_crt_example.__script__()V @58: ifnonnull\n  Reason:\n    Type integer (current frame, stack[5]) is not assignable to reference type\n  Current Frame:\n    bci: @58\n    flags: { }\n    locals: { }\n    stack: { null, '[Lstanhebben/zenscript/value/IAny;', '[Lstanhebben/zenscript/value/IAny;', integer, integer, integer }\n  Bytecode:\n    0000000: 0112 0abd 000c 5912 0d12 0f12 0db8 0015\n    0000010: 59c7 0008 5701 a700 06b8 001b 5359 121c\n    0000020: 121e 120d b800 1559 c700 0857 01a7 0006\n    0000030: b800 1b53 5912 1f12 2059 c700 0857 01a7\n    0000040: 0006 b800 25b8 002b 59c7 0008 5701 a700\n    0000050: 06b8 0025 53b9 002f 0200 5701 120a bd00\n    0000060: 0c59 120d 1230 bd00 3259 120d 0153 5912\n    0000070: 1c01 5359 121f 0153 5912 0a01 5359 c700\n    0000080: 0857 01a7 0006 b800 25b8 0038 59c7 0008\n    0000090: 5701 a700 06b8 0025 5359 121c 121e 120d\n    00000a0: b800 1559 c700 0857 01a7 0006 b800 1b53\n    00000b0: 5912 1f12 3959 c700 0857 01a7 0006 b800\n    00000c0: 25b8 002b 59c7 0008 5701 a700 06b8 0025\n    00000d0: 53b9 002f 0200 5701 123a bd00 0c59 120d\n    00000e0: 123c 120d b800 1559 c700 0857 01a7 0006\n    00000f0: b800 1b53 5912 1c12 1fbd 0032 5912 0d01\n    0000100: 5359 121c 0112 1cbd 000c 5912 0d12 3e59\n    0000110: c700 0857 01a7 0006 b800 43b8 0049 53b9\n    0000120: 002f 0200 1232 b900 4d02 0053 59c7 0008\n    0000130: 5701 a700 06b8 0025 b800 3859 c700 0857\n    0000140: 01a7 0006 b800 2553 5912 1f12 3c12 1cb8\n    0000150: 0015 59c7 0008 5701 a700 06b8 001b 5359\n    0000160: 120a 123a bd00 3259 120d 121c b800 2b53\n    0000170: 5912 1c12 1cb8 002b 5359 121f 0153 5912\n    0000180: 0a12 1cb8 002b 5359 1230 120d b800 2b53\n    0000190: 5912 4e01 5359 c700 0857 01a7 0006 b800\n    00001a0: 25b8 0038 59c7 0008 5701 a700 06b8 0025\n    00001b0: 5359 1230 124f 59c7 0008 5701 a700 06b8\n    00001c0: 0025 b800 2b59 c700 0857 01a7 0006 b800\n    00001d0: 2553 5912 4e12 5059 c700 0857 01a7 0006\n    00001e0: b800 25b8 002b 59c7 0008 5701 a700 06b8\n    00001f0: 0025 53b9 002f 0200 57b1               \n  Stackmap Table:\n    full_frame(@25,{},{Null,Object[#82],Object[#82],Integer,Object[#84]})\n    full_frame(@28,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@48,{},{Null,Object[#82],Object[#82],Integer,Object[#84]})\n    full_frame(@51,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@66,{},{Null,Object[#82],Object[#82],Integer,Integer})\n    full_frame(@69,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@81,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@84,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@134,{},{Null,Object[#82],Object[#82],Integer,Object[#86]})\n    full_frame(@137,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@149,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@152,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@172,{},{Null,Object[#82],Object[#82],Integer,Object[#84]})\n    full_frame(@175,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@190,{},{Null,Object[#82],Object[#82],Integer,Integer})\n    full_frame(@193,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@205,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@208,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@240,{},{Null,Object[#82],Object[#82],Integer,Object[#84]})\n    full_frame(@243,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@280,{},{Null,Object[#82],Object[#82],Integer,Object[#86],Object[#86],Integer,Null,Object[#82],Object[#82],Integer,Object[#88]})\n    full_frame(@283,{},{Null,Object[#82],Object[#82],Integer,Object[#86],Object[#86],Integer,Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@309,{},{Null,Object[#82],Object[#82],Integer,Object[#86]})\n    full_frame(@312,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@324,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@327,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@347,{},{Null,Object[#82],Object[#82],Integer,Object[#84]})\n    full_frame(@350,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@414,{},{Null,Object[#82],Object[#82],Integer,Object[#86]})\n    full_frame(@417,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@429,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@432,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@447,{},{Null,Object[#82],Object[#82],Integer,Integer})\n    full_frame(@450,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@462,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@465,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@480,{},{Null,Object[#82],Object[#82],Integer,Integer})\n    full_frame(@483,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@495,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@498,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n, caused by java.lang.VerifyError: Bad type on operand stack\nException Details:\n  Location:\n    Mythicbotany_crt_example.__script__()V @58: ifnonnull\n  Reason:\n    Type integer (current frame, stack[5]) is not assignable to reference type\n  Current Frame:\n    bci: @58\n    flags: { }\n    locals: { }\n    stack: { null, '[Lstanhebben/zenscript/value/IAny;', '[Lstanhebben/zenscript/value/IAny;', integer, integer, integer }\n  Bytecode:\n    0000000: 0112 0abd 000c 5912 0d12 0f12 0db8 0015\n    0000010: 59c7 0008 5701 a700 06b8 001b 5359 121c\n    0000020: 121e 120d b800 1559 c700 0857 01a7 0006\n    0000030: b800 1b53 5912 1f12 2059 c700 0857 01a7\n    0000040: 0006 b800 25b8 002b 59c7 0008 5701 a700\n    0000050: 06b8 0025 53b9 002f 0200 5701 120a bd00\n    0000060: 0c59 120d 1230 bd00 3259 120d 0153 5912\n    0000070: 1c01 5359 121f 0153 5912 0a01 5359 c700\n    0000080: 0857 01a7 0006 b800 25b8 0038 59c7 0008\n    0000090: 5701 a700 06b8 0025 5359 121c 121e 120d\n    00000a0: b800 1559 c700 0857 01a7 0006 b800 1b53\n    00000b0: 5912 1f12 3959 c700 0857 01a7 0006 b800\n    00000c0: 25b8 002b 59c7 0008 5701 a700 06b8 0025\n    00000d0: 53b9 002f 0200 5701 123a bd00 0c59 120d\n    00000e0: 123c 120d b800 1559 c700 0857 01a7 0006\n    00000f0: b800 1b53 5912 1c12 1fbd 0032 5912 0d01\n    0000100: 5359 121c 0112 1cbd 000c 5912 0d12 3e59\n    0000110: c700 0857 01a7 0006 b800 43b8 0049 53b9\n    0000120: 002f 0200 1232 b900 4d02 0053 59c7 0008\n    0000130: 5701 a700 06b8 0025 b800 3859 c700 0857\n    0000140: 01a7 0006 b800 2553 5912 1f12 3c12 1cb8\n    0000150: 0015 59c7 0008 5701 a700 06b8 001b 5359\n    0000160: 120a 123a bd00 3259 120d 121c b800 2b53\n    0000170: 5912 1c12 1cb8 002b 5359 121f 0153 5912\n    0000180: 0a12 1cb8 002b 5359 1230 120d b800 2b53\n    0000190: 5912 4e01 5359 c700 0857 01a7 0006 b800\n    00001a0: 25b8 0038 59c7 0008 5701 a700 06b8 0025\n    00001b0: 5359 1230 124f 59c7 0008 5701 a700 06b8\n    00001c0: 0025 b800 2b59 c700 0857 01a7 0006 b800\n    00001d0: 2553 5912 4e12 5059 c700 0857 01a7 0006\n    00001e0: b800 25b8 002b 59c7 0008 5701 a700 06b8\n    00001f0: 0025 53b9 002f 0200 57b1               \n  Stackmap Table:\n    full_frame(@25,{},{Null,Object[#82],Object[#82],Integer,Object[#84]})\n    full_frame(@28,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@48,{},{Null,Object[#82],Object[#82],Integer,Object[#84]})\n    full_frame(@51,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@66,{},{Null,Object[#82],Object[#82],Integer,Integer})\n    full_frame(@69,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@81,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@84,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@134,{},{Null,Object[#82],Object[#82],Integer,Object[#86]})\n    full_frame(@137,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@149,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@152,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@172,{},{Null,Object[#82],Object[#82],Integer,Object[#84]})\n    full_frame(@175,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@190,{},{Null,Object[#82],Object[#82],Integer,Integer})\n    full_frame(@193,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@205,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@208,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@240,{},{Null,Object[#82],Object[#82],Integer,Object[#84]})\n    full_frame(@243,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@280,{},{Null,Object[#82],Object[#82],Integer,Object[#86],Object[#86],Integer,Null,Object[#82],Object[#82],Integer,Object[#88]})\n    full_frame(@283,{},{Null,Object[#82],Object[#82],Integer,Object[#86],Object[#86],Integer,Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@309,{},{Null,Object[#82],Object[#82],Integer,Object[#86]})\n    full_frame(@312,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@324,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@327,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@347,{},{Null,Object[#82],Object[#82],Integer,Object[#84]})\n    full_frame(@350,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@414,{},{Null,Object[#82],Object[#82],Integer,Object[#86]})\n    full_frame(@417,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@429,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@432,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@447,{},{Null,Object[#82],Object[#82],Integer,Integer})\n    full_frame(@450,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@462,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@465,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@480,{},{Null,Object[#82],Object[#82],Integer,Integer})\n    full_frame(@483,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n    full_frame(@495,{},{Null,Object[#82],Object[#82],Integer,Object[#50]})\n    full_frame(@498,{},{Null,Object[#82],Object[#82],Integer,Object[#12]})\n
482:[10:51:30] [Client thread/WARN] [net.minecraft.client.audio.SoundManager]: Unable to play empty soundEvent: minecraft:entity.small_slime.jump
483:[10:51:30] [Client thread/WARN] [net.minecraft.client.audio.SoundManager]: Unable to play empty soundEvent: minecraft:entity.small_slime.squish
===RESOURCES===
src/main/resources/assets/hexcasting: 26 files
src/main/resources/assets/inline: missing
src/main/resources/assets/paucal: missing
src/main/resources/assets/patchouli_books: missing

----- END EVIDENCE -----

## 风险判断

1. 1.12.2 Forge 与 1.20.1 Forge 的注册、资源、网络和客户端渲染 API 不兼容；上游源码不能通过机械复制完成移植。
2. 本地 jar 只解决 Gradle 的依赖解析，不能替代源码层面的 API 适配，也不能证明客户端运行时资源有效。
3. Forgelin-Continuous 当前作为运行时兼容依赖保留，但旧 ForgeGradle 和 Kotlin 编译器无法处理其 Kotlin metadata，因此项目 Kotlin 编译任务仍主动跳过。
4. Inline 的数据类型、匹配器和文本回退已经存在；真实聊天组件中的内嵌渲染、Tooltip overlay 和完整上游 matcher 仍属于后续工作。
5. Paucal 当前是 Hex 内部的 1.12.2 网络和 API 兼容层，不是完整 Paucal 资源、贡献者、命令、声音和 datagen 移植。

## 下一批实施顺序

1. 由动作注册和上游候选文件生成逐项 ID 与语义对照，优先补齐法杖编程闭环需要的读取、堆栈、实体和世界交互动作。
2. 用新启动产生的客户端日志验证物品纹理、模型、图案纹理、Patchouli 唯一指南书和法杖施法。
3. 对照 Patchouli 1.12.2 API 检查书页、配方页和中文本地化键。
4. 继续补齐 JEI 自定义 recipe category、Paucal 资源、贡献者、命令和 Inline 的真实聊天与 Tooltip 内嵌渲染。

