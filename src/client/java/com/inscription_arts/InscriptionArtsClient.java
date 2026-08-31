package com.inscription_arts;

import com.inscription_arts.api.InscriptionProgress;
import com.inscription_arts.balance.ModConfig;
import com.inscription_arts.component.ModDataComponents;
import com.inscription_arts.network.InscribeOfferPacket;
import com.inscription_arts.registry.ModScreenHandlers;
import com.inscription_arts.screen.RuneAltarScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Locale;
import java.util.Map;

public class InscriptionArtsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 注册祭坛界面：将 MenuType 绑定到客户端 Screen
		MenuScreens.register(ModScreenHandlers.RUNE_ALTAR, RuneAltarScreen::new);

		// 服务端发来的三选一候选 → 转发给当前打开的祭坛界面，弹出选择面板
		ClientPlayNetworking.registerGlobalReceiver(InscribeOfferPacket.ID,
				(InscribeOfferPacket packet, net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context context) -> {
					Minecraft mc = Minecraft.getInstance();
					Screen screen = mc.screen;
					if (screen instanceof RuneAltarScreen altar) {
						// 在客户端线程应用，避免跨线程访问界面组件
						mc.execute(() -> altar.showOffer(packet.ids(), packet.charge()));
					}
				});

		// 物品提示框：为带铭刻进度的装备追加「有效分数等级」与「充能余量」两行
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			InscriptionProgress prog = stack.get(ModDataComponents.INSCRIPTION_PROGRESS);
			if (prog == null || prog.charges().isEmpty()) {
				return;
			}

			// 整数等级来自原版附魔组件；进度余数来自本 mod 的 INSCRIPTION_PROGRESS
			ItemEnchantments ench = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
			var level0 = Minecraft.getInstance().level;
			if (level0 == null) {
				return;
			}
			var reg = level0.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

			lines.add(Component.translatable("tooltip.inscription_arts.inscription_header")
					.withStyle(ChatFormatting.GOLD));

			for (Map.Entry<String, Float> e : prog.charges().entrySet()) {
				ResourceLocation id = ResourceLocation.parse(e.getKey());
				float acc = e.getValue();
				int level = reg.getHolder(ResourceKey.create(Registries.ENCHANTMENT, id))
						.map(h -> ench.getLevel(h)).orElse(0);
				Component name = Component.translatable(
						"enchantment." + id.getNamespace() + "." + id.getPath());

				if (level >= ModConfig.MAX_LEVEL) {
					lines.add(Component.literal("  ").append(name).append(Component.literal(" "))
							.append(Component.translatable("tooltip.inscription_arts.effective", "10.00"))
							.withStyle(ChatFormatting.AQUA));
					lines.add(Component.literal("  ").append(
							Component.translatable("tooltip.inscription_arts.maxed"))
							.withStyle(ChatFormatting.YELLOW));
					continue;
				}

				int threshold = ModConfig.levelUpCost(level);
				float effective = level + (threshold > 0 ? acc / threshold : 0f);
				float need = Math.max(0f, threshold - acc);
				lines.add(Component.literal("  ").append(name).append(Component.literal(" "))
						.append(Component.translatable("tooltip.inscription_arts.effective",
								String.format(Locale.ROOT, "%.2f", effective)))
						.withStyle(ChatFormatting.AQUA));
				lines.add(Component.literal("  ").append(
								Component.translatable("tooltip.inscription_arts.charge",
										String.format(Locale.ROOT, "%.1f", acc),
										String.format(Locale.ROOT, "%d", threshold)))
						.append(Component.literal("（"))
						.append(Component.translatable("tooltip.inscription_arts.need",
								String.format(Locale.ROOT, "%d", level + 1),
								String.format(Locale.ROOT, "%.1f", need)))
						.append(Component.literal("）"))
						.withStyle(ChatFormatting.GRAY));
			}
		});
	}
}
