package com.inscription_arts.network;

import com.inscription_arts.InscriptionArts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 铭刻候选数据包（服务端 → 客户端）。
 * <p>
 * 服务端在玩家点击「铭刻」并校验通过后，生成最多 3 个候选附魔的注册表 id、
 * 连同本次铭刻的充能值发给客户端，客户端据此弹出三选一面板。此时尚未消耗精华。
 */
public record InscribeOfferPacket(BlockPos pos, List<ResourceLocation> ids, float charge)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InscribeOfferPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "inscribe_offer"));

    /** ResourceLocation 列表编解码（该 MC 版本未提供 StreamCodec.list，手动实现） */
    private static final StreamCodec<RegistryFriendlyByteBuf, List<ResourceLocation>> ID_LIST_CODEC =
            new StreamCodec<RegistryFriendlyByteBuf, List<ResourceLocation>>() {
                @Override
                public List<ResourceLocation> decode(RegistryFriendlyByteBuf buf) {
                    int n = buf.readVarInt();
                    List<ResourceLocation> list = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) {
                        list.add(ResourceLocation.STREAM_CODEC.decode(buf));
                    }
                    return list;
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, List<ResourceLocation> list) {
                    buf.writeVarInt(list.size());
                    for (ResourceLocation rl : list) {
                        ResourceLocation.STREAM_CODEC.encode(buf, rl);
                    }
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, InscribeOfferPacket> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    InscribeOfferPacket::pos,
                    ID_LIST_CODEC,
                    InscribeOfferPacket::ids,
                    ByteBufCodecs.FLOAT,
                    InscribeOfferPacket::charge,
                    InscribeOfferPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
