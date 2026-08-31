package com.inscription_arts.api;

import com.inscription_arts.InscriptionArts;
import com.inscription_arts.balance.ModConfig;
import com.inscription_arts.component.ModDataComponents;
import com.inscription_arts.registry.ModEnchantments;
import com.inscription_arts.registry.ModRunes;
import com.inscription_arts.registry.ModTraits;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * 铭刻之艺公开 API。附属模组可通过本类：
 * <ul>
 *   <li>{@link #registerRune(RuneType)} / {@link #registerTrait(MaterialTrait)} / {@link #registerMaterial(Item, RuneType)}
 *       注册自定义符文、材料特性与可萃取材料；</li>
 *   <li>{@link #registeredRunes()} / {@link #registeredTraits()} 拿到内置 + 自定义的全部定义；</li>
 *   <li>{@link #offerCandidates(ItemStack, ToolCategory, RegistryAccess, RandomSource)} 为某装备生成三选一候选；</li>
 *   <li>{@link #applyCharge(ItemStack, ResourceKey, float, RegistryAccess)} 在权威服务端把一次铭刻的充能累加到所选附魔。</li>
 * </ul>
 * 注册应在 {@code onInitialize} 中、早于任何游戏内使用时调用。
 */
public final class InscriptionApi {

    private InscriptionApi() {
    }

    private static final List<RuneType> EXTRA_RUNES = new ArrayList<>();
    private static final List<MaterialTrait> EXTRA_TRAITS = new ArrayList<>();
    /** 外部可萃取材料 → 符文（供萃取台与修复反查） */
    private static final Map<Item, RuneType> EXTRA_MATERIALS = new java.util.LinkedHashMap<>();

    /** 疾速（haste）附魔对应的挖掘速度属性修改器 id，便于覆盖旧值而非叠加 */
    private static final ResourceLocation HASTE_ID =
            ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "haste");

    /** 注册自定义符文（id 不得与内置重复） */
    public static void registerRune(RuneType rune) {
        if (ModRunes.getById(rune.id()).isPresent()) {
            throw new IllegalArgumentException("符文 id 已存在: " + rune.id());
        }
        EXTRA_RUNES.add(rune);
    }

    /** 注册自定义材料特性 */
    public static void registerTrait(MaterialTrait trait) {
        EXTRA_TRAITS.add(trait);
    }

    /** 注册自定义可萃取材料（右键萃取台即可产出对应精华） */
    public static void registerMaterial(Item material, RuneType rune) {
        EXTRA_MATERIALS.put(material, rune);
    }

    /** 全部符文（内置 + 自定义） */
    public static List<RuneType> registeredRunes() {
        List<RuneType> all = new ArrayList<>(ModRunes.all());
        all.addAll(EXTRA_RUNES);
        return all;
    }

    /** 全部材料特性（内置 + 自定义） */
    public static List<MaterialTrait> registeredTraits() {
        List<MaterialTrait> all = new ArrayList<>(ModTraits.all());
        all.addAll(EXTRA_TRAITS);
        return all;
    }

    /** 按 id 取符文（内置 + 自定义） */
    public static Optional<RuneType> getRuneById(String id) {
        Optional<RuneType> builtin = ModRunes.getById(id);
        if (builtin.isPresent()) {
            return builtin;
        }
        return EXTRA_RUNES.stream().filter(r -> r.id().equals(id)).findFirst();
    }

    /** 按物品取可萃取符文（内置 + 自定义） */
    public static Optional<RuneType> getByItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        RuneType rt = EXTRA_MATERIALS.get(stack.getItem());
        return Optional.ofNullable(rt);
    }

    /** 取某符文对应的萃取材料（内置 + 自定义） */
    public static Optional<Item> materialOfRune(String runeId) {
        Optional<Item> builtin = ModRunes.materialOfRune(runeId);
        if (builtin.isPresent()) {
            return builtin;
        }
        return EXTRA_MATERIALS.entrySet().stream()
                .filter(e -> e.getValue().id().equals(runeId))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /**
     * 为某装备生成三选一候选特性。从 {@link ModTraits#traitsFor(ToolCategory)} 的候选池中
     * 剔除已封顶（满 {@link ModConfig#MAX_LEVEL} 级）的附魔后随机抽最多 3 个；若全部满级或
     * 该类别无候选（如盔甲）则返回空列表。
     */
    public static List<MaterialTrait> offerCandidates(ItemStack equipment, ToolCategory cat,
                                                      RegistryAccess access, RandomSource random) {
        List<MaterialTrait> pool = new ArrayList<>(ModTraits.traitsFor(cat));
        if (pool.isEmpty()) {
            return List.of();
        }
        Registry<Enchantment> reg = access.registryOrThrow(Registries.ENCHANTMENT);
        List<MaterialTrait> available = new ArrayList<>();
        for (MaterialTrait t : pool) {
            int lvl = equipment.getEnchantments().getLevel(reg.getHolderOrThrow(t.enchantment()));
            if (lvl < ModConfig.MAX_LEVEL) {
                available.add(t);
            }
        }
        if (available.isEmpty()) {
            return List.of();
        }
        Collections.shuffle(available, new Random(random.nextLong()));
        int n = Math.min(3, available.size());
        return available.subList(0, n);
    }

    /**
     * 把一次铭刻的充能累加到指定附魔（权威服务端调用）。按 {@link ModConfig#levelUpCost(int)}
     * 的二次成本曲线把累计充能折算成整数等级提升（可一次跨多级），余数落盘到
     * {@code INSCRIPTION_PROGRESS} 组件，封顶 {@link ModConfig#MAX_LEVEL}。
     */
    public static void applyCharge(ItemStack stack, ResourceKey<Enchantment> key,
                                   float charge, RegistryAccess access) {
        Holder<Enchantment> holder = access.registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(key);
        applyCharge(stack, holder, charge);
    }

    /** 附魔充能「分数级」累加：按成本曲线提级、余数额落盘，封顶 {@link ModConfig#MAX_LEVEL} */
    private static void applyCharge(ItemStack stack, Holder<Enchantment> holder, float charge) {
        int currentLevel = stack.getEnchantments().getLevel(holder);
        Optional<ResourceKey<Enchantment>> keyOpt = holder.unwrapKey();
        String id = keyOpt.map(k -> k.location().toString()).orElse(null);
        if (id == null) {
            int next = Math.min(currentLevel + (int) Math.floor(charge), ModConfig.MAX_LEVEL);
            setLevel(stack, holder, next);
            return;
        }
        InscriptionProgress prog = stack.get(ModDataComponents.INSCRIPTION_PROGRESS);
        Map<String, Float> charges = new HashMap<>(prog == null ? Map.of() : prog.charges());
        float acc = charges.getOrDefault(id, 0f) + charge;
        int level = currentLevel;
        while (level < ModConfig.MAX_LEVEL && acc >= ModConfig.levelUpCost(level)) {
            acc -= ModConfig.levelUpCost(level);
            level++;
        }
        if (level >= ModConfig.MAX_LEVEL) {
            level = ModConfig.MAX_LEVEL;
            acc = 0f;
        }
        setLevel(stack, holder, level);
        // 关键修复：只要还有剩余充能就保存进度，让跨次铭刻能「积少成多」累加。
        // 旧逻辑用 level>0 作判断，导致未跨到 1 级时的充能被直接丢弃（点了属性却毫无变化）。
        if (acc > 0) {
            charges.put(id, acc);
        } else {
            charges.remove(id);
        }
        stack.set(ModDataComponents.INSCRIPTION_PROGRESS, new InscriptionProgress(charges));
        // 疾速需要同步维护「挖掘速度」属性（Minecraft 无挖掘速度附魔事件钩子）
        if (keyOpt.isPresent() && keyOpt.get().equals(ModEnchantments.HASTE)) {
            applyHasteAttribute(stack, level);
        }
    }

    /** 设置/移除装备上的整数附魔等级 */
    private static void setLevel(ItemStack stack, Holder<Enchantment> holder, int level) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        if (level > 0) {
            mutable.set(holder, level);
        } else {
            mutable.removeIf(h -> h.equals(holder));
        }
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    /** 维护装备上的「疾速」挖掘速度属性（按当前整数等级重建，避免重复叠加） */
    private static void applyHasteAttribute(ItemStack stack, int level) {
        ItemAttributeModifiers current = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry entry : current.modifiers()) {
            if (!HASTE_ID.equals(entry.modifier().id())) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        if (level > 0) {
            builder.add(Attributes.BLOCK_BREAK_SPEED,
                    new AttributeModifier(HASTE_ID,
                            ModConfig.HASTE_SPEED_PER_LEVEL * level, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }
}
