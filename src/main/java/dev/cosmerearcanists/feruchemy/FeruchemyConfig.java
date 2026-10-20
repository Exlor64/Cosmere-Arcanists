package dev.cosmerearcanists.feruchemy;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class FeruchemyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,IRON,DENOMINATIONS,OWNERSHIP,CURIOS,INVENTORY,FALL,DRAG,KNOCKBACK,MASS,CRITICAL,IDLE_STORAGE,STEEL,STEEL_ATTACK,STEEL_MINING,STEEL_CONSUMING,STEEL_STEP,PEWTER,PEWTER_BULK,PEWTER_JUMPS,PEWTER_MUSCLES;
    public static final ModConfigSpec.IntValue RING_SLOTS,BRACELET_SLOTS,NECKLACE_SLOTS,IDLE_SECONDS;
    public static final ModConfigSpec.DoubleValue CAPACITY,STORE_PER_ATTRIBUTE,TAP_LEVELS_PER_ATTRIBUTE,STORE_STEP,MAX_STORE,TAP_STEP,MAX_MASS,TAP_LOSS,TERMINAL,KNOCKBACK_STEP,KNOCKBACK_CAP,CRITICAL_STEP,STEEL_STORE_PER_ATTRIBUTE,STEEL_TAP_LEVELS,STEEL_STORE_STEP,STEEL_TAP_STEP,STEEL_CAP,STEEL_LOSS,STEEL_STEP_RATE,STEEL_STEP_CAP,PEWTER_STORE_PER_ATTRIBUTE,PEWTER_TAP_LEVELS,PEWTER_STORE_STEP,PEWTER_TAP_STEP,PEWTER_CAP,PEWTER_LOSS,PEWTER_BULK_THRESHOLD,PEWTER_BULK_RATE,PEWTER_BULK_CAP,PEWTER_JUMP_RATE,PEWTER_JUMP_CAP,PEWTER_LEAP_RATE,PEWTER_LEAP_CAP;
    static{var b=new ModConfigSpec.Builder();ENABLED=b.define("enabled",true);IRON=b.define("iron",true);
        PEWTER=b.define("pewter",true);PEWTER_BULK=b.define("pewterBulkSlowdown",true);PEWTER_JUMPS=b.define("pewterEnhancedJumps",true);PEWTER_MUSCLES=b.define("pewterMuscleVisuals",true);
        PEWTER_STORE_PER_ATTRIBUTE=b.defineInRange("pewterStorageFractionPerAttribute",.30,.001,1.0);PEWTER_TAP_LEVELS=b.defineInRange("pewterTapLevelsPerAttribute",10.0,.1,1000.0);PEWTER_STORE_STEP=b.defineInRange("pewterStoreFractionPerLevel",.1,.001,.9);PEWTER_TAP_STEP=b.defineInRange("pewterTapStrengthPerLevel",.15,.001,10.0);PEWTER_CAP=b.defineInRange("pewterMaximumStrengthMultiplier",20.0,1.0,100.0);PEWTER_LOSS=b.defineInRange("pewterTapCompressionExponent",.25,0.0,2.0);
        PEWTER_BULK_THRESHOLD=b.defineInRange("pewterBulkSlowdownThreshold",3.0,1.0,100.0);PEWTER_BULK_RATE=b.defineInRange("pewterBulkSlowdownPerExtraStrength",.04,0.0,1.0);PEWTER_BULK_CAP=b.defineInRange("pewterMaximumBulkSlowdown",.5,0.0,.95);
        PEWTER_JUMP_RATE=b.defineInRange("pewterJumpStrengthPerExtraStrength",.8,0.0,10.0);PEWTER_JUMP_CAP=b.defineInRange("pewterMaximumJumpVelocityMultiplier",2.5,1.0,10.0);PEWTER_LEAP_RATE=b.defineInRange("pewterLeapImpulsePerSqrtExtraStrength",.3,0.0,3.0);PEWTER_LEAP_CAP=b.defineInRange("pewterMaximumLeapImpulse",1.2,0.0,5.0);
        STEEL_CONSUMING=b.define("steelAffectsEatingAndDrinking",true);STEEL_STEP=b.define("steelStepAssist",true);STEEL_STEP_RATE=b.defineInRange("steelStepHeightPerExtraSpeed",.3,0.0,2.0);STEEL_STEP_CAP=b.defineInRange("steelMaximumExtraStepHeight",.4,0.0,2.0);
        STEEL=b.define("steel",true);STEEL_ATTACK=b.define("steelAffectsAttackSpeed",true);STEEL_MINING=b.define("steelAffectsMiningSpeed",true);
        STEEL_STORE_PER_ATTRIBUTE=b.defineInRange("steelStorageFractionPerAttribute",.30,.001,1.0);STEEL_TAP_LEVELS=b.defineInRange("steelTapLevelsPerAttribute",10.0,.1,1000.0);
        STEEL_STORE_STEP=b.defineInRange("steelStoreFractionPerLevel",.1,.001,.9);STEEL_TAP_STEP=b.defineInRange("steelTapSpeedPerLevel",.15,.001,10.0);
        STEEL_CAP=b.comment("Maximum total physical speed multiplier, before other mods' independent modifiers.").defineInRange("steelMaximumSpeedMultiplier",8.0,1.0,100.0);STEEL_LOSS=b.defineInRange("steelTapCompressionExponent",.25,0.0,2.0);
        IDLE_STORAGE=b.comment("Stop Feruchemy storing after standing still for the configured duration. Moving resets the timer; tapping is unaffected.").define("stopStoringWhenIdle",true);
        IDLE_SECONDS=b.defineInRange("idleStorageTimeoutSeconds",300,1,86400);
        DENOMINATIONS=b.comment("Allow single nuggets, ingots and storage-block items as metalminds. Stacks of two or more are always rejected.").define("denominationMetalminds",true);
        INVENTORY=b.comment("Allow storing and tapping from single matching items in the36main inventory/hotbar slots. Does not permit stacked items or nested containers. Worn and held pieces are used first.").define("inventoryMetalminds",false);
        RING_SLOTS=b.comment("Default ring count from this mod. Other mods/datapacks may add their own slot bonuses. Updates existing players without deleting charge.").defineInRange("defaultRingSlots",2,0,64);
        BRACELET_SLOTS=b.defineInRange("defaultBraceletSlots",2,0,64);NECKLACE_SLOTS=b.defineInRange("defaultNecklaceSlots",1,0,64);
        CURIOS=b.define("curiosCompatibility",true);OWNERSHIP=b.define("requireOwner",true);CAPACITY=b.comment("Full-normal-attribute seconds per nugget of metal. Ingot/bracelet/necklace =9 times; block =81 times. Captured item capacity remains stable if settings change.").defineInRange("capacityPerNugget",600.0,1.0,1000000.0);
        STORE_PER_ATTRIBUTE=b.comment("Maximum fraction of normal mass storable per attribute point, including fractional attributes; still bounded by maximumStoredAttributeFraction.").defineInRange("ironStorageFractionPerAttribute",.30,.001,1.0);
        TAP_LEVELS_PER_ATTRIBUTE=b.comment("Maximum tap level = floor(attribute * this value). Attribute1.5 permits level15 by default.").defineInRange("ironTapLevelsPerAttribute",10.0,.1,1000.0);
        STORE_STEP=b.defineInRange("ironStoreFractionPerLevel",.1,.001,.9);MAX_STORE=b.comment("Global Feruchemy storage safety ceiling; never more than95percent of an attribute may be stored.").defineInRange("maximumStoredAttributeFraction",.95,.01,.95);
        TAP_STEP=b.defineInRange("ironTapWeightPerLevel",.15,.001,10.0);MAX_MASS=b.defineInRange("ironTapMassCap",10000.0,1.0,1000000.0);
        TAP_LOSS=b.comment("Compression cost = extraMass * max(1,extraMass)^exponent. Zero disables compression loss.").defineInRange("ironTapCompressionExponent",.25,0.0,2.0);
        CRITICAL=b.define("ironCriticalDamage",true);CRITICAL_STEP=b.comment("Additional damage fraction per funded tap level, applied only to vanilla falling critical hits.").defineInRange("ironCriticalDamagePerLevel",.05,0.0,10.0);
        KNOCKBACK_STEP=b.defineInRange("ironKnockbackReductionPerLevel",.02,0.0,1.0);KNOCKBACK_CAP=b.defineInRange("ironMaximumKnockbackReduction",.8,0.0,1.0);
        MASS=b.define("affectAllomanticMass",true);KNOCKBACK=b.define("affectKnockback",true);FALL=b.define("reduceLightweightFallDamage",true);DRAG=b.define("lightweightAirResistance",true);
        TERMINAL=b.comment("Approximate baseline terminal fall speed; lighter bodies reach a lower cap without changing gravitational acceleration.").defineInRange("baselineTerminalFallSpeed",3.92,.1,10.0);SPEC=b.build();}
    private FeruchemyConfig(){}
}
