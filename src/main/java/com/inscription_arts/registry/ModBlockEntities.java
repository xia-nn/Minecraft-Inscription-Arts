package com.inscription_arts.registry;

import com.inscription_arts.InscriptionArts;
import com.inscription_arts.block.RuneAltarCore;
import com.inscription_arts.block.entity.RuneAltarCoreBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 方块实体注册表。目前注册符文祭坛核心的方块实体。
 */
public final class ModBlockEntities {

    public static final BlockEntityType<RuneAltarCoreBlockEntity> RUNE_ALTAR_CORE =
            BlockEntityType.Builder.of(RuneAltarCoreBlockEntity::new, ModBlocks.RUNE_ALTAR_CORE)
                    .build(null);

    private ModBlockEntities() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "rune_altar_core"),
                RUNE_ALTAR_CORE);
    }
}
