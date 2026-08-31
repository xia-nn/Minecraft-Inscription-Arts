package com.inscription_arts.item;

import com.inscription_arts.InscriptionArts;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 自创工具：符文之锤（3×3 镐）、符文之铲（3×3 铲）、符文之斧（3×3 斧）、符文之锄（3×3 耕地）、符文之剑（横扫）。
 * <p>
 * 工具均继承自原版对应工具类型，因此天然兼容铭刻系统（祭坛可对其施加任意符文 / 附魔，
 * 效果由 {@code RuneEffectApplier} 读取 {@code InscriptionData} 施加）。范围能力分别通过不同的
 * 服务端权威事件实现：采矿类走 {@code PlayerBlockBreakEvents.BEFORE}，锄走 {@code UseBlockCallback}，
 * 剑走 {@code AttackEntityCallback}。
 */
public final class ModTools {

    /** 符文之锤：镐类，3×3 挖掘石头 / 矿石等 */
    public static final PickaxeItem HAMMER = new PickaxeItem(
            Tiers.NETHERITE, new Item.Properties()) {
    };

    /** 符文之铲：铲类，3×3 挖掘泥土 / 沙 / 砾石等 */
    public static final ShovelItem EXCAVATOR = new ShovelItem(
            Tiers.NETHERITE, new Item.Properties()) {
    };

    /** 符文之斧：斧类，3×3 砍伐木头 / 原木 / 木板等（复用采矿机制） */
    public static final AxeItem AXE = new AxeItem(
            Tiers.NETHERITE, new Item.Properties()) {
    };

    /** 符文之锄：锄类，右击 3×3 耕地 */
    public static final HoeItem HOE = new HoeItem(
            Tiers.NETHERITE, new Item.Properties()) {
    };

    /** 符文之剑：剑类，左击横扫 3×3 内生物（Tiers.NETHERITE 自带下界合金剑攻击/速度） */
    public static final SwordItem SWORD = new SwordItem(
            Tiers.NETHERITE, new Item.Properties()) {
    };

    /** 防止 3×3 采矿展开时因递归再次触发本方块事件（双重保险） */
    private static final AtomicBoolean EXPANDING = new AtomicBoolean(false);

    /** 防止剑横扫时因受击回调递归再次触发攻击事件 */
    private static final AtomicBoolean ATTACKING = new AtomicBoolean(false);

    private ModTools() {
    }

    /** 注册工具物品与各类范围能力事件（服务端权威） */
    public static void register() {
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "hammer"), HAMMER);
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "excavator"), EXCAVATOR);
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "axe"), AXE);
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "hoe"), HOE);
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "sword"), SWORD);

        // —— 采矿类范围挖掘（锤 / 铲 / 斧）——
        PlayerBlockBreakEvents.BEFORE.register((Level world, Player player, BlockPos pos,
                                                BlockState state, BlockEntity blockEntity) -> {
            if (world.isClientSide()) {
                return true;
            }
            ItemStack tool = player.getMainHandItem();
            if (tool.isEmpty()
                    || (tool.getItem() != HAMMER && tool.getItem() != EXCAVATOR && tool.getItem() != AXE)) {
                return true;
            }
            if (!EXPANDING.compareAndSet(false, true)) {
                return true;
            }
            try {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        breakArea(world, player, pos.offset(dx, 0, dz), tool);
                    }
                }
                // 一次挥动消耗 1 点耐久（效率高于原版单格，但仍有损耗）
                tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            } finally {
                EXPANDING.set(false);
            }
            // 取消原版单格破坏：9 格已由本方块逻辑统一处理
            return false;
        });

        // —— 锄：右击 3×3 耕地 ——
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            ItemStack tool = player.getMainHandItem();
            if (tool.getItem() != HOE) {
                return InteractionResult.PASS;
            }
            BlockPos center = hit.getBlockPos();
            if (!isTillable(world.getBlockState(center))) {
                return InteractionResult.PASS; // 不是可耕方块 → 放行原版
            }
            tillArea(world, player, center, tool);
            return InteractionResult.SUCCESS; // 已处理 3×3，取消原版单格耕地
        });

        // —— 剑：左击横扫 3×3 内生物 ——
        AttackEntityCallback.EVENT.register((player, world, hand, target, hit) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            ItemStack tool = player.getMainHandItem();
            if (tool.getItem() != SWORD) {
                return InteractionResult.PASS;
            }
            if (!ATTACKING.compareAndSet(false, true)) {
                return InteractionResult.PASS; // 防递归
            }
            try {
                sweepArea(world, player, target, tool);
            } finally {
                ATTACKING.set(false);
            }
            return InteractionResult.PASS; // 不取消原版攻击，原目标照常受击，横扫只额外打周围
        });
    }

    private static void breakArea(Level world, Player player, BlockPos p, ItemStack tool) {
        BlockState st = world.getBlockState(p);
        if (st.isAir() || st.getBlock() == Blocks.BEDROCK) {
            return;
        }
        if (!world.mayInteract(player, p)) {
            return;
        }
        if (!tool.isCorrectToolForDrops(st)) {
            return;
        }
        BlockEntity be = st.hasBlockEntity() ? world.getBlockEntity(p) : null;
        // playerDestroy 只负责掉落物与统计，不会移除方块本身（1.21 行为），
        // 必须在此显式移除并补破碎粒子，否则会出现「掉落却不消失」的幽灵方块。
        st.getBlock().playerDestroy(world, player, p, st, be, tool);
        world.removeBlock(p, false);
        world.levelEvent(2001, p, Block.getId(st));
    }

    private static boolean isTillable(BlockState st) {
        return st.is(Blocks.DIRT) || st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.DIRT_PATH);
    }

    private static void tillArea(Level world, Player player, BlockPos center, ItemStack tool) {
        boolean tilled = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = center.offset(dx, 0, dz);
                if (!world.mayInteract(player, p)) {
                    continue; // 跳过受保护区域
                }
                BlockState st = world.getBlockState(p);
                if (!isTillable(st)) {
                    continue;
                }
                if (!world.getBlockState(p.above()).isAir()) {
                    continue; // 上方需为空才能种地
                }
                world.setBlock(p, Blocks.FARMLAND.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
                tilled = true;
            }
        }
        if (tilled) {
            world.playSound(null, center, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND); // 一次使用 -1 耐久
        }
    }

    private static void sweepArea(Level world, Player player, Entity target, ItemStack tool) {
        double r = 1.5D;
        AABB box = new AABB(target.getX() - r, target.getY() - r, target.getZ() - r,
                target.getX() + r, target.getY() + r, target.getZ() + r);
        float dmg = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F; // 横扫 = 半伤
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == target || e == player) {
                continue;
            }
            if (player.distanceTo(e) > 4.0D) {
                continue;
            }
            e.hurt(world.damageSources().playerAttack(player), dmg);
        }
    }
}
