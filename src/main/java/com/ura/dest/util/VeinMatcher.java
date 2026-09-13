package com.ura.dest.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
            // 只取 forge:ores/<type>，跳过 forge:ores_in_ground/*
            if (!loc.getPath().startsWith("ores/")) continue;
            for (var holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
                result.add(holder.value());
            }
        }
        result.remove(block);
        return Collections.unmodifiableSet(result);
    }
}