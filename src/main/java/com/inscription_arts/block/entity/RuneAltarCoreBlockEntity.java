package com.inscription_arts.block.entity;

import com.inscription_arts.registry.ModBlockEntities;
import com.inscription_arts.screen.RuneAltarScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 符文祭坛核心的方块实体。实现 {@link ExtendedScreenHandlerFactory}，
 * 使服务端打开界面时能把核心坐标作为附加数据发送给客户端。
 */
public class RuneAltarCoreBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {

    public RuneAltarCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNE_ALTAR_CORE, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.inscription_arts.rune_altar_core");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
        return new RuneAltarScreenHandler(syncId, inventory, this.getBlockPos());
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.getBlockPos();
    }
}
