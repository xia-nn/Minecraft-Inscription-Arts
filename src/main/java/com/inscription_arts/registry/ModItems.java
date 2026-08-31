package com.inscription_arts.registry;

import com.inscription_arts.InscriptionArts;
import com.inscription_arts.item.InscriptionGuide;
import com.inscription_arts.item.RuneEssenceItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * 物品注册表。注册 {@link RuneEssenceItem}（符文精华）与 {@link InscriptionGuide}（铭刻手册）。
 */
public final class ModItems {

    public static final RuneEssenceItem RUNE_ESSENCE = RuneEssenceItem.INSTANCE;
    public static final InscriptionGuide GUIDE = InscriptionGuide.INSTANCE;

    private ModItems() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "rune_essence"),
                RUNE_ESSENCE);
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "guide"),
                GUIDE);
    }
}
