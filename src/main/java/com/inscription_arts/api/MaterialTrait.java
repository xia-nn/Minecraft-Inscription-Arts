package com.inscription_arts.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * 材料特性。每种用于萃取的材料都会携带若干特性，这些特性会随符文精华一起铭刻到装备上。
 * <p>
 * 阶段 2 重构：特性不再只是「描述」，而是直接绑定一个自创附魔 {@link ResourceKey}。
 * 铭刻时 {@code InscriptionApi} 按装备的工具类别取出对应特性列表，为每个特性附魔累加等级。
 * 真正的游戏效果由 {@code RuneEffectApplier} 读取附魔等级来施加。
 *
 * @param id          特性的唯一标识（小写蛇形，如 {@code blazing}）
 * @param description 给玩家看的特性说明
 * @param enchantment 该特性对应的自创附魔键（铭刻时写入装备的附魔）
 */
public record MaterialTrait(String id, String description, ResourceKey<Enchantment> enchantment) {
    public MaterialTrait {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("MaterialTrait id 不能为空");
        }
        if (description == null) {
            throw new IllegalArgumentException("MaterialTrait description 不能为 null");
        }
        if (enchantment == null) {
            throw new IllegalArgumentException("MaterialTrait enchantment 不能为 null");
        }
    }
}
