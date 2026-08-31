package com.inscription_arts.network;

import com.inscription_arts.InscriptionArts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 铭刻请求数据包（客户端 → 服务端）。
 * <p>
 * 仅携带祭坛核心坐标；具体铭刻结果由服务端依据祭坛内物品与多方块结构权威计算，
 * 避免客户端伪造、保证确定性（对应匠魂「可预期成长」）。
 */
public record InscribePacket(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InscribePacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "inscribe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InscribePacket> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    InscribePacket::pos,
                    InscribePacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
