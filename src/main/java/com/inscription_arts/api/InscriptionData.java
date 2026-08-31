package com.inscription_arts.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 铭刻数据。承载在某件装备物品上（通过 {@code DataComponentType} 持久化），
 * 记录该装备<b>曾经铭刻过的全部符文</b>（按铭刻顺序），用于萃取台修复时
 * 反查「对应材料」（取稀有度最高、即 {@code charge} 最大者所来源的材料）。
 *
 * @param runes 曾铭刻的符文注册表 id 列表（如 {@code inscription_arts:diamond}）
 */
public record InscriptionData(List<ResourceLocation> runes) {

    /** 序列化为 JSON 用的 MapCodec（键名 {@code runes}，值为符文 id 字符串数组） */
    public static final MapCodec<InscriptionData> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ResourceLocation.CODEC.listOf().fieldOf("runes").forGetter(InscriptionData::runes)
    ).apply(inst, InscriptionData::new));

    public InscriptionData {
        runes = List.copyOf(new ArrayList<>(runes));
    }

    /** 是否曾铭刻过任意符文 */
    public boolean isEmpty() {
        return runes.isEmpty();
    }
}
