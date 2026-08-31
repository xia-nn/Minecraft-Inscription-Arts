package com.inscription_arts.registry;

import com.inscription_arts.InscriptionArts;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * 自创附魔的键与解析工具。
 * <p>
 * 1.21 起 {@link Enchantment} 属于「数据驱动的动态注册表」（{@link Registries#ENCHANTMENT}），
 * 无法像方块/物品那样在 {@code onInitialize} 期间写入 {@code BuiltInRegistries}。
 * 因此附魔本体改用数据包 JSON 定义（{@code data/inscription_arts/enchantment/*.json}），
 * 本类只保留 {@link ResourceKey} 常量与「按世界注册表解析 {@link Holder}」的工具方法。
 * <p>
 * 真正的效果（烈焰点燃 / 淬毒 / 汲取回血）由 {@link com.inscription_arts.effect.RuneEffectApplier}
 * 在攻击事件中读取附魔等级来施加，无需依赖 1.21 复杂的 effect-component 体系。
 */
public final class ModEnchantments {

    public static final ResourceKey<Enchantment> BLAZING = key("blazing");
    public static final ResourceKey<Enchantment> VENOM = key("venom");
    public static final ResourceKey<Enchantment> SIPHON = key("siphon");

    // —— 战斗组（剑 / 斧 / 重锤 / 三叉戟 / 权杖）——
    public static final ResourceKey<Enchantment> THUNDER = key("thunder");   // 雷霆：攻击雷击
    public static final ResourceKey<Enchantment> FROST = key("frost");       // 寒霜：攻击减速冻结

    // —— 挖掘组（镐）——
    public static final ResourceKey<Enchantment> REFINE = key("refine");       // 精炼：自动熔炼
    public static final ResourceKey<Enchantment> RADIANCE = key("radiance");   // 辉光：持有时照明/夜视
    public static final ResourceKey<Enchantment> MAGNETISM = key("magnetism"); // 磁力：吸附掉落物
    public static final ResourceKey<Enchantment> PROSPECT = key("prospect");   // 勘探：额外矿石掉落
    public static final ResourceKey<Enchantment> HASTE = key("haste");         // 疾速：挖掘提速

    // —— 弓组 ——
    public static final ResourceKey<Enchantment> PIERCE = key("pierce");         // 贯穿：箭矢穿透
    public static final ResourceKey<Enchantment> PRECISION = key("precision");   // 精准：箭矢增伤
    public static final ResourceKey<Enchantment> VELOCITY = key("velocity");     // 迅捷：箭矢加速
    public static final ResourceKey<Enchantment> EXPLOSIVE = key("explosive");   // 爆裂：命中爆炸
    public static final ResourceKey<Enchantment> INCINERATE = key("incinerate"); // 灼燃：命中点燃

    private ModEnchantments() {
    }

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, name));
    }

    /** 从世界注册表访问器解析附魔 {@link Holder}（附魔为动态注册表，必须在游戏运行时取用） */
    public static Holder<Enchantment> holder(RegistryAccess access, ResourceKey<Enchantment> key) {
        return access.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }

    /** 从附魔注册表解析 {@link Holder} */
    public static Holder<Enchantment> holder(Registry<Enchantment> registry, ResourceKey<Enchantment> key) {
        return registry.getHolderOrThrow(key);
    }
}
