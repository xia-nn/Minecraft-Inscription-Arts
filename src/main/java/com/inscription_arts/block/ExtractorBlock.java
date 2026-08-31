package com.inscription_arts.block;

import com.inscription_arts.api.InscriptionApi;
import com.inscription_arts.api.InscriptionData;
import com.inscription_arts.api.RuneType;
import com.inscription_arts.balance.ModConfig;
import com.inscription_arts.component.ModDataComponents;
import com.inscription_arts.item.RuneEssenceItem;
import com.inscription_arts.registry.ModRunes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * 萃取台。
 * <p>
 * 两个用途：
 * <ol>
 *   <li>右键手持可萃取材料（铁锭 / 金锭 / 纸… 含附属模组注册的材料）时，消耗 1 个材料，
 *       产出对应的符文精华直接放入玩家背包。这是「材料 → 精华」的第一步。</li>
 *   <li>修复：当<b>任意一手</b>持有<b>已受损的铭刻装备</b>、<b>另一手</b>持有其<b>对应材料</b>时，
 *       消耗 1 个材料为该装备恢复耐久（不消失）。对应材料 = 装备上强度最高符文所来源的材料。</li>
 * </ol>
 */
public class ExtractorBlock extends Block {

    public ExtractorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockBehaviour.simpleCodec(ExtractorBlock::new);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()) {
            // 客户端仅做预测，权威行为在服务端
            return ItemInteractionResult.SUCCESS;
        }

        // —— 修复：任一手持受损铭刻装备 + 另一手对应材料 ——
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        ItemStack toRepair = null;
        ItemStack material = null;
        if (stack.has(ModDataComponents.INSCRIPTION) && stack.isDamaged()) {
            toRepair = stack;
            material = other;
        } else if (other.has(ModDataComponents.INSCRIPTION) && other.isDamaged()) {
            toRepair = other;
            material = stack;
        }
        if (toRepair != null) {
            Optional<String> bestRuneOpt = repairRuneId(toRepair);
            if (bestRuneOpt.isPresent()) {
                Optional<Item> requiredOpt = InscriptionApi.materialOfRune(bestRuneOpt.get());
                if (requiredOpt.isPresent()) {
                    Item required = requiredOpt.get();
                    if (!material.isEmpty() && material.is(required)) {
                        float strength = InscriptionApi.getRuneById(bestRuneOpt.get())
                                .map(RuneType::charge).orElse(1.0f);
                        int amount = Math.max(1, (int) (ModConfig.REPAIR_AMOUNT * strength));
                        int damage = Math.max(0, toRepair.getDamageValue() - amount);
                        toRepair.setDamageValue(damage);
                        material.shrink(1);
                        world.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.0f);
                        if (world instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.ENCHANT,
                                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                                    10, 0.4, 0.5, 0.4, 0.05);
                        }
                        player.displayClientMessage(Component.translatable(
                                "message.inscription_arts.repair_done",
                                required.getName(ItemStack.EMPTY)), true);
                        return ItemInteractionResult.SUCCESS;
                    } else {
                        player.displayClientMessage(Component.translatable(
                                "message.inscription_arts.repair_need",
                                required.getName(ItemStack.EMPTY)), true);
                        return ItemInteractionResult.SUCCESS;
                    }
                }
            }
        }

        // —— 萃取：手持可萃取材料 → 产出对应精华 ——
        var runeOpt = ModRunes.getByItem(stack);
        if (runeOpt.isEmpty()) {
            runeOpt = InscriptionApi.getByItem(stack);
        }
        if (runeOpt.isPresent()) {
            ItemStack essence = RuneEssenceItem.create(runeOpt.get().id());
            if (!player.getInventory().add(essence)) {
                player.drop(essence, false);
            }
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 取装备上充能最高的符文 id（用于决定修复所需的对应材料） */
    private static Optional<String> repairRuneId(ItemStack stack) {
        InscriptionData data = stack.get(ModDataComponents.INSCRIPTION);
        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }
        String bestId = null;
        float best = -1f;
        for (ResourceLocation rl : data.runes()) {
            Optional<RuneType> rt = InscriptionApi.getRuneById(rl.getPath());
            if (rt.isPresent() && rt.get().charge() > best) {
                best = rt.get().charge();
                bestId = rt.get().id();
            }
        }
        return Optional.ofNullable(bestId);
    }
}
