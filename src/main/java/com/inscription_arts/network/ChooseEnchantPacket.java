package com.inscription_arts.network;

import com.inscription_arts.InscriptionArts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 选择附魔数据包（客户端 → 服务端）。
 * <p>
 * 玩家在三选一面板中点选某个候选后，把该候选的索引（0/1/2）与祭坛坐标发回，
 * 服务端据此把本次铭刻的充能累加到所选附魔并消耗 1 枚精华。index 越界会被服务端忽略。
 */
public record ChooseEnchantPacket(BlockPos pos, int index) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChooseEnchantPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "choose_enchant"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseEnchantPacket> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ChooseEnchantPacket::pos,
                    ByteBufCodecs.VAR_INT,
                    ChooseEnchantPacket::index,
                    ChooseEnchantPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
