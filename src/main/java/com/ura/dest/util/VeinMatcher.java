package com.ura.dest.util;

import com.ura.dest.Dest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Dest.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VeinMatcher {

    private static final TagKey<Block> ORES_TAG =
            BlockTags.create(new ResourceLocation("forge", "ores"));

    private static final Map<Block, Set<Block>> MATCH_CACHE = new ConcurrentHashMap<>();

    private VeinMatcher() {}

    public static boolean areSameVein(BlockState a, BlockState b) {
        Block blockA = a.getBlock();
        Block blockB = b.getBlock();
        if (blockA == blockB) return true;
        return MATCH_CACHE.computeIfAbsent(blockA, VeinMatcher::computeMatches).contains(blockB);
    }

    private static Set<Block> computeMatches(Block block) {
        BlockState state = block.defaultBlockState();
        if (!state.is(ORES_TAG)) return Collections.emptySet();

        Set<Block> result = new HashSet<>();
        for (TagKey<Block> tag : state.getTags().toList()) {
            ResourceLocation loc = tag.location();
            if (!"forge".equals(loc.getNamespace())) continue;
            if (!loc.getPath().startsWith("ores/")) continue;
            for (var holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
                result.add(holder.value());
            }
        }
        result.remove(block);
        return Collections.unmodifiableSet(result);
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        MATCH_CACHE.clear();
    }
}