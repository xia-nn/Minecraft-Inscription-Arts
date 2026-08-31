package com.inscription_arts.world;

import com.inscription_arts.InscriptionArts;
import com.inscription_arts.registry.ModRunes;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;

/**
 * 符文矿石世界生成注册。
 * <p>
 * 采用 Fabric 官方 {@link BiomeModifications#addFeature} Java API 将每种符文对应的
 * placed_feature（数据驱动，定义于 {@code data/.../worldgen/placed_feature/rune_ore_*.json}）
 * 加入主世界所有生物群系的 {@code UNDERGROUND_ORES} 生成阶段。
 * <p>
 * 注意：早期尝试的 {@code fabric:add_features} 数据包 biome_modifier 在本版 fabric-api
 * 下不会生效（无报错但矿石实际不生成），因此改用此 Java API，行为稳定且会被 Fabric
 * 的 biome 修改日志统计。
 */
public final class ModWorldGen {

    private ModWorldGen() {
    }

    public static void register() {
        int count = 0;
        for (var rune : ModRunes.all()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    InscriptionArts.MOD_ID, "rune_ore_" + rune.id());
            var key = ResourceKey.create(Registries.PLACED_FEATURE, id);
            BiomeModifications.addFeature(
                    BiomeSelectors.foundInOverworld(),
                    GenerationStep.Decoration.UNDERGROUND_ORES,
                    key);
            count++;
        }
        InscriptionArts.LOGGER.info("已注册 {} 种符文矿石的世界生成（主世界地下）", count);
    }
}
