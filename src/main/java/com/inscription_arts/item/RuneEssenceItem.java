package com.inscription_arts.item;

import com.inscription_arts.api.MaterialTrait;
import com.inscription_arts.component.ModDataComponents;
import com.inscription_arts.registry.ModRunes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 符文精华物品。从材料在萃取台中萃取而出，携带 {@link ModDataComponents#RUNE_ESSENCE} 组件记录其符文 id。
 * 可铭刻到装备的对应槽位（前缀 / 核心 / 后缀）。
 */
public class RuneEssenceItem extends Item {

    public static final RuneEssenceItem INSTANCE = new RuneEssenceItem(new Properties().stacksTo(16));

    public RuneEssenceItem(Properties props) {
        super(props);
    }

    /** 生成一枚携带指定符文 id 的精华 */
    public static ItemStack create(String runeId) {
        ItemStack stack = new ItemStack(INSTANCE);
        stack.set(ModDataComponents.RUNE_ESSENCE, runeId);
        return stack;
    }

    /** 读取精华上的符文 id（无则 null） */
    public static String getRuneId(ItemStack stack) {
        return stack.get(ModDataComponents.RUNE_ESSENCE);
    }

    @Override
    public Component getName(ItemStack stack) {
        String id = stack.get(ModDataComponents.RUNE_ESSENCE);
        if (id != null) {
            return ModRunes.getById(id)
                    .map(r -> (Component) Component.literal("符文精华 · ").append(r.displayName()))
                    .orElseGet(() -> super.getName(stack));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                List<Component> lines, TooltipFlag flag) {
        String id = stack.get(ModDataComponents.RUNE_ESSENCE);
        if (id != null) {
            ModRunes.getById(id).ifPresent(r -> {
                lines.add(r.displayName().copy().withStyle(ChatFormatting.AQUA));
                lines.add(Component.literal("充能倍率: ×" + r.charge()).withStyle(ChatFormatting.YELLOW));
                List<MaterialTrait> shown = r.allTraits().stream().distinct().limit(3).toList();
                for (MaterialTrait t : shown) {
                    lines.add(Component.literal("· " + t.description()).withStyle(ChatFormatting.GRAY));
                }
                if (r.allTraits().stream().distinct().count() > 3) {
                    lines.add(Component.literal("· 以及更多...").withStyle(ChatFormatting.DARK_GRAY));
                }
            });
        }
    }
}
