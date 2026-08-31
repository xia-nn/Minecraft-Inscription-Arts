# 铭刻之艺 · Inscription Arts

> 基于《匠魂（Tinkers' Construct）》设计哲学的 **原创装备强化模组**，运行于 **Fabric 1.21.1**。
> 用「符文铭刻」彻底替代原版附魔台的随机 roll——从材料中萃取符文精华，铭刻到装备上获得**固定、可预期**的强化。

- **Mod ID**：`inscription_arts`
- **包名**：`com.inscription_arts`
- **环境**：Minecraft 1.21.1 · Fabric Loader 0.19.3 · Fabric API 0.116.12+ · Java 21

---

## 一、核心玩法循环

```
 材料 ──(萃取台)──▶ 符文精华 ──┐
                              ├─(符文祭坛)──▶ 铭刻后的装备（固定强化）
 工具/武器 ───────────────────┘
```

1. **萃取**：把材料放入「萃取台」右键，得到对应**符文精华**。
2. **建坛**：用「符文祭坛核心」搭好多方块结构。
3. **铭刻**：在祭坛界面里把精华放进装备的 前缀 / 核心 / 后缀 三槽，点击「铭刻」即写入。
4. **修复**：受损的铭刻装备用「对应材料」在萃取台修复，装备**不会消失**。

每件装备有 **3 个固定槽位**，每个槽位放入对应符文即获得固定效果；可反复铭刻叠高附魔等级。

---

## 二、合成配方

| 物品 | 配方 |
|---|---|
| 萃取台 `extractor_block` | 黑石 3×3 中空，中心铁块（I） |
| 符文祭坛核心 `rune_altar_core` | 黑曜石四角(O)、黑石十字(I)、中心下界合金锭(N) |

```
萃取台：            祭坛核心：
 B B B              O I O
 B I B              I N I
 B B B              O I O
B=黑石  I=铁块      O=黑曜石 I=黑石 N=下界合金锭
```

> 若不想合成，也可在创造模式「铭刻之艺」标签页直接拿取所有方块与物品。

---

## 三、符文祭坛结构（多方块）

以「符文祭坛核心」为中心，需满足：

- **基座**：核心**正下方一层**为 3×3 的**黑石（Blackstone）**；
- **立柱**：核心**同一层**的四个对角位置放 **黑曜石（Obsidian）**。

```
俯视（核心所在层 Y）：       侧视（核心在中间）：
O . O  (O=黑曜石)           Y  :   . O .   (核心左右后方为黑曜石)
. C .  (C=核心)              Y-1: O B B O  (黑石基座在核心下方一层)
O . O                        （B=黑石基座 3×3，O=对角黑曜石）
```

结构不完整时祭坛无法铭刻（服务端会拒绝请求并提示）。

---

## 四、符文图鉴

每个符文对应一种从材料萃取出的精华，铭刻到装备的指定槽位后提供能力。
**强度（strength）= 每次铭刻为每个精华槽施加的附魔等级增量**，越稀有增量越大。

| 符文（中文 / id） | 槽位 | 主效果（写入的附魔） | 稀有度 | 强度 | 萃取材料 |
|---|---|---|---|---|---|
| 煤痕铭文 `coal` | 后缀 | 击退（另附额外击退冲量） | 普通 | 1 | 煤 |
| 铜辉铭文 `copper` | 前缀 | 效率 | 普通 | 1 | 铜锭 |
| 铁锋铭文 `iron_edge` | 前缀 | 锋利 / 击退（随机其一） | 普通 | 1 | 铁锭 |
| 纸纹铭文 `paper_slot` | 后缀 | 耐久（不毁） | 普通 | 1 | 纸 |
| 金运铭文 `gold_luck` | 核心 | 时运 | 少见 | 2 | 金锭 |
| 钻耀铭文 `diamond` | 核心 | **烈焰**（攻击点燃） | 稀有 | 3 | 钻石 |
| 翡华铭文 `emerald` | 后缀 | **淬毒**（攻击中毒） | 稀有 | 3 | 绿宝石 |
| 冥金铭文 `netherite` | 前缀 | **汲取**（攻击回血） | 极稀有 | 4 | 下界合金锭 |

### 自创附魔（稀有材料解锁）

| 附魔 | 触发 | 效果 |
|---|---|---|
| 烈焰 `blazing` | 攻击命中 | 点燃目标，每级燃烧约 2 秒 |
| 淬毒 `venom` | 攻击命中 | 使目标中毒，每级持续约 3 秒、毒级 = 附魔等级−1 |
| 汲取 `siphon` | 攻击命中 | 回复生命，每级回复 1 颗心（2 HP） |

> 自创附魔**突破原版附魔等级上限**，最高可叠到 **X（10 级）**。重复铭刻同一符文会累加等级。

