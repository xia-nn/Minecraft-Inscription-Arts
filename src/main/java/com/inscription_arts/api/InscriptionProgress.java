package com.inscription_arts.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

/**
 * 铭刻充能进度。
 * <p>
 * 由于升级到 N+1 级需要按 {@link com.inscription_arts.balance.ModConfig#levelUpCost(int)}
 * 累计「煤等价充能」，而单次铭刻的充能（由符文稀有度决定）往往不足 1 级，
 * 本组件记录每个附魔「距下一级还差多少充能」的<b>余数</b>，配合装备上的整数附魔等级，
 * 实现「积少成多、跨次累加」的等级提升。
 * <p>
 * 键为附魔的注册表 id 字符串（如 {@code inscription_arts:blazing}）。
 */
public record InscriptionProgress(Map<String, Float> charges) {

    public static final MapCodec<InscriptionProgress> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT)
                    .fieldOf("charges").forGetter(InscriptionProgress::charges)
    ).apply(in, InscriptionProgress::new));

    public InscriptionProgress {
        charges = Map.copyOf(charges);
    }

    /** 取某附魔距下一级的剩余充能（无则 0） */
    public float chargeOf(String enchantmentId) {
        return charges.getOrDefault(enchantmentId, 0f);
    }
}
