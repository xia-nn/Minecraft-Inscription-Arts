package com.inscription_arts.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 符文祭坛多方块结构校验。
 * <p>
 * 以核心方块为原点：其正下方必须是一层 3×3 的<b>原石（圆石）</b>基底，
 * 四个对角位置（核心同一层起，向上 3 格）必须是<b>原木</b>立柱。
 * 校验通过才允许铭刻，结构不完整则仪式无效。
 */
public final class AltarDetector {

    private AltarDetector() {
    }

    public static boolean isValid(Level world, BlockPos core) {
        // 3×3 原石基底（核心正下方一层）
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (world.getBlockState(core.offset(dx, -1, dz)).getBlock() != Blocks.COBBLESTONE) {
                    return false;
                }
            }
        }
        // 四角原木立柱（核心同层起向上 3 格，共 3 块；任一种原木均可）
        for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
                for (int dy = 0; dy <= 2; dy++) {
                    BlockState pillar = world.getBlockState(core.offset(dx, dy, dz));
                    if (!pillar.is(BlockTags.LOGS)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
