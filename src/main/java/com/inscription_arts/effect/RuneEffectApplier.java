package com.inscription_arts.effect;

import com.inscription_arts.balance.ModConfig;
import com.inscription_arts.registry.ModEnchantments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 符文效果应用器。在游戏事件中根据装备上铭刻的自创附魔等级施加固定、可预期的效果。
 * <p>
 * 三组自创附魔：
 * <ul>
 *   <li>战斗（剑/斧/重锤/三叉戟/权杖）：烈焰/淬毒/汲取/雷霆/寒霜 —— 在 {@link AttackEntityCallback} 中施加；</li>
 *   <li>挖掘（镐）：精炼(自动熔炼)/辉光(夜视)/磁力(吸附)/勘探(额外矿石)/疾速(属性) —— 在破坏事件或 tick 中施加；</li>
 *   <li>弓：贯穿/精准/迅捷/爆裂/灼燃 —— 在箭矢生成({@link ServerEntityEvents#ENTITY_LOAD})与命中({@link ServerLivingEntityEvents#AFTER_DAMAGE})时施加。</li>
 * </ul>
 * 所有效果均读取装备上的附魔整数等级，按 {@link ModConfig} 缩放并封顶，与铭刻系统解耦。
 */
public final class RuneEffectApplier {

    private RuneEffectApplier() {
    }

    /** AbstractArrow.setPierceLevel(byte) 在 1.21.1 为 private，运行时用中间名 field_7589 反射赋值 */
    private static final Field ARROW_PIERCE_FIELD;
    static {
        Field f = null;
        try {
            f = AbstractArrow.class.getDeclaredField("field_7589");
            f.setAccessible(true);
        } catch (Exception ignored) {
            try {
                f = AbstractArrow.class.getDeclaredField("PIERCE_LEVEL");
                f.setAccessible(true);
            } catch (Exception ignored2) {
                f = null;
            }
        }
        ARROW_PIERCE_FIELD = f;
    }

    private static void setArrowPierce(AbstractArrow arrow, int level) {
        if (ARROW_PIERCE_FIELD == null) {
            return;
        }
        try {
            ARROW_PIERCE_FIELD.setByte(arrow, (byte) Math.min(127, level));
        } catch (Exception ignored) {
        }
    }

    /** AbstractArrow.getPickupItem() 在 1.21.1 为 protected，运行时用中间名 field_46970 反射取物品栈 */
    private static final Field ARROW_ITEM_FIELD;
    static {
        Field f = null;
        try {
            f = AbstractArrow.class.getDeclaredField("field_46970");
            f.setAccessible(true);
        } catch (Exception ignored) {
            try {
                f = AbstractArrow.class.getDeclaredField("pickupItemStack");
                f.setAccessible(true);
            } catch (Exception ignored2) {
                f = null;
            }
        }
        ARROW_ITEM_FIELD = f;
    }

    private static ItemStack getArrowItem(AbstractArrow arrow) {
        if (ARROW_ITEM_FIELD == null) {
            return ItemStack.EMPTY;
        }
        try {
            return (ItemStack) ARROW_ITEM_FIELD.get(arrow);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static void register() {
        registerCombat();
        registerMining();
        registerRadiance();
        registerBow();
    }

    /** 读取物品上某自创附魔的等级 */
    private static int enchLevel(ItemStack stack, ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key, RegistryAccess access) {
        if (stack.isEmpty()) {
            return 0;
        }
        return stack.getEnchantments().getLevel(ModEnchantments.holder(access, key));
    }

    // ——————————————————————— 战斗组 ———————————————————————

    private static void registerCombat() {
        AttackEntityCallback.EVENT.register((player, world, hand, target, hitResult) -> {
            ItemStack weapon = player.getMainHandItem();
            if (!world.isClientSide() && target instanceof LivingEntity living) {
                var ench = weapon.getEnchantments();
                if (!ench.isEmpty()) {
                    var access = world.registryAccess();
                    int blazing = ench.getLevel(ModEnchantments.holder(access, ModEnchantments.BLAZING));
                    if (blazing > 0) {
                        living.setRemainingFireTicks(blazing * ModConfig.BLAZING_SECONDS_PER_LEVEL * 20);
                    }
                    int venom = ench.getLevel(ModEnchantments.holder(access, ModEnchantments.VENOM));
                    if (venom > 0) {
                        living.addEffect(new MobEffectInstance(MobEffects.POISON,
                                venom * ModConfig.VENOM_TICKS_PER_LEVEL, venom - 1));
                    }
                    int siphon = ench.getLevel(ModEnchantments.holder(access, ModEnchantments.SIPHON));
                    if (siphon > 0) {
                        player.heal(siphon * ModConfig.SIPHON_HEAL_PER_LEVEL);
                    }
                    int thunder = ench.getLevel(ModEnchantments.holder(access, ModEnchantments.THUNDER));
                    if (thunder > 0) {
                        float chance = Math.min(ModConfig.THUNDER_CHANCE_PER_LEVEL * thunder, ModConfig.THUNDER_MAX_CHANCE);
                        if (world.getRandom().nextFloat() < chance) {
                            net.minecraft.world.entity.LightningBolt bolt = new net.minecraft.world.entity.LightningBolt(
                                    net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, world);
                            bolt.moveTo(target.getX(), target.getY(), target.getZ());
                            world.addFreshEntity(bolt);
                        }
                    }
                    int frost = ench.getLevel(ModEnchantments.holder(access, ModEnchantments.FROST));
                    if (frost > 0) {
                        int amp = Math.min(frost - 1, ModConfig.FROST_MAX_AMPLIFIER);
                        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                                frost * ModConfig.FROST_TICKS_PER_LEVEL, amp));
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }

    // ——————————————————————— 挖掘组 ———————————————————————

    private static void registerMining() {
        PlayerBlockBreakEvents.AFTER.register((Level world, Player player, BlockPos pos,
                                               BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity) -> {
            if (world.isClientSide()) {
                return;
            }
            ItemStack tool = player.getMainHandItem();
            var ench = tool.getEnchantments();
            if (ench.isEmpty()) {
                return;
            }
            var access = world.registryAccess();
            int refine = ench.getLevel(ModEnchantments.holder(access, ModEnchantments.REFINE));
            int magnetism = ench.getLevel(ModEnchantments.holder(access, ModEnchantments.MAGNETISM));
            int prospect = ench.getLevel(ModEnchantments.holder(access, ModEnchantments.PROSPECT));

            AABB near = new AABB(pos).inflate(1.0);
            if (refine > 0 && ModConfig.REFINE_ENABLED) {
                refineDrops(world, near);
            }
            if (prospect > 0 && isOreBlock(state)) {
                prospectBonus(world, near, prospect);
            }
            if (magnetism > 0) {
                attractDrops(world, player, magnetism);
            }
        });
    }

    /** 精炼：把破坏产生的掉落物按熔炼配方替换为成品 */
    private static void refineDrops(Level world, AABB area) {
        RecipeManager rm = world.getRecipeManager();
        for (ItemEntity ie : world.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack drop = ie.getItem();
            if (drop.isEmpty()) {
                continue;
            }
            var opt = rm.getRecipeFor(RecipeType.SMELTING,
                    new net.minecraft.world.item.crafting.SingleRecipeInput(drop), world);
            if (opt.isEmpty()) {
                continue;
            }
            RecipeHolder<SmeltingRecipe> r = opt.get();
            ItemStack result = r.value().getResultItem(world.registryAccess());
            if (result == null || result.isEmpty()) {
                continue;
            }
            int count = drop.getCount() * result.getCount();
            ie.setItem(result.copyWithCount(count));
        }
    }

    /** 勘探：对矿石破坏产生的掉落物按等级概率额外复制一份 */
    private static void prospectBonus(Level world, AABB area, int prospect) {
        double chance = Math.min(ModConfig.PROSPECT_CHANCE_PER_LEVEL * prospect, 1.0);
        for (ItemEntity ie : world.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack drop = ie.getItem();
            if (drop.isEmpty()) {
                continue;
            }
            if (world.getRandom().nextFloat() >= chance) {
                continue;
            }
            int extra = Math.min(ModConfig.PROSPECT_MAX_EXTRA, drop.getCount());
            ItemStack copy = drop.copy();
            copy.setCount(extra);
            world.addFreshEntity(new ItemEntity(world, ie.getX(), ie.getY(), ie.getZ(), copy));
        }
    }

    /** 磁力：把半径内的掉落物拉向玩家 */
    private static void attractDrops(Level world, Player player, int magnetism) {
        double radius = ModConfig.MAGNETISM_BASE_RADIUS + ModConfig.MAGNETISM_RADIUS_PER_LEVEL * magnetism;
        AABB area = new AABB(player.blockPosition()).inflate(radius);
        Vec3 playerPos = player.position();
        for (ItemEntity ie : world.getEntitiesOfClass(ItemEntity.class, area)) {
            Vec3 to = playerPos.subtract(ie.position());
            double dist = to.length();
            if (dist > 0.15) {
                ie.setDeltaMovement(to.normalize().scale(ModConfig.MAGNETISM_PULL_SPEED));
            }
        }
    }

    private static boolean isOreBlock(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.COAL_ORES)
                || state.is(net.minecraft.tags.BlockTags.IRON_ORES)
                || state.is(net.minecraft.tags.BlockTags.COPPER_ORES)
                || state.is(net.minecraft.tags.BlockTags.GOLD_ORES)
                || state.is(net.minecraft.tags.BlockTags.DIAMOND_ORES)
                || state.is(net.minecraft.tags.BlockTags.EMERALD_ORES)
                || state.is(net.minecraft.tags.BlockTags.REDSTONE_ORES)
                || state.is(net.minecraft.tags.BlockTags.LAPIS_ORES);
    }

    // ——————————————————————— 辉光（夜视） ———————————————————————

    private static void registerRadiance() {
        ServerTickEvents.END_WORLD_TICK.register((ServerLevel level) -> {
            for (Player player : level.players()) {
                int rad = Math.max(
                        enchLevel(player.getMainHandItem(), ModEnchantments.RADIANCE, level.registryAccess()),
                        enchLevel(player.getOffhandItem(), ModEnchantments.RADIANCE, level.registryAccess()));
                if (rad > 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                            ModConfig.RADIANCE_NIGHT_VISION_TICKS, 0, true, false));
                }
            }
        });
    }

    // ——————————————————————— 弓组 ———————————————————————

    private static void registerBow() {
        // 箭矢生成：按弓上的附魔等级初始化箭矢（贯穿/精准/迅捷/灼燃），并记录爆裂/灼燃等级供命中时读取
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (world.isClientSide() || !(entity instanceof AbstractArrow arrow)) {
                return;
            }
            if (!(arrow.getOwner() instanceof Player player)) {
                return;
            }
            ItemStack arrowItem = getArrowItem(arrow);
            CustomData existing = arrowItem.get(DataComponents.CUSTOM_DATA);
            if (existing != null && existing.getUnsafe().getBoolean("ia_proc")) {
                return; // 已处理（如区块重载），避免重复加速
            }
            ItemStack bow = player.getMainHandItem();
            if (!(bow.getItem() instanceof net.minecraft.world.item.BowItem)) {
                bow = player.getOffhandItem();
                if (!(bow.getItem() instanceof net.minecraft.world.item.BowItem)) {
                    return;
                }
            }
            RegistryAccess access = world.registryAccess();
            int pierce = enchLevel(bow, ModEnchantments.PIERCE, access);
            int precision = enchLevel(bow, ModEnchantments.PRECISION, access);
            int velocity = enchLevel(bow, ModEnchantments.VELOCITY, access);
            int explosive = enchLevel(bow, ModEnchantments.EXPLOSIVE, access);
            int incinerate = enchLevel(bow, ModEnchantments.INCINERATE, access);

            if (pierce > 0) {
                setArrowPierce(arrow, pierce * ModConfig.PIERCE_PER_LEVEL);
            }
            if (precision > 0) {
                arrow.setBaseDamage(arrow.getBaseDamage() + precision * ModConfig.PRECISION_DMG_PER_LEVEL);
            }
            if (velocity > 0) {
                arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1.0 + ModConfig.VELOCITY_PER_LEVEL * velocity));
            }
            if (incinerate > 0) {
                arrow.igniteForSeconds(100f);
            }
            CustomData.update(DataComponents.CUSTOM_DATA, arrowItem, tag -> {
                tag.putBoolean("ia_proc", true);
                tag.putInt("ia_explosive", explosive);
                tag.putInt("ia_incinerate", incinerate);
                tag.putBoolean("ia_exploded", false);
            });
        });

        // 箭矢命中：爆裂（爆炸）与灼燃（点燃）
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (!(source.getDirectEntity() instanceof AbstractArrow arrow)) {
                return;
            }
            if (arrow.level().isClientSide()) {
                return;
            }
            ItemStack arrowItem = getArrowItem(arrow);
            CustomData cd = arrowItem.get(DataComponents.CUSTOM_DATA);
            if (cd == null) {
                return;
            }
            CompoundTag tag = cd.getUnsafe();
            int explosive = tag.getInt("ia_explosive");
            int incinerate = tag.getInt("ia_incinerate");
            Level world = arrow.level();
            if (explosive > 0 && !tag.getBoolean("ia_exploded")) {
                float radius = Math.min(explosive * ModConfig.EXPLOSIVE_RADIUS_PER_LEVEL, ModConfig.EXPLOSIVE_MAX_RADIUS);
                world.explode(arrow.getOwner(), entity.getX(), entity.getY(), entity.getZ(),
                        radius, false, ExplosionInteraction.MOB);
                CustomData.update(DataComponents.CUSTOM_DATA, arrowItem, t -> t.putBoolean("ia_exploded", true));
            }
            if (incinerate > 0 && entity instanceof LivingEntity living) {
                living.setRemainingFireTicks(incinerate * ModConfig.INCINERATE_SECONDS_PER_LEVEL * 20);
            }
        });
    }
}