---

## 五、自创工具（范围挖掘）

| 工具 | 类型 | 能力 |
|---|---|---|
| 符文之锤 `hammer` | 镐（下界合金级） | 破坏方块时**以该格为中心 3×3 同层**一并挖掘（仅采集可正确掉落的方块，跳过基岩/受保护区域） |
| 符文之铲 `excavator` | 铲（下界合金级） | 同上，针对泥土 / 沙 / 砾石等 |

两件工具本身也是「装备」，可在祭坛铭刻任意符文，效果由攻击 / 挖掘事件正常生效。

---

## 六、修复系统

铭刻装备本质仍是普通物品，**耐久耗尽也不会消失**。修复方式：

- 一手持有**已受损的铭刻装备**，另一手持有其**对应材料**（= 装备上强度最高符文所来源的材料）；
- 右键**萃取台**：消耗 1 个材料，为装备恢复 `25 × 材料强度` 点耐久；
- 若材料不对，会提示「需要用 X 修复此铭刻装备」。

---

## 七、数值平衡

核心数值集中在 `src/main/java/com/inscription_arts/balance/ModConfig.java`，便于调参：

| 常量 | 默认值 | 含义 |
|---|---|---|
| `MAX_LEVEL` | 10 | 附魔等级上限（突破原版） |
| `REPAIR_AMOUNT` | 25 | 每次修复基础耐久，实际 = `25 × 材料强度` |
| `BLAZING_SECONDS_PER_LEVEL` | 2 | 烈焰每级燃烧秒数 |
| `VENOM_TICKS_PER_LEVEL` | 60 | 淬毒每级持续刻数（20 刻 = 1 秒） |
| `SIPHON_HEAL_PER_LEVEL` | 2.0 | 汲取每级回血（半颗心为单位，2 = 1 颗心） |

---

## 八、附属模组 API（阶段 5）

`com.inscription_arts.api.InscriptionApi` 与 `com.inscription_arts.api.events.InscriptionEvents`
开放给其它模组扩展：

```java
// 注册自定义符文 / 特性 / 可萃取材料（在 onInitialize 中、游戏内使用前调用）
InscriptionApi.registerRune(new RuneType(
    "my_rune", RuneSlot.CORE,
    Component.literal("我的符文"),
    List.of(ModTraits.DURABILITY), stack -> {}, 2));
InscriptionApi.registerTrait(new MaterialTrait("my_trait", "描述", Function.identity()));
InscriptionApi.registerMaterial(Items.AMETHyst, /* 你的 RuneType */ null);

// 在权威服务端对物品施加一次铭刻
InscriptionApi.inscribe(equipment, RuneSlot.CORE, "my_rune", player);

// 监听铭刻事件（前/后可取消、可读取最终数据）
InscriptionEvents.BEFORE_INSCRIBE.register((player, equipment, runes) -> true);
InscriptionEvents.AFTER_INSCRIBE.register((player, equipment, data) -> { /* ... */ });
```

---

## 九、常见问题

- **铭刻装备能放进原版附魔台吗？** 可以放，但本模组不依赖随机附魔——强化来自 `Data Component`，
  附魔台不会改变铭刻数据。请统一在符文祭坛进行操作。
- **祭坛提示无效？** 检查：核心正下方一层是否为 3×3 黑石，且核心同层四角为黑曜石。
- **等级上限是多少？** 自创附魔最高 10 级（显示为 X）。
- **精华能叠加吗？** 同名精华可堆叠（上限 16），每次铭刻只消耗 1 枚，可反复叠高等级。

---

## 十、构建与开发

```bash
./gradlew build          # 产出 build/libs/inscription_arts-1.0.0.jar
./gradlew runClient      # 启动带模组的客户端进行实测
./gradlew runServer      # 启动服务端验证加载无崩溃
```

代码组织（按分包）：

| 包 / 类 | 职责 |
|---|---|
| `api/*` | 符文、槽位、材料特性、铭刻数据、公开 API 与事件 |
| `component/ModDataComponents` | `INSCRIPTION` / `RUNE_ESSENCE` 数据组件 |
| `registry/*` | 符文、特性、附魔、物品、方块、创造标签页注册 |
| `block/*` `screen/*` `network/*` | 萃取台、祭坛、铭刻界面与网络包 |
| `effect/RuneEffectApplier` | 攻击 / 挖掘时施加符文效果 |
| `item/*` | 符文精华、铭刻手册、自创工具（锤/铲） |
| `data/ModMaterials` | 材料分层与成长曲线 |
| `balance/ModConfig` | 可调数值中心 |

详细的分阶段开发计划见 [`stellar-beacon-turing.md`](./stellar-beacon-turing.md)。
