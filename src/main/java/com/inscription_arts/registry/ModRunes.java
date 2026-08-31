package com.inscription_arts.registry;

import com.inscription_arts.api.MaterialTrait;
import com.inscription_arts.api.RuneSlot;
import com.inscription_arts.api.RuneType;
import com.inscription_arts.api.ToolCategory;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 符文注册表。
 * <p>
 * 每个符文对应一种从材料萃取出的「符文精华」，铭刻到装备后，会按装备的工具类别
 * 给出一组候选自创附魔，玩家三选一，把该符文的<b>充能</b>累加到所选附魔上。
 * <p>
 * 稀有度 → 充能倍率（{@code charge} 即每次铭刻贡献的「煤等价充能」）：
 * <ul>
 *   <li>普通（煤/铜/铁/纸）：×1 —— 升 1 级需要累计 3 点充能（一次「3 符文」铭刻的最低充能，≈3 块煤）；</li>
 *   <li>少见（金）：×4；</li>
 *   <li>稀有（钻石/绿宝石）：×10（低级附魔 1 次铭刻即可刷 2 级）；</li>
 *   <li>极稀有（下界合金）：×25 —— 最高级符文，单次充能最多。</li>
 * </ul>
 * 越高级的符文每次充能越大；升级到 N+1 级所需充能随等级按二次曲线激增（见 {@link ModConfig#levelUpCost}）。
 */
public final class ModRunes {

    /** 构建「四类工具各一组候选特性」的映射（共用 ModTraits 的四组常量） */
    private static Map<ToolCategory, List<MaterialTrait>> combatPickBow() {
        Map<ToolCategory, List<MaterialTrait>> m = new EnumMap<>(ToolCategory.class);
        m.put(ToolCategory.COMBAT, ModTraits.COMBAT_TRAITS);
        m.put(ToolCategory.AXE, ModTraits.AXE_TRAITS);
        m.put(ToolCategory.PICKAXE, ModTraits.PICKAXE_TRAITS);
        m.put(ToolCategory.BOW, ModTraits.BOW_TRAITS);
        return m;
    }

    // 普通 tier：充能 ×1
    public static final RuneType COAL = new RuneType(
            "coal", RuneSlot.SUFFIX, Component.translatable("rune.inscription_arts.coal"),
            combatPickBow(), stack -> {}, 1.0f);
    public static final RuneType COPPER = new RuneType(
            "copper", RuneSlot.PREFIX, Component.translatable("rune.inscription_arts.copper"),
            combatPickBow(), stack -> {}, 1.0f);
    public static final RuneType IRON_EDGE = new RuneType(
            "iron_edge", RuneSlot.PREFIX, Component.translatable("rune.inscription_arts.iron_edge"),
            combatPickBow(), stack -> {}, 1.0f);
    public static final RuneType PAPER_SLOT = new RuneType(
            "paper_slot", RuneSlot.SUFFIX, Component.translatable("rune.inscription_arts.paper_slot"),
            combatPickBow(), stack -> {}, 1.0f);

    // 少见 tier：充能 ×4
    public static final RuneType GOLD_LUCK = new RuneType(
            "gold_luck", RuneSlot.CORE, Component.translatable("rune.inscription_arts.gold_luck"),
            combatPickBow(), stack -> {}, 4.0f);

    // 稀有 tier：充能 ×10
    public static final RuneType DIAMOND = new RuneType(
            "diamond", RuneSlot.CORE, Component.translatable("rune.inscription_arts.diamond"),
            combatPickBow(), stack -> {}, 10.0f);
    public static final RuneType EMERALD = new RuneType(
            "emerald", RuneSlot.SUFFIX, Component.translatable("rune.inscription_arts.emerald"),
            combatPickBow(), stack -> {}, 10.0f);

    // 极稀有 tier：充能 ×25（最高级符文）
    public static final RuneType NETHERITE = new RuneType(
            "netherite", RuneSlot.PREFIX, Component.translatable("rune.inscription_arts.netherite"),
            combatPickBow(), stack -> {}, 25.0f);

    private static final List<RuneType> ALL = List.of(
            COAL, COPPER, IRON_EDGE, PAPER_SLOT, GOLD_LUCK, DIAMOND, EMERALD, NETHERITE);

    /** 符文 id → 符文对象 */
    private static final Map<String, RuneType> BY_ID = ALL.stream()
            .collect(Collectors.toMap(RuneType::id, r -> r));

    /** 可萃取材料 → 对应符文（萃取台据此把材料转为精华） */
    private static final Map<Item, RuneType> EXTRACT_MAP = Map.of(
            Items.COAL, COAL,
            Items.COPPER_INGOT, COPPER,
            Items.IRON_INGOT, IRON_EDGE,
            Items.GOLD_INGOT, GOLD_LUCK,
            Items.PAPER, PAPER_SLOT,
            Items.DIAMOND, DIAMOND,
            Items.EMERALD, EMERALD,
            Items.NETHERITE_INGOT, NETHERITE);

    private ModRunes() {
    }

    public static void register() {
        // 符文以静态常量形式存在；完整注册表与萃取逻辑已就绪
    }

    public static List<RuneType> all() {
        return ALL;
    }

    public static Optional<RuneType> getById(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** 该物品是否可作为萃取材料；若是返回对应符文 */
    public static Optional<RuneType> getByItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(EXTRACT_MAP.get(stack.getItem()));
    }

    /** 萃取台支持的全部材料 */
    public static List<Item> extractMaterials() {
        return List.copyOf(EXTRACT_MAP.keySet());
    }

    /** 符文 id → 其萃取来源材料（供修复逻辑「对应材料」反查） */
    private static final Map<String, Item> RUNE_TO_MATERIAL = EXTRACT_MAP.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getValue().id(), Map.Entry::getKey));

    /** 取某符文对应的萃取材料（无则空） */
    public static Optional<Item> materialOfRune(String runeId) {
        return Optional.ofNullable(RUNE_TO_MATERIAL.get(runeId));
    }
}
