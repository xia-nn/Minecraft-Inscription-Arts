package com.inscription_arts.registry;

import com.inscription_arts.api.MaterialTrait;
import com.inscription_arts.api.ToolCategory;

import java.util.List;

/**
 * 材料特性注册表。
 * <p>
 * 每个特性对应一个自创附魔（{@link ModEnchantments} 中的键）。
 * 铭刻时 {@code InscriptionApi} 按装备工具类别，从 {@link com.inscription_arts.api.RuneType}
 * 取出该类别对应的候选特性，玩家三选一后把充能累加到所选附魔。
 * <p>
 * 四组自创附魔（每工具类别一组候选池）：
 * <ul>
 *   <li>战斗组（剑/重锤/三叉戟/权杖）：烈焰/淬毒/汲取/雷霆/寒霜；</li>
 *   <li>斧组（斧）：兼具伤害与效率，战斗 5 + 挖掘 5 = 10 个；</li>
 *   <li>挖掘组（镐）：精炼/辉光/磁力/勘探/疾速；</li>
 *   <li>弓组：贯穿/精准/迅捷/爆裂/灼燃。</li>
 * </ul>
 */
public final class ModTraits {

    // —— 战斗组 ——
    public static final MaterialTrait BLAZING = new MaterialTrait(
            "blazing", "烈焰：攻击点燃目标，等级越高燃烧越久", ModEnchantments.BLAZING);
    public static final MaterialTrait VENOM = new MaterialTrait(
            "venom", "淬毒：攻击使目标中毒，等级越高毒越深", ModEnchantments.VENOM);
    public static final MaterialTrait SIPHON = new MaterialTrait(
            "siphon", "汲取：攻击回复生命，等级越高回血越多", ModEnchantments.SIPHON);
    public static final MaterialTrait THUNDER = new MaterialTrait(
            "thunder", "雷霆：攻击有几率召唤落雷，等级越高越频繁", ModEnchantments.THUNDER);
    public static final MaterialTrait FROST = new MaterialTrait(
            "frost", "寒霜：攻击减速冻结目标，等级越高越久", ModEnchantments.FROST);

    // —— 挖掘组（镐）——
    public static final MaterialTrait REFINE = new MaterialTrait(
            "refine", "精炼：破坏方块自动熔炼为成品", ModEnchantments.REFINE);
    public static final MaterialTrait RADIANCE = new MaterialTrait(
            "radiance", "辉光：持有时照亮四周（夜视）", ModEnchantments.RADIANCE);
    public static final MaterialTrait MAGNETISM = new MaterialTrait(
            "magnetism", "磁力：破坏时吸附附近掉落物", ModEnchantments.MAGNETISM);
    public static final MaterialTrait PROSPECT = new MaterialTrait(
            "prospect", "勘探：破坏矿石额外掉落，等级越高越多", ModEnchantments.PROSPECT);
    public static final MaterialTrait HASTE = new MaterialTrait(
            "haste", "疾速：提升挖掘速度，等级越高越快", ModEnchantments.HASTE);

    // —— 弓组 ——
    public static final MaterialTrait PIERCE = new MaterialTrait(
            "pierce", "贯穿：箭矢可穿透更多实体", ModEnchantments.PIERCE);
    public static final MaterialTrait PRECISION = new MaterialTrait(
            "precision", "精准：箭矢伤害提升", ModEnchantments.PRECISION);
    public static final MaterialTrait VELOCITY = new MaterialTrait(
            "velocity", "迅捷：箭矢飞行更快", ModEnchantments.VELOCITY);
    public static final MaterialTrait EXPLOSIVE = new MaterialTrait(
            "explosive", "爆裂：箭矢命中爆炸，等级越高范围越大", ModEnchantments.EXPLOSIVE);
    public static final MaterialTrait INCINERATE = new MaterialTrait(
            "incinerate", "灼燃：箭矢命中点燃目标", ModEnchantments.INCINERATE);

    /** 战斗组 5 特性（剑 / 重锤 / 三叉戟 / 权杖） */
    public static final List<MaterialTrait> COMBAT_TRAITS = List.of(
            BLAZING, VENOM, SIPHON, THUNDER, FROST);
    /** 挖掘组 5 特性（镐） */
    public static final List<MaterialTrait> PICKAXE_TRAITS = List.of(
            REFINE, RADIANCE, MAGNETISM, PROSPECT, HASTE);
    /** 斧组 10 特性（斧）：战斗 5（伤害）+ 挖掘 5（效率），兼具伤害与效率 */
    public static final List<MaterialTrait> AXE_TRAITS = List.of(
            BLAZING, VENOM, SIPHON, THUNDER, FROST,
            REFINE, RADIANCE, MAGNETISM, PROSPECT, HASTE);
    /** 弓组 5 特性 */
    public static final List<MaterialTrait> BOW_TRAITS = List.of(
            PIERCE, PRECISION, VELOCITY, EXPLOSIVE, INCINERATE);

    private static final List<MaterialTrait> ALL = List.of(
            BLAZING, VENOM, SIPHON, THUNDER, FROST,
            REFINE, RADIANCE, MAGNETISM, PROSPECT, HASTE,
            PIERCE, PRECISION, VELOCITY, EXPLOSIVE, INCINERATE);

    private ModTraits() {
    }

    public static void register() {
        // 特性以静态常量形式存在；完整注册表已就绪
    }

    public static List<MaterialTrait> all() {
        return ALL;
    }

    /** 取某工具类别对应的候选特性列表（COMBAT=5，AXE=10，PICKAXE=5，BOW=5） */
    public static List<MaterialTrait> traitsFor(ToolCategory cat) {
        return switch (cat) {
            case COMBAT -> COMBAT_TRAITS;
            case AXE -> AXE_TRAITS;
            case PICKAXE -> PICKAXE_TRAITS;
            case BOW -> BOW_TRAITS;
            default -> List.of();
        };
    }
}
