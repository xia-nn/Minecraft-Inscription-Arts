package com.inscription_arts.api;

import com.inscription_arts.InscriptionArts;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/**
 * 工具类别。用于把「符文精华」按装备类型施加到不同的自创附魔组：
 * <ul>
 *   <li>{@code COMBAT}：剑 / 重锤 / 三叉戟 / 权杖 —— 共享 5 个战斗附魔；</li>
 *   <li>{@code AXE}：符文之斧 —— 兼具伤害与效率，候选池 = 战斗 5 + 挖掘 5 = 10 个附魔；</li>
 *   <li>{@code PICKAXE}：镐 —— 独占 5 个挖掘附魔；</li>
 *   <li>{@code BOW}：弓 —— 独占 5 个弓弩附魔；</li>
 *   <li>{@code OTHER}：其余物品（盔甲、杂项），本次扩展不施加自创附魔。</li>
 * </ul>
 * 注意：普通 {@link AxeItem}（木斧/铁斧等）按原版分类仍走 {@code COMBAT} 仅享战斗附魔；
 * 只有本模组的「符文之斧」({@code inscription_arts:axe}) 因兼具效率，在此特判为 {@code AXE}，
 * 候选池扩展为战斗+挖掘共 10 个附魔。符文之锤（{@code inscription_arts:hammer}）虽继承自
 * {@link PickaxeItem}，但定位为「重武器」，归为 {@code COMBAT}。
 */
public enum ToolCategory {
    COMBAT,
    AXE,
    PICKAXE,
    BOW,
    OTHER;

    public static ToolCategory of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return OTHER;
        }
        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        // 符文之锤特判：虽是 PickaxeItem，但作为重武器走战斗附魔组
        if (id.getNamespace().equals(InscriptionArts.MOD_ID) && id.getPath().equals("hammer")) {
            return COMBAT;
        }
        if (item instanceof BowItem) {
            return BOW;
        }
        // 斧头：兼具伤害与效率，候选池 = 战斗 5 + 挖掘 5（覆盖原版斧与符文之斧）
        if (item instanceof AxeItem) {
            return AXE;
        }
        if (item instanceof SwordItem || item instanceof MaceItem || item instanceof TridentItem) {
            return COMBAT;
        }
        if (item instanceof PickaxeItem) {
            return PICKAXE;
        }
        return OTHER;
    }
}
