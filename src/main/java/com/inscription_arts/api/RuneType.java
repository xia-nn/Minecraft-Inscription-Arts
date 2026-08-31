package com.inscription_arts.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 符文类型。从材料中萃取出的「符文精华」对应一种 {@link RuneType}，
 * 铭刻到装备的指定槽位后即可按装备的工具类别获得对应的材料特性（自创附魔）。
 * <p>
 * 与匠魂「强化槽」对应：效果固定、可预期，不存在附魔台随机 roll。
 *
 * @param id                 符文唯一标识（小写蛇形，如 {@code iron_edge}）
 * @param slot               该符文可铭刻到的槽位（前缀 / 核心 / 后缀，单槽祭坛下仅作记号，不再区分）
 * @param displayName        给玩家看的名称
 * @param traitsByCategory   该符文按工具类别携带的特性列表（每类一组候选自创附魔）
 * @param effect             铭刻后于游戏中触发的基础效果（占位，真正效果由 RuneEffectApplier 读取附魔等级施加）
 * @param charge             稀有度充能：每次铭刻贡献的「煤等价充能」倍数（越稀有越大），
 *                           普通=1 / 少见=4 / 稀有=10 / 极稀有=25
 */
public record RuneType(String id, RuneSlot slot, Component displayName,
                       Map<ToolCategory, List<MaterialTrait>> traitsByCategory,
                       Consumer<ItemStack> effect, float charge) {
    public RuneType {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("RuneType id 不能为空");
        }
        if (slot == null) {
            throw new IllegalArgumentException("RuneType slot 不能为 null");
        }
        if (displayName == null) {
            throw new IllegalArgumentException("RuneType displayName 不能为 null");
        }
        if (effect == null) {
            throw new IllegalArgumentException("RuneType effect 不能为 null");
        }
        if (charge <= 0f) {
            throw new IllegalArgumentException("RuneType charge 必须 > 0");
        }
        Map<ToolCategory, List<MaterialTrait>> copy = new EnumMap<>(ToolCategory.class);
        for (Map.Entry<ToolCategory, List<MaterialTrait>> e : traitsByCategory.entrySet()) {
            copy.put(e.getKey(), List.copyOf(e.getValue()));
        }
        traitsByCategory = Map.copyOf(copy);
    }

    /** 取某工具类别对应的特性列表（无则空） */
    public List<MaterialTrait> traitsFor(ToolCategory cat) {
        return traitsByCategory.getOrDefault(cat, List.of());
    }

    /** 全部类别的全部特性（去重前的合并列表），供精华悬停文本展示 */
    public List<MaterialTrait> allTraits() {
        List<MaterialTrait> all = new ArrayList<>();
        for (List<MaterialTrait> l : traitsByCategory.values()) {
            all.addAll(l);
        }
        return all;
    }
}
