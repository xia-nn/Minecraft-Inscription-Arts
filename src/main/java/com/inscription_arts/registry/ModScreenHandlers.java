package com.inscription_arts.registry;

import com.inscription_arts.InscriptionArts;
import com.inscription_arts.screen.RuneAltarScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

/**
 * 界面（菜单）类型注册表。祭坛界面需要随打开动作把核心坐标发给客户端，
 * 因此使用 Fabric 的 {@link ExtendedScreenHandlerType}，以 {@link BlockPos} 作为附加数据。
 */
public final class ModScreenHandlers {

    public static final MenuType<RuneAltarScreenHandler> RUNE_ALTAR =
            Registry.register(
                    BuiltInRegistries.MENU,
                    ResourceLocation.fromNamespaceAndPath(InscriptionArts.MOD_ID, "rune_altar"),
                    new ExtendedScreenHandlerType<>(
                            (syncId, inventory, pos) -> new RuneAltarScreenHandler(syncId, inventory, pos),
                            BlockPos.STREAM_CODEC));

    private ModScreenHandlers() {
    }

    public static void register() {
        // 注册已在静态字段初始化中完成
    }
}
