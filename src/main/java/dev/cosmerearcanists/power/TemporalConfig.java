package dev.cosmerearcanists.power;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Bounded short-term forecasts, not a simulation of unknown future player inputs. */
public final class TemporalConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, ATIUM, ELECTRUM, SHADOWS, WARNINGS, COUNTERS, JUMP, PROJECTILES, ATTACKS, REQUIRE_HOLD, AGGRO, DODGE;
    public static final ModConfigSpec.DoubleValue ATIUM_DRAIN, ELECTRUM_DRAIN, RANGE, MAX_RANGE, BASE_HORIZON, MAX_HORIZON, DODGE_CHANCE, DODGE_CAP;
    public static final ModConfigSpec.IntValue INTERVAL, TARGETS, SAMPLES;
    static {
        var b=new ModConfigSpec.Builder();
        ENABLED=b.define("enabled",true);ATIUM=b.comment("The existing atium items/reserve represent the atium-electrum alloy. No pure-atium effect is granted.").define("atium",true);
        ELECTRUM=b.define("electrum",true);SHADOWS=b.define("futureShadows",true);WARNINGS=b.define("injuryWarnings",true);
        COUNTERS=b.comment("Atium/electrum burners split an atium observer's predictions; no reliable central future or hit warning is sent for them.").define("futureSightInterference",true);
        REQUIRE_HOLD=b.define("requireHeldActivation",true);AGGRO=b.define("electrumAggroWarnings",true);
        JUMP=b.define("heldElectrumJumpPreview",true);PROJECTILES=b.define("projectileForecasts",true);ATTACKS=b.define("mobAttackIntentWarnings",true);
        ATIUM_DRAIN=b.defineInRange("atiumReservePerSecond",4.0,0.0,100.0);ELECTRUM_DRAIN=b.defineInRange("electrumReservePerSecond",1.0,0.0,100.0);
        RANGE=b.defineInRange("atiumRange",24.0,1.0,64.0);MAX_RANGE=b.defineInRange("maxAtiumRange",48.0,1.0,64.0);
        BASE_HORIZON=b.comment("Ticks ahead at attribute 1 Burning. Strength gives a modest logarithmic increase; never long-term prophecy.").defineInRange("baseHorizonTicks",20.0,4.0,60.0);
        MAX_HORIZON=b.defineInRange("maxHorizonTicks",40.0,4.0,60.0);
        INTERVAL=b.defineInRange("updateIntervalTicks",2,1,10);TARGETS=b.defineInRange("maxTargets",12,1,24);SAMPLES=b.defineInRange("samplesPerShadow",6,2,8);
        b.push("atiumDodge");
        DODGE=b.define("enabled",true);
        DODGE_CHANCE=b.comment("Chance at attribute 1 while flaring. Scales with square root of relative effective strength. Requires active future sight; environmental damage cannot be dodged.").defineInRange("baseChance",.10,0.0,1.0);
        DODGE_CAP=b.defineInRange("maxChance",.25,0.0,1.0);
        b.pop();
        SPEC=b.build();
    }
    private TemporalConfig() { }
}
