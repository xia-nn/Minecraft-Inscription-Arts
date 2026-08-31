package com.inscription_arts.component;

import com.inscription_arts.api.InscriptionData;
import com.inscription_arts.api.InscriptionProgress;
import com.inscription_arts.InscriptionArts;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponentType;

/**
 * 自定义数据组件注册表。
 * <p>
 * {@link #INSCRIPTION} 持久化 {@link InscriptionData}（装备上的铭刻数据）；
 * {@link #RUNE_ESSENCE} 记录某枚符文精华对应的符文 id，挂载在 {@code RuneEssenceItem} 上。
 * 两者均使用 {@code persistent}，既写入存档也会随物品栈同步到客户端（用于显示名称/提示）。
 */
public final class ModDataComponents {

    public static final DataComponentType<InscriptionData> INSCRIPTION =
            DataComponentType.<InscriptionData>builder()
                    .persistent(InscriptionData.CODEC.codec())
                    .build();

    public static final DataComponentType<String> RUNE_ESSENCE =
            DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .build();

    /** 分数级铭刻进度：记录每个附魔的分数余数，配合整数附魔等级实现跨次累加 */
    public static final DataComponentType<InscriptionProgress> INSCRIPTION_PROGRESS =
            DataComponentType.<InscriptionProgress>builder()
                    .persistent(InscriptionProgress.CODEC.codec())
                    .build();

    private ModDataComponents() {
    }

    public static void register() {
        Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "inscription"),
                INSCRIPTION);
        Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "rune_essence"),
                RUNE_ESSENCE);
        Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "inscription_progress"),
                INSCRIPTION_PROGRESS);
    }
}
