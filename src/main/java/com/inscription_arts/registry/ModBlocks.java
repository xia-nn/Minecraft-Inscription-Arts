package com.inscription_arts.registry;

import com.inscription_arts.InscriptionArts;
import com.inscription_arts.block.ExtractorBlock;
import com.inscription_arts.block.RuneAltarCore;
import com.inscription_arts.block.RuneOreBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.HashMap;
import java.util.Map;

/**
 * 方块注册表。注册萃取台、符文祭坛核心，以及 8 种符文矿石（每种对应一种符文，地下生成）。
 * 为每个方块自动注册对应的 {@link BlockItem}。
 */
public final class ModBlocks {

    public static final Block EXTRACTOR = new ExtractorBlock(
            BlockBehaviour.Properties.of().strength(2.0f).requiresCorrectToolForDrops());
    public static final Block RUNE_ALTAR_CORE = new RuneAltarCore(
            BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops());

    /** 符文 id → 对应矿石方块（供创造栏 / 世界生成引用） */
    public static final Map<String, Block> RUNE_ORES = new HashMap<>();

    private ModBlocks() {
    }

    public static void register() {
        registerBlock("extractor_block", EXTRACTOR);
        registerBlock("rune_altar_core", RUNE_ALTAR_CORE);
        // 8 种符文矿石：按稀有度/槽位顺序，每种对应一个符文 id
        for (var rune : ModRunes.all()) {
            Block ore = new RuneOreBlock(rune.id(), BlockBehaviour.Properties.of());
            RUNE_ORES.put(rune.id(), ore);
            registerBlock("rune_ore_" + rune.id(), ore);
        }
    }

    private static void registerBlock(String name, Block block) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, name);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
    }
}
