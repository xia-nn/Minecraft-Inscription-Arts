package com.inscription_arts.balance;

/**
 * 数值平衡中心。
 * <p>
 * 模组所有「可调数值」集中在此，便于联机服或后续平衡调整，无需改动逻辑代码。
 * 阶段 4 引入，原先散落在 {@code ModEnchantments} / {@code RuneEffectApplier} /
 * {@code RuneAltarScreenHandler} / {@code ExtractorBlock} 中的魔法数字统一改用本类引用。
 */
public final class ModConfig {

    private ModConfig() {
    }

    /** 附魔等级上限（突破原版上限，原版罗马数字显示支持到 X / 10 级） */
    public static final int MAX_LEVEL = 10;

    /**
     * 升级到 {@code currentLevel + 1} 级所需的「煤等价充能」总量。
     * <p>
     * 二次曲线 {@code floor(6.29·N² + 64·N − 65)}，N 为当前等级：
     * <ul>
     *   <li>N=0（从零到第 1 级）：3（一次「3 符文」铭刻的最低充能，即 3 块煤符文即可升 1 级）；</li>
     *   <li>N=6（6→7 级）：545；</li>
     *   <li>N=9（9→10 级）：1020。</li>
     * </ul>
     * 等级越高所需充能激增，越往上的附魔升级越难；配合稀有度充能倍率
     * （普通 ×1 / 少见 ×4 / 稀有 ×10 / 极稀有 ×25），低级符文需反复铭刻，高级符文事半功倍。
     * <p>
     * 注：当前为「3 符文 / 次」设计，故第 1 级门槛设为 3，使最低配的一次铭刻（3×煤，充能 3）直接升 1 级、
     * 点下去立刻有反馈；更高等级（545 / 1020）沿用原二次曲线，未改动。
     */
    public static int levelUpCost(int currentLevel) {
        if (currentLevel <= 0) {
            return 3;
        }
        double cost = 6.29 * currentLevel * currentLevel + 64.0 * currentLevel - 65.0;
        return (int) Math.floor(cost);
    }

    /** 用材料修复时每点「材料强度」恢复的耐久值；实际恢复 = REPAIR_AMOUNT * 材料强度 */
    public static final int REPAIR_AMOUNT = 25;

    /** 烈焰附魔每级使目标燃烧的秒数 */
    public static final int BLAZING_SECONDS_PER_LEVEL = 2;

    /** 淬毒附魔每级持续的刻数（20 刻 = 1 秒），毒的等级 = 附魔等级 - 1 */
    public static final int VENOM_TICKS_PER_LEVEL = 60;

    /** 汲取附魔每级回复的生命值（单位：半颗心，即 1 = 0.5 心） */
    public static final float SIPHON_HEAL_PER_LEVEL = 2.0f;

    // —— 雷霆（thunder）——
    /** 每级雷击触发概率（叠加，封顶 {@link #THUNDER_MAX_CHANCE}） */
    public static final float THUNDER_CHANCE_PER_LEVEL = 0.08f;
    /** 雷击概率上限 */
    public static final float THUNDER_MAX_CHANCE = 0.8f;
    /** 雷击每级附加伤害（半颗心） */
    public static final float THUNDER_DAMAGE_PER_LEVEL = 2.0f;

    // —— 寒霜（frost）——
    /** 每级减速持续刻数（20 刻 = 1 秒） */
    public static final int FROST_TICKS_PER_LEVEL = 40;
    /** 减速等级 = 附魔等级 - 1（上限 {@link #FROST_MAX_AMPLIFIER}） */
    public static final int FROST_MAX_AMPLIFIER = 4;

    // —— 精炼（refine）——
    /** 精炼：破坏方块时自动熔炼（无需额外数值，保留开关） */
    public static final boolean REFINE_ENABLED = true;

    // —— 辉光（radiance）——
    /** 辉光：持有时给予的夜视时长（刻），每 tick 刷新以持续生效 */
    public static final int RADIANCE_NIGHT_VISION_TICKS = 220;

    // —— 磁力（magnetism）——
    /** 磁力基础吸附半径（方块） */
    public static final double MAGNETISM_BASE_RADIUS = 3.0;
    /** 每级额外吸附半径 */
    public static final double MAGNETISM_RADIUS_PER_LEVEL = 0.5;
    /** 磁力每 tick 把掉落物拉向玩家的速度 */
    public static final double MAGNETISM_PULL_SPEED = 0.35;

    // —— 勘探（prospect）——
    /** 勘探：每级触发额外矿石掉落的概率 */
    public static final float PROSPECT_CHANCE_PER_LEVEL = 0.18f;
    /** 勘探单次最多额外掉落的矿石数量 */
    public static final int PROSPECT_MAX_EXTRA = 3;

    // —— 疾速（haste）——
    /** 疾速：每级额外挖掘速度倍率（加法乘法，0.2 = +20%） */
    public static final double HASTE_SPEED_PER_LEVEL = 0.2;

    // —— 贯穿（pierce）——
    /** 贯穿：每级箭矢可额外穿透的实体数（封顶 127） */
    public static final int PIERCE_PER_LEVEL = 1;

    // —— 精准（precision）——
    /** 精准：每级箭矢附加伤害（半颗心） */
    public static final float PRECISION_DMG_PER_LEVEL = 1.5f;

    // —— 迅捷（velocity）——
    /** 迅捷：每级箭矢飞行速度倍率（1 + 0.15*level） */
    public static final double VELOCITY_PER_LEVEL = 0.15;

    // —— 爆裂（explosive）——
    /** 爆裂：每级爆炸半径（方块） */
    public static final float EXPLOSIVE_RADIUS_PER_LEVEL = 0.4f;
    /** 爆裂爆炸半径上限 */
    public static final float EXPLOSIVE_MAX_RADIUS = 3.0f;

    // —— 灼燃（incinerate）——
    /** 灼燃：每级使目标燃烧的秒数 */
    public static final int INCINERATE_SECONDS_PER_LEVEL = 2;
}
