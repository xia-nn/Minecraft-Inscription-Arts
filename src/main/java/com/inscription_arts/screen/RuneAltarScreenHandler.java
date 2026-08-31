package com.inscription_arts.screen;

import com.inscription_arts.InscriptionArts;
import com.inscription_arts.api.InscriptionData;
import com.inscription_arts.api.RuneType;
import com.inscription_arts.api.ToolCategory;
import com.inscription_arts.api.InscriptionApi;
import com.inscription_arts.api.MaterialTrait;
import com.inscription_arts.component.ModDataComponents;
import com.inscription_arts.item.RuneEssenceItem;
import com.inscription_arts.network.InscribeOfferPacket;
import com.inscription_arts.registry.ModScreenHandlers;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 符文祭坛界面容器。服务端打开时由方块实体提供玩家背包与核心坐标；
 * 客户端由 {@link com.inscription_arts.registry.ModScreenHandlers} 的扩展类型携带坐标重建，
 * 并通过 Fabric 自动注入的玩家背包构造，无需引用客户端专用类。
 * <p>
 * 槽位：0 = 装备，1/2/3 = 3 个符文精华（一次铭刻消耗全部 3 枚，充能 = 3 枚之和）。
 * 点击「铭刻」由 {@link #requestOffer(Player)} 向服务端请求三选一候选（不消耗符文）；
 * 玩家在 UI 上选定后，由 {@link #commitChoice(Player, int)} 把该次铭刻的充能累加到所选附魔并消耗 3 枚精华。
 */
public class RuneAltarScreenHandler extends AbstractContainerMenu {

    /** 0 = 装备，1/2/3 = 3 个符文精华 */
    private final SimpleContainer inventory = new SimpleContainer(4);
    private final BlockPos pos;

    /** 待定三选一：服务端生成候选后暂存，待玩家选择后消费；客户端关闭界面即丢弃（不消耗精华） */
    private List<ResourceKey<Enchantment>> pendingCandidates;
    private float pendingCharge;
    private List<ResourceLocation> pendingRuneIds;

    /** 服务端/客户端共用的构造：坐标由方块实体（服务端）或网络包（客户端）提供 */
    public RuneAltarScreenHandler(int syncId, Inventory inv, BlockPos pos) {
        super(ModScreenHandlers.RUNE_ALTAR, syncId);
        this.pos = pos;
        addSlot(new Slot(inventory, 0, 40, 35));   // 装备
        addSlot(new Slot(inventory, 1, 82, 35));   // 符文精华 1
        addSlot(new Slot(inventory, 2, 100, 35));  // 符文精华 2
        addSlot(new Slot(inventory, 3, 118, 35));  // 符文精华 3
        addPlayerSlots(inv);
    }

    private void addPlayerSlots(Inventory inv) {
        int baseX = 8, baseY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, baseX + col * 18, baseY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, baseX + col * 18, baseY + 58));
        }
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = getSlot(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index <= 3) {
            // 祭坛槽 → 玩家背包
            if (!moveItemStackTo(stack, 4, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 玩家背包 → 祭坛槽：精华进槽 1-3，其余进装备槽 0
            if (RuneEssenceItem.getRuneId(stack) != null) {
                if (!moveItemStackTo(stack, 1, 4, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        // 玩家离祭坛核心超过 8 格则自动关闭界面（避免走远后界面悬空）
        return player.blockPosition().distSqr(pos) <= 64;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (!s.isEmpty()) {
                player.getInventory().add(s);
            }
        }
    }

    /**
     * 客户端/服务端共用：判断是否满足铭刻前置条件——装备槽与 3 个符文精华槽均已放置有效精华。
     * 客户端据此启用/置灰「铭刻」按钮，服务端据此做权威校验（防止客户端伪造请求绕过）。
     */
    public boolean inscriptionReady() {
        if (inventory.getItem(0).isEmpty()) {
            return false;
        }
        for (int i = 1; i <= 3; i++) {
            ItemStack r = inventory.getItem(i);
            if (r.isEmpty() || RuneEssenceItem.getRuneId(r) == null) {
                return false;
            }
        }
        return true;
    }

    /** 返回第一个为空的槽位显示名（已翻译）；全部放好则返回 null */
    private Component missingSlotName() {
        if (inventory.getItem(0).isEmpty()) {
            return Component.translatable("inscription_arts.slot.equipment");
        }
        for (int i = 1; i <= 3; i++) {
            ItemStack r = inventory.getItem(i);
            if (r.isEmpty() || RuneEssenceItem.getRuneId(r) == null) {
                return Component.translatable("inscription_arts.slot.rune");
            }
        }
        return null;
    }

    /**
     * 由服务端在收到铭刻请求（{@link com.inscription_arts.network.InscribePacket}）后执行。
     * 仅做<b>只读</b>校验与候选生成：校验装备/精华齐全、累加 3 枚精华的充能、按工具类别取出候选池、
     * 剔除满级后随机抽最多 3 个，连同本次总充能值通过 {@link InscribeOfferPacket} 发给客户端。
     * 此时<b>不消耗精华、不施加附魔</b>，待玩家在 UI 选定后才由 {@link #commitChoice} 落盘。
     */
    public void requestOffer(Player player) {
        Component missing = missingSlotName();
        if (missing != null) {
            player.sendSystemMessage(Component.translatable("message.inscription_arts.inscribe_missing", missing));
            return;
        }
        ItemStack equipment = inventory.getItem(0);
        float totalCharge = 0f;
        List<ResourceLocation> runeIds = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            ItemStack r = inventory.getItem(i);
            String id = RuneEssenceItem.getRuneId(r);
            Optional<RuneType> rt = InscriptionApi.getRuneById(id);
            if (rt.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.inscription_arts.inscribe_no_rune"));
                return;
            }
            totalCharge += rt.get().charge();
            runeIds.add(ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, id));
        }
        ToolCategory cat = ToolCategory.of(equipment);
        RegistryAccess access = player.level().registryAccess();
        List<MaterialTrait> candidates = InscriptionApi.offerCandidates(
                equipment, cat, access, player.level().getRandom());
        if (candidates.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.inscription_arts.inscribe_full"));
            return;
        }
        this.pendingCandidates = candidates.stream().map(MaterialTrait::enchantment).toList();
        this.pendingCharge = totalCharge;
        this.pendingRuneIds = runeIds;
        List<ResourceLocation> ids = pendingCandidates.stream().map(ResourceKey::location).toList();
        if (player instanceof ServerPlayer sp) {
            ServerPlayNetworking.send(sp, new InscribeOfferPacket(pos, ids, pendingCharge));
        }
    }

    /**
     * 由服务端在收到玩家选择（{@link com.inscription_arts.network.ChooseEnchantPacket}）后执行。
     * 把本次铭刻的充能累加到所选附魔、记录 3 个符文 id 到 {@code INSCRIPTION} 组件、消耗全部 3 枚精华。
     * 防作弊：index 越界或 pending 为空直接忽略；若精华已被移走也不产生副作用。
     */
    public void commitChoice(Player player, int index) {
        if (pendingCandidates == null || index < 0 || index >= pendingCandidates.size()) {
            return;
        }
        ItemStack equipment = inventory.getItem(0);
        // 二次校验：3 枚精华仍在位
        for (int i = 1; i <= 3; i++) {
            if (inventory.getItem(i).isEmpty()
                    || RuneEssenceItem.getRuneId(inventory.getItem(i)) == null) {
                pendingCandidates = null;
                pendingRuneIds = null;
                return;
            }
        }
        ResourceKey<Enchantment> key = pendingCandidates.get(index);
        RegistryAccess access = player.level().registryAccess();
        InscriptionApi.applyCharge(equipment, key, pendingCharge, access);
        // 记录符文 id（供萃取台修复反查对应材料）
        InscriptionData existing = equipment.get(ModDataComponents.INSCRIPTION);
        List<ResourceLocation> list = existing == null
                ? new ArrayList<>() : new ArrayList<>(existing.runes());
        if (pendingRuneIds != null) {
            list.addAll(pendingRuneIds);
        }
        equipment.set(ModDataComponents.INSCRIPTION, new InscriptionData(list));
        // 消耗全部 3 枚符文精华
        for (int i = 1; i <= 3; i++) {
            inventory.getItem(i).shrink(1);
        }
        inventory.setItem(0, equipment);
        pendingCandidates = null;
        pendingRuneIds = null;
        broadcastChanges();
        player.getInventory().setChanged();
        player.sendSystemMessage(Component.translatable("message.inscription_arts.inscribe_success"));
    }
}
