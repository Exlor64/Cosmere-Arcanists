package dev.cosmerearcanists.power;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Zero-valued effect settings disable individual effects without deleting saved debt. */
public final class PewterConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, DRAG, REST, SATURATION;
    public static final ModConfigSpec.DoubleValue SPEED, ATTACK, EXHAUSTION, MITIGATION, BASE_CAP, STRONG_CAP,
        DRAG_FRACTION, REST_FRACTION, SATURATION_RATE, SATURATION_RECOVERY, HEAL, DRAIN, MAX_SPEED_BONUS, MAX_ATTACK_BONUS;
    public static final ModConfigSpec.IntValue REST_TICKS, HEAL_TICKS;
    static {
        var b=new ModConfigSpec.Builder();
        ENABLED=b.define("enabled",true);
        SPEED=b.comment("Movement speed fraction added per effective strength. 0 disables.").defineInRange("speedPerStrength",.20,0.0,5.0);
        MAX_SPEED_BONUS=b.defineInRange("maxSpeedBonus",3.0,0.0,10.0);
        ATTACK=b.comment("Additional melee attack damage per effective strength. 0 disables.").defineInRange("attackDamagePerStrength",3.0,0.0,100.0);
        MAX_ATTACK_BONUS=b.defineInRange("maxAttackDamageBonus",100.0,0.0,1000.0);
        EXHAUSTION=b.comment("Fraction of exhaustion prevented per effective strength; capped at 90%. 0 disables.").defineInRange("exhaustionReductionPerStrength",.50,0.0,.90);
        MITIGATION=b.defineInRange("mitigationPerStrength",.25,0.0,.70);
        BASE_CAP=b.comment("Hard-limited to 50% at attributes <= 1, regardless of burn/config.").defineInRange("baselineMitigationCap",.50,0.0,.50);
        STRONG_CAP=b.comment("Hard-limited to 70% at attributes > 1.").defineInRange("strongMitigationCap",.70,0.0,.70);
        HEAL=b.comment("Health points healed per interval per effective strength; 2 points = one heart. 0 disables.").defineInRange("healingPerInterval",1.0,0.0,20.0);
        HEAL_TICKS=b.defineInRange("healingIntervalTicks",200,20,12000);
        DRAIN=b.defineInRange("reserveDrainPerSecond",1.0,0.0,100.0);
        b.push("drag");
        DRAG=b.comment("When disabled, saved debt is frozen, not erased or charged.").define("enabled",true);
        DRAG_FRACTION=b.comment("Fraction of post-mitigation, post-absorption health damage deferred. Fixed across burn levels.").defineInRange("damageFraction",.10,0.0,.90);
        REST=b.define("restRecovery",true);
        REST_FRACTION=b.comment("Fraction of remaining debt cleared per continuous rest interval, times effective strength.").defineInRange("restRecoveryFraction",.01,0.0,1.0);
        REST_TICKS=b.defineInRange("restIntervalTicks",100,20,12000);
        SATURATION=b.define("saturationRecovery",true);
        SATURATION_RATE=b.comment("Saturation spent per second while debt exists, times effective strength.").defineInRange("saturationPerSecond",.20,0.0,20.0);
        SATURATION_RECOVERY=b.comment("Health points of debt cleared per saturation point spent.").defineInRange("recoveryPerSaturation",1.0,0.0,100.0);
        b.pop(); SPEC=b.build();
    }
    private PewterConfig() { }
}
