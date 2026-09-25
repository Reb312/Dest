package com.ura.dest.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public final class VeinScanner {
    private static final List<BlockPos> OFFSETS = new ArrayList<>();

    static {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    OFFSETS.add(new BlockPos(x, y, z));
                }
            }
        }
    }

    public static List<BlockPos> scan(Level level, BlockPos origin, BlockState originState, int maxBlocks) {
        var result = new ArrayList<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();

        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            var current = queue.poll();
            result.add(current);

            for (var offset : OFFSETS) {
                var neighbor = current.offset(offset);
                if (!visited.contains(neighbor)) {
                    var state = level.getBlockState(neighbor);
                    if (VeinMatcher.areSameVein(originState, state)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return result;
    }
}