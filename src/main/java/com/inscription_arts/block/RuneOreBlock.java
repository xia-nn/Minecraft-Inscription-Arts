package com.inscription_arts.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;

/**
 * 符文矿石：地下生成，破坏后由数据包 {@code loot_table/blocks/rune_ore_<id>.json} 掉落对应符文精华
 * （通过 {@code set_components} 写入符文 id 的 data 组件），直接接入铭刻流水线。
 * 此处仅设定方块属性（硬度 / 音效），掉落逻辑完全由 loot table 驱动，避免脆弱的 getDrops 覆写。
 */
public class RuneOreBlock extends Block {

    public RuneOreBlock(String runeId, BlockBehaviour.Properties props) {
        // 硬度 2.0F（铜矿级别），不设 requiresCorrectToolForDrops()。
        // 掉落完全由 loot_table 驱动（data/inscription_arts/loot_table/blocks/rune_ore_<id>.json），
        // 不依赖 Java 层的"正确工具才掉落"机制，避免标签系统加载异常时镐被当错误工具惩罚 30×。
        super(props.strength(2.0F, 2.0F).sound(SoundType.STONE));
    }
}
