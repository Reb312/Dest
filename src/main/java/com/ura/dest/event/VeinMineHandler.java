package com.ura.dest.event;

import com.ura.dest.Config;
import com.ura.dest.core.BreakContext;
import com.ura.dest.util.BlockBreaker;
import com.ura.dest.util.KeyStateHolder;
import com.ura.dest.util.VeinScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class VeinMineHandler {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (BreakContext.isProcessing()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!KeyStateHolder.isPressed(player)) return;

        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ItemStack tool = player.getMainHandItem();
        if (Config.REQUIRE_TOOL.get() && tool.isEmpty()) return;

        BlockPos origin = event.getPos();
        var originState = event.getState();
        int maxBlocks = Config.BLOCK_LIMIT.get();

        List<BlockPos> positions = VeinScanner.scan(level, origin, originState, maxBlocks);
        if (positions.size() <= 1) return;

        // 提前取消原版破坏，避免重复掉落/重复事件
        event.setCanceled(true);

        // 附魔等级
        int fortune = 0;
        int silkTouch = 0;
        var enchantments = EnchantmentHelper.getEnchantments(tool);
        for (var entry : enchantments.entrySet()) {
            var key = BuiltInRegistries.ENCHANTMENT.getKey(entry.getKey());
            if (key == null) continue;
            String name = key.toString();
            if ("minecraft:fortune".equals(name)) fortune = entry.getValue();
            else if ("minecraft:silk_touch".equals(name)) silkTouch = 1;
        }

        boolean consumeOnce = Config.CONSUME_DURABILITY.get();
        boolean consumeFood = Config.CONSUME_FOOD.get();
        boolean isCreative = player.isCreative();

        // 工具快照用于掉落计算，避免一次性耐久预消耗导致工具为空
        ItemStack toolSnapshot = tool.copy();

        boolean toolBroken = false;
        // 一次性耐久预消耗：如果因此损坏，最多只破坏 1 个方块
        if (!isCreative && !tool.isEmpty() && consumeOnce) {
            if (tool.hurt(1, player.getRandom(), player)) {
                toolBroken = true;
            }
        }

        List<ItemStack> allDrops = new ArrayList<>();
        int totalExp = 0;
        int destroyedCount = 0;
        boolean originDestroyed = false;

        BreakContext.setProcessing(true);
        try {
            for (BlockPos pos : positions) {
                if (toolBroken && destroyedCount >= 1) break;

                // 仅做保护检查，不做“能否掉落”的判断
                if (!level.mayInteract(player, pos)) continue;

                var state = level.getBlockState(pos);
                if (state.isAir()) continue;
                // 负硬度（基岩/命令方块等）跳过
                if (state.getDestroySpeed(level, pos) < 0) continue;

                boolean correctTool = player.hasCorrectToolForDrops(state);

                List<ItemStack> drops =
                        BlockBreaker.breakBlock(player, pos, level, toolSnapshot);
                if (drops == null) continue; // 被其他模组取消

                allDrops.addAll(drops);
                destroyedCount++;
                if (pos.equals(origin)) originDestroyed = true;

                // 仅在工具正确时给经验
                if (correctTool) {
                    totalExp += state.getExpDrop(
                            level, level.random, pos, fortune, silkTouch);
                }

                // 非一次性模式：每破坏一个方块扣 1 点耐久
                if (!consumeOnce && !isCreative && !tool.isEmpty()) {
                    if (tool.hurt(1, player.getRandom(), player)) {
                        toolBroken = true;
                        break;
                    }
                }
            }

            if (destroyedCount == 0) return;

            if (!isCreative) {
                // 掉落物：优先入背包，否则弹出
                if (Config.PUT_IN_INVENTORY.get()) {
                    for (ItemStack drop : allDrops) {
                        if (drop.isEmpty()) continue;
                        ItemStack remaining = drop.copy();
                        if (!player.getInventory().add(remaining) && !remaining.isEmpty()) {
                            Block.popResource(level, origin, remaining);
                        }
                    }
                } else {
                    for (ItemStack drop : allDrops) {
                        if (!drop.isEmpty()) Block.popResource(level, origin, drop);
                    }
                }

                // 经验
                if (totalExp > 0) {
                    originState.getBlock().popExperience(level, origin, totalExp);
                }

                // 饥饿值消耗（实现 CONSUME_FOOD）
                if (consumeFood) {
                    player.getFoodData().addExhaustion(0.1f * destroyedCount);
                }
            }

            // 粒子效果：仅在 origin 实际被破坏时播放
            if (originDestroyed) {
                level.addDestroyBlockEffect(origin, originState);
            }

        } finally {
            BreakContext.clear();
        }
    }
}