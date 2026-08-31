package com.inscription_arts.api.events;

import com.inscription_arts.api.InscriptionData;
import com.inscription_arts.api.RuneSlot;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * 铭刻相关公开事件，供附属模组挂接。
 * <ul>
 *   <li>{@link #BEFORE_INSCRIBE}：铭刻写入前触发，回调返回 {@code false} 可取消本次铭刻。</li>
 *   <li>{@link #AFTER_INSCRIBE}：铭刻成功写入装备后触发，可拿到最终的 {@link InscriptionData}。</li>
 * </ul>
 */
public final class InscriptionEvents {

    private InscriptionEvents() {
    }

    /** 铭刻前事件。任一回调返回 false 即取消本次铭刻。 */
    public static final Event<BeforeInscribe> BEFORE_INSCRIBE = EventFactory.createArrayBacked(
            BeforeInscribe.class,
            callbacks -> (player, equipment, runes) -> {
                for (BeforeInscribe cb : callbacks) {
                    if (!cb.before(player, equipment, runes)) {
                        return false;
                    }
                }
                return true;
            });

    /** 铭刻后事件。 */
    public static final Event<AfterInscribe> AFTER_INSCRIBE = EventFactory.createArrayBacked(
            AfterInscribe.class,
            callbacks -> (player, equipment, data) -> {
                for (AfterInscribe cb : callbacks) {
                    cb.after(player, equipment, data);
                }
            });

    @FunctionalInterface
    public interface BeforeInscribe {
        /**
         * @param player     执行铭刻的玩家
         * @param equipment  被铭刻的装备（已发生附魔写入，但 INSCRIPTION 组件尚未落盘）
         * @param runes      本次铭刻计划写入的槽位 → 符文映射
         * @return {@code true} 允许铭刻，{@code false} 取消
         */
        boolean before(Player player, ItemStack equipment, Map<RuneSlot, ResourceLocation> runes);
    }

    @FunctionalInterface
    public interface AfterInscribe {
        /**
         * @param player     执行铭刻的玩家
         * @param equipment  已铭刻完成的装备
         * @param data       装备上的最终铭刻数据
         */
        void after(Player player, ItemStack equipment, InscriptionData data);
    }
}
