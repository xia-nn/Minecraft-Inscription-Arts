package com.inscription_arts.registry;

import com.inscription_arts.InscriptionArts;
import com.inscription_arts.item.ModTools;
import com.inscription_arts.item.RuneEssenceItem;
import com.inscription_arts.registry.ModRunes;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * 创造模式物品栏（Creative Tab）注册。1.21 起物品/方块必须显式加入某个标签页才会在创造模式中出现。
 * 这里为模组单独注册一个名为「铭刻之艺」的标签页，并把本模组的所有物品与方块放进去。
 */
public final class ModItemGroups {

    public static final ResourceKey<CreativeModeTab> INSCRIPTION_ARTS = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "inscription_arts"));

    private ModItemGroups() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, INSCRIPTION_ARTS,
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(ModItems.RUNE_ESSENCE))
                        .title(Component.translatable("itemGroup.inscription_arts.inscription_arts"))
                        .build());

        // 向标签页添加本模组的物品与方块（方块用其对应的 BlockItem）
        ItemGroupEvents.modifyEntriesEvent(INSCRIPTION_ARTS).register(entries -> {
            entries.accept(ModBlocks.EXTRACTOR.asItem());
            entries.accept(ModBlocks.RUNE_ALTAR_CORE.asItem());
            // 8 种符文矿石：每种对应一个符文，地下生成，破坏掉对应精华
            for (var ore : ModBlocks.RUNE_ORES.values()) {
                entries.accept(ore.asItem());
            }
            // 8 种符文精华：按稀有度/槽位顺序（普通→少见→稀有→极稀有）各占一张卡片，
            // 用 RuneEssenceItem.create 生成携带对应符文 id 的堆叠，方便创造模式直接取用具体精华。
            for (var rune : ModRunes.all()) {
                entries.accept(RuneEssenceItem.create(rune.id()));
            }
            entries.accept(ModItems.GUIDE);
            entries.accept(ModTools.HAMMER);
            entries.accept(ModTools.EXCAVATOR);
            entries.accept(ModTools.AXE);
            entries.accept(ModTools.HOE);
            entries.accept(ModTools.SWORD);
        });
    }
}
