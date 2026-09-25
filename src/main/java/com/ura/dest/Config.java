package com.ura.dest;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class Config {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.IntValue BLOCK_LIMIT;
    public static final ForgeConfigSpec.IntValue MAX_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue CONSUME_DURABILITY;
    public static final ForgeConfigSpec.BooleanValue CONSUME_FOOD;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_TOOL;
    public static final ForgeConfigSpec.BooleanValue PUT_IN_INVENTORY;
    public static final ForgeConfigSpec.ConfigValue<String> CONTOUR_COLOR;
    public static final ForgeConfigSpec.DoubleValue CONTOUR_THICKNESS;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.comment("Dest Config").push("common");

        BLOCK_LIMIT = builder
                .comment("单次破坏方块数")
                .defineInRange("blockLimit", 64, 1, 4096);

        MAX_DISTANCE = builder
                .comment("连锁最大距离")
                .defineInRange("maxDistance", 16, 1, 64);

        CONSUME_DURABILITY = builder
                .comment("连锁消耗耐久值")
                .define("consumeDurabilityOnce", true);

        CONSUME_FOOD = builder
                .comment("连锁消耗饥饿值")
                .define("consumeFood", true);

        REQUIRE_TOOL = builder
                .comment("必须手持工具才能触发")
                .define("requireTool", false);

        PUT_IN_INVENTORY = builder
                .comment("掉落物优先纳入背包")
                .define("putInInventory", true);

        CONTOUR_COLOR = builder
                .comment("轮廓线条颜色")
                .define("contourColor", "#FFFFFF");

        CONTOUR_THICKNESS = builder
                .comment("轮廓线条粗细")
                .defineInRange("contourThickness", 0.04D, 0.005D, 0.5D);

        builder.pop();
        COMMON_SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }
}