package com.p1nero.lmm;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec.DoubleValue YANG_JIAN_HEALTH;
    public static final ForgeConfigSpec.DoubleValue YANG_JIAN_MOVE_SPEED;
    public static final ForgeConfigSpec.BooleanValue PLAY_BGM;
    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        YANG_JIAN_HEALTH = null;
        YANG_JIAN_MOVE_SPEED = null;
        PLAY_BGM = builder
                .comment("Enable play boss fight music")
                .define("play_bgm", true);
        SPEC = builder.build();
    }
}
