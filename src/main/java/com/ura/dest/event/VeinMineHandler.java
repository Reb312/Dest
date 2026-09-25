package com.ura.dest.event;

import com.ura.dest.Config;
import com.ura.dest.core.BreakContext;
import com.ura.dest.util.BlockBreaker;
import com.ura.dest.util.KeyStateHolder;
import com.ura.dest.util.VeinScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VeinMineHandler {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (BreakContext.isProcessing()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!KeyStateHolder.isPressed(player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ItemStack tool = player.getMainHandItem();
        boolean isCreative = player.isCreative();
        // 创造模式豁免 REQUIRE_TOOL：即使空手也允许连锁
        if (Config.REQUIRE_TOOL.get() && tool.isEmpty() && !isCreative) return;

        BlockPos origin = event.getPos();
        BlockState originState = event.getState();

        // 基础校验：origin 本身必须可破坏
        if (originState.isAir()) return;
        if (originState.getDestroySpeed(level, origin) < 0) return;
        if (!level.mayInteract(player, origin)) return;

        int maxBlocks = Config.BLOCK_LIMIT.get();
        int maxDistance = Config.MAX_DISTANCE.get();
        double maxDistSq = (double) maxDistance * maxDistance;

        List<BlockPos> scanned = VeinScanner.scan(level, origin, originState, maxBlocks);

        // 过滤超出距离的方块
        List<BlockPos> positions = new ArrayList<>(scanned.size());
        for (BlockPos pos : scanned) {
            double dx = pos.getX() + 0.5 - player.getX();
            double dy = pos.getY() + 0.5 - player.getY();
            double dz = pos.getZ() + 0.5 - player.getZ();
            if (dx * dx + dy * dy + dz * dz <= maxDistSq) {
                positions.add(pos);
            }
        }

        if (positions.size() <= 1) return;

        // 预检查：至少有一个方块可被实际破坏（避免取消后一个都没破坏）
        boolean anyBreakable = false;
        for (BlockPos pos : positions) {
            if (!level.mayInteract(player, pos)) continue;
            BlockState st = level.getBlockState(pos);
            if (st.isAir()) continue;
            if (st.getDestroySpeed(level, pos) < 0) continue;
            anyBreakable = true;
            break;
        }
        if (!anyBreakable) return;

        // 取消原版破坏，避免重复掉落 / 重复事件
        event.setCanceled(true);

        // 附魔等级
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool);
        int silkTouch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0 ? 1 : 0;

        boolean consumeOnce = Config.CONSUME_DURABILITY.get();
        boolean consumeFood = Config.CONSUME_FOOD.get();

        // 工具快照用于掉落计算，避免一次性耐久预消耗导致工具为空
        ItemStack toolSnapshot = tool.copy();

        boolean toolBroken = false;
        // 一次性耐久预消耗：可能因耐久附魔而不消耗，因此不再依赖 hurt 的返回值
        if (!isCreative && !tool.isEmpty() && consumeOnce) {
            tool.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            if (tool.isEmpty()) {
                toolBroken = true;
            }
        }

        List<ItemStack> allDrops = new ArrayList<>();
        int totalExp = 0;
        int destroyedCount = 0;
        Map<BlockPos, BlockState> destroyedMap = new LinkedHashMap<>();

        BreakContext.setProcessing(true);
        try {
            for (BlockPos pos : positions) {
                // 工具已在预消耗时损坏：最多只破坏 1 个方块
                if (toolBroken && destroyedCount >= 1) break;

                if (!level.mayInteract(player, pos)) continue;

                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;
                if (state.getDestroySpeed(level, pos) < 0) continue;

                boolean correctTool = player.hasCorrectToolForDrops(state);

                List<ItemStack> drops =
                        BlockBreaker.breakBlock(player, pos, level, toolSnapshot);
                if (drops == null) continue;

                allDrops.addAll(drops);
                destroyedCount++;
                destroyedMap.put(pos.immutable(), state);

                if (correctTool) {
                    totalExp += state.getExpDrop(
                            level, level.random, pos, fortune, silkTouch);
                }

                // 非一次性模式：每破坏一个方块扣 1 点耐久
                if (!consumeOnce && !isCreative && !tool.isEmpty()) {
                    tool.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                    if (tool.isEmpty()) {
                        toolBroken = true;
                        break;
                    }
                }
            }

            if (destroyedCount == 0) {
                // 极端情况：所有方块都被其他模组取消，但我们已经取消了原事件
                // 兜底：手动破坏 origin，尽量还原原版体验
                List<ItemStack> drops = BlockBreaker.breakBlock(player, origin, level, toolSnapshot);
                if (drops != null) {
                    for (ItemStack drop : drops) {
                        if (!drop.isEmpty()) Block.popResource(level, origin, drop);
                    }
                    level.addDestroyBlockEffect(origin, originState);
                }
                return;
            }

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

                // 饥饿值消耗
                if (consumeFood) {
                    player.getFoodData().addExhaustion(0.1f * destroyedCount);
                }
            }

            // 粒子效果：所有被连锁破坏的方块都播放
            for (Map.Entry<BlockPos, BlockState> entry : destroyedMap.entrySet()) {
                level.addDestroyBlockEffect(entry.getKey(), entry.getValue());
            }

        } finally {
            BreakContext.clear();
        }
    }
}