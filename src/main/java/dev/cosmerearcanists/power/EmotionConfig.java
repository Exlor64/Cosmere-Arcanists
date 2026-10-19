package dev.cosmerearcanists.power;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EmotionConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,ZINC,BRASS,PLAYERS,MOBS,ANIMALS,AREA,ANGER,FEAR,TRUST,LOVE,COMPLIANCE,BREEDING,TRADES,ALUMINUM,DOMINATION;
    public static final ModConfigSpec.DoubleValue DRAIN,RANGE,RANGE_CAP,ANIMAL_FACTOR,ANGER_DAMAGE,FEAR_DAMAGE,FEAR_ATTACK_SPEED,FEAR_SPEED,LOVE_REDUCTION,DISCOUNT,COMPLIANCE_FORCE,COMPLIANCE_CAP,DOMINATION_STRENGTH,MAX_INTEGRITY;
    public static final ModConfigSpec.IntValue DURATION,TARGETS,AFFECTED,FEAR_BURST,FEAR_COOLDOWN,MINION_TICKS,MINION_CAP;
    static {
        var b=new ModConfigSpec.Builder();ENABLED=b.define("enabled",true);ZINC=b.define("zincEnabled",true);BRASS=b.define("brassEnabled",true);
        PLAYERS=b.define("affectPlayers",true);MOBS=b.define("affectMobs",true);ANIMALS=b.define("affectAnimals",true);AREA=b.define("alternateAreaInfluence",true);
        ALUMINUM=b.comment("Aluminum-containing headwear or emotion_shielding item tags block influence; copper's existing emotional gateway also applies.").define("aluminumHeadwearProtection",true);
        DRAIN=b.defineInRange("reservePerSecond",1.0,0.0,100.0);RANGE=b.defineInRange("range",16.0,1.0,64.0);RANGE_CAP=b.defineInRange("maxRange",32.0,1.0,64.0);
        ANIMAL_FACTOR=b.comment("Animals respond, but less strongly than other mobs.").defineInRange("animalStrengthMultiplier",.5,.01,1.0);
        DURATION=b.defineInRange("lingerTicks",80,5,600);TARGETS=b.defineInRange("maxTargetsPerUse",16,1,64);AFFECTED=b.defineInRange("maxAffectedEntities",256,16,2048);
        b.push("effects");ANGER=b.define("anger",true);FEAR=b.define("fear",true);TRUST=b.define("trust",true);LOVE=b.define("love",true);COMPLIANCE=b.define("compliance",true);
        BREEDING=b.define("loveBreeding",true);TRADES=b.define("trustTradeDiscounts",true);
        ANGER_DAMAGE=b.defineInRange("playerAngerDamagePerStrength",.25,0.0,1.0);FEAR_DAMAGE=b.defineInRange("playerFearDamageReductionPerStrength",.30,0.0,.9);
        FEAR_ATTACK_SPEED=b.defineInRange("playerFearAttackSpeedReductionPerStrength",.30,0.0,.9);FEAR_SPEED=b.defineInRange("playerFearSpeedBurstPerStrength",.40,0.0,2.0);
        FEAR_BURST=b.defineInRange("fearBurstTicks",40,1,200);FEAR_COOLDOWN=b.defineInRange("fearBurstCooldownTicks",100,20,600);
        LOVE_REDUCTION=b.comment(".125 per effective strength: baseline attribute1 Flaring strength2 reaches25 percent against the influencing player.").defineInRange("loveDamageReductionPerStrength",.125,0.0,.5);
        DISCOUNT=b.defineInRange("trustDiscountPerStrength",.15,0.0,.5);
        COMPLIANCE_FORCE=b.defineInRange("playerComplianceImpulsePerStrength",.003,0.0,.02);COMPLIANCE_CAP=b.defineInRange("maxPlayerComplianceImpulse",.02,0.0,.05);
        b.pop();b.push("domination");DOMINATION=b.define("enabled",true);
        DOMINATION_STRENGTH=b.comment("Effective positive compliance strength needed to seize a sufficiently damaged spirit. Includes burn level, distance and animal response.").defineInRange("minimumStrength",4.0,.1,1024.0);
        MAX_INTEGRITY=b.comment("Spiritual integrity is1 for intact spirits; Hemalurgy integration can lower it via SpiritualIntegrity API.").defineInRange("maximumSpiritualIntegrity",.5,0.0,1.0);
        MINION_TICKS=b.defineInRange("baseDurationTicks",1200,20,72000);MINION_CAP=b.defineInRange("maximumDurationTicks",12000,20,72000);
        b.pop();SPEC=b.build();
    }
    public static boolean effect(Emotion e){return switch(e){case ANGER->ANGER.get();case FEAR->FEAR.get();case TRUST->TRUST.get();case LOVE->LOVE.get();case COMPLIANCE->COMPLIANCE.get();};}
    private EmotionConfig() { }
}
