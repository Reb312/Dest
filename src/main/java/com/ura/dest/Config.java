package com.ura.dest;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class Config {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.IntValue BLOCK_LIMIT;
    public static final ForgeConfigSpec.BooleanValue CONSUME_DURABILITY;
    public static final ForgeConfigSpec.BooleanValue CONSUME_FOOD;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_TOOL;
    public static final ForgeConfigSpec.BooleanValue PUT_IN_INVENTORY;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.comment("Dest Config").push("common");

        BLOCK_LIMIT = builder
                .comment("单次破坏方块数")
                .defineInRange("blockLimit", 64, 1, 4096);

        CONSUME_DURABILITY = builder
                .comment("连锁消耗耐久值")
                .define("consumeDurabilityOnce", true);

        CONSUME_FOOD = builder
                .comment("连锁消耗饥饿值")
                .define("consumeFood", true);

        REQUIRE_TOOL = builder
                .comment("手持工具触发")
                .define("requireTool", false);

        PUT_IN_INVENTORY = builder
                .comment("优先纳入背包")
                .define("putInInventory", true);

        builder.pop();
        COMMON_SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }
}