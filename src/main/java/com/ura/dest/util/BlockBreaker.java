package com.ura.dest.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import java.util.Collections;
import java.util.List;

public final class BlockBreaker {

    private BlockBreaker() {}

    /**
     * 破坏方块并返回掉落物列表。
     * 若被其他模组（保护、领地等）取消，则返回 null。
     */
    public static List<ItemStack> breakBlock(ServerPlayer player, BlockPos pos,
                                             ServerLevel level, ItemStack toolSnapshot) {
        var state = level.getBlockState(pos);
        if (state.isAir()) return Collections.emptyList();

        // 用工具快照算掉落，避免耐久度变更影响掉落判定
        var drops = Block.getDrops(
                state,
                level,
                pos,
                level.getBlockEntity(pos),
                player,
                toolSnapshot
        );

        // 触发 Forge 事件，让其他模组可以取消
        // 我们的处理器此时处于 BreakContext 保护中，会提前 return，不会再次取消
        var breakEvent = new BlockEvent.BreakEvent(level, pos, state, player);
        if (MinecraftForge.EVENT_BUS.post(breakEvent)) {
            return null;
        }

        // 无掉落、无粒子地移除方块（掉落由我们自己处理）
        level.removeBlock(pos, false);

        return drops;
    }
}