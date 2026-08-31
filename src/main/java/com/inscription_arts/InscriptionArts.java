package com.inscription_arts;

import com.inscription_arts.altar.AltarDetector;
import com.inscription_arts.effect.RuneEffectApplier;
import com.inscription_arts.item.ModTools;
import com.inscription_arts.network.ChooseEnchantPacket;
import com.inscription_arts.network.InscribeOfferPacket;
import com.inscription_arts.network.InscribePacket;
import com.inscription_arts.registry.ModBlockEntities;
import com.inscription_arts.registry.ModBlocks;
import com.inscription_arts.component.ModDataComponents;
import com.inscription_arts.registry.ModItems;
import com.inscription_arts.registry.ModItemGroups;
import com.inscription_arts.registry.ModRunes;
import com.inscription_arts.registry.ModScreenHandlers;
import com.inscription_arts.registry.ModTraits;
import com.inscription_arts.screen.RuneAltarScreenHandler;
import com.inscription_arts.world.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InscriptionArts implements ModInitializer {
	public static final String MOD_ID = "inscription_arts";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 数据组件与注册表
		ModDataComponents.register();
		// 注：自创附魔（blazing/venom/siphon）为 1.21 数据驱动动态注册表，
		// 通过 data/inscription_arts/enchantment/*.json 定义，无需在此代码注册。
		ModRunes.register();
		ModTraits.register();
		// 方块 / 方块实体 / 界面 / 物品
		ModBlocks.register();
		ModBlockEntities.register();
		ModScreenHandlers.register();
		ModItems.register();
		// 自创工具（锤 / 铲）的注册与范围挖掘事件，须在标签页之前完成物品注册
		ModTools.register();
		// 创造模式标签页（物品/方块必须加入标签页才会在创造模式显示）
		ModItemGroups.register();
		// 符文矿石世界生成（加入主世界地下）
		ModWorldGen.register();
		// 游戏内效果（事件）
		RuneEffectApplier.register();

		// 铭刻请求网络包：先在双方注册编解码器，再注册服务端处理器
		PayloadTypeRegistry.playC2S().register(InscribePacket.ID, InscribePacket.CODEC);
		PayloadTypeRegistry.playC2S().register(ChooseEnchantPacket.ID, ChooseEnchantPacket.CODEC);
		// 三选一候选（服务端 → 客户端）
		PayloadTypeRegistry.playS2C().register(InscribeOfferPacket.ID, InscribeOfferPacket.CODEC);

		// 客户端铭刻请求 → 服务端生成三选一候选（不消耗精华）
		ServerPlayNetworking.registerGlobalReceiver(InscribePacket.ID, (InscribePacket packet, Context context) -> {
			ServerPlayer player = context.player();
			Level world = player.level();
			BlockPos altarPos = packet.pos();
			if (!AltarDetector.isValid(world, altarPos)) {
				LOGGER.warn("铭刻失败：祭坛结构不完整 @ {}", altarPos);
				return;
			}
			if (player.containerMenu instanceof RuneAltarScreenHandler handler
					&& handler.getPos().equals(altarPos)) {
				handler.requestOffer(player);
			}
		});

		// 客户端选择某附魔 → 服务端落盘充能并消耗精华
		ServerPlayNetworking.registerGlobalReceiver(ChooseEnchantPacket.ID, (ChooseEnchantPacket packet, Context context) -> {
			ServerPlayer player = context.player();
			Level world = player.level();
			BlockPos altarPos = packet.pos();
			if (!AltarDetector.isValid(world, altarPos)) {
				return;
			}
			if (player.containerMenu instanceof RuneAltarScreenHandler handler
					&& handler.getPos().equals(altarPos)) {
				handler.commitChoice(player, packet.index());
			}
		});

		String version = FabricLoader.getInstance()
				.getModContainer(MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString())
				.orElse("?");
		LOGGER.info("铭刻之艺 (Inscription Arts) v{} 已加载", version);
	}
}
