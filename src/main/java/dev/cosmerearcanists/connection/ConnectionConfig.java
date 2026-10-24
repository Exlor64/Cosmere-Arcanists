package dev.cosmerearcanists.connection;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Independent server controls for Connection storage, social effects and persistent relationships. */
public final class ConnectionConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,TRADES,DETECTION,ANIMALS,BONDS,SENSING;
    public static final ModConfigSpec.DoubleValue STORE_PER_ATTRIBUTE,STORE_STEP,TAP_LEVELS,TAP_STEP,CAP,LOSS,DISCOUNT_STEP,DISCOUNT_CAP,STORE_PRICE,DETECTION_REDUCTION,ANIMAL_STEP,ANIMAL_CAP,PROXIMITY_RANGE,PROXIMITY_GAIN,INTERACTION_GAIN,INTERACTION_DIMINISH,FIRST_SENSE,REQUIRED_BOND,WEAKER_BONDS,BASE_RANGE,RANGE_STEP,RANGE_CAP;
    public static final ModConfigSpec.IntValue INTERACTION_COOLDOWN,IDLE_SECONDS,MAX_BONDS,MAX_PAIRS,MAX_SENSED;
    static {
        var b=new ModConfigSpec.Builder();
        ENABLED=b.define("enabled",true);TRADES=b.define("villagerPrices",true);DETECTION=b.define("storageReducesInitialHostileDetection",true);ANIMALS=b.define("tappingReducesAnimalAvoidance",true);BONDS=b.define("organicPlayerConnections",true);SENSING=b.define("senseConnectedPlayers",true);
        STORE_PER_ATTRIBUTE=b.defineInRange("storageFractionPerAttribute",.3,.001,10.0);STORE_STEP=b.defineInRange("storeFractionPerLevel",.1,.001,1.0);TAP_LEVELS=b.defineInRange("tapLevelsPerAttribute",10.0,.1,1000.0);TAP_STEP=b.defineInRange("connectionPerTapLevel",.15,.001,10.0);CAP=b.defineInRange("maximumConnectionMultiplier",20.0,1.0,100.0);LOSS=b.defineInRange("tapCompressionExponent",.25,0.0,2.0);
        DISCOUNT_STEP=b.defineInRange("discountPerFundedTapLevel",.02,0.0,.5);DISCOUNT_CAP=b.defineInRange("maximumDiscount",.25,0.0,.75);STORE_PRICE=b.defineInRange("pricePenaltyAtFullStorage",.5,0.0,2.0);
        DETECTION_REDUCTION=b.defineInRange("initialDetectionReductionAtFullStorage",.8,0.0,.95);ANIMAL_STEP=b.defineInRange("animalAvoidanceReductionPerTapLevel",.05,0.0,.5);ANIMAL_CAP=b.defineInRange("maximumAnimalAvoidanceReduction",.75,0.0,.95);
        PROXIMITY_RANGE=b.defineInRange("nearbyConnectionRange",16.0,1.0,64.0);PROXIMITY_GAIN=b.comment("Bond points per minute together, maximum100. At least one player must have moved or interacted recently.").defineInRange("bondPointsPerNearbyMinute",1.0,.001,100.0);INTERACTION_GAIN=b.defineInRange("bondPointsPerInteraction",2.0,0.0,100.0);INTERACTION_DIMINISH=b.defineInRange("interactionDiminishingReturnsAtFullBond",.75,0.0,1.0);INTERACTION_COOLDOWN=b.comment("Shared per-pair cooldown for direct interaction, item sharing and combat; repeated clicks cannot accelerate it.").defineInRange("interactionCooldownTicks",600,20,72000);IDLE_SECONDS=b.defineInRange("nearbyActivityTimeoutSeconds",300,1,86400);
        MAX_BONDS=b.comment("New bonds stop at this per-player limit; existing bonds are preserved when lowered.").defineInRange("maximumBondsPerPlayer",64,1,256);MAX_PAIRS=b.defineInRange("maximumWorldBondPairs",16384,1,262144);
        FIRST_SENSE=b.defineInRange("firstSensingTapLevel",1.0,.001,100000.0);REQUIRED_BOND=b.defineInRange("requiredBondAtFirstSensingLevel",100.0,1.0,100.0);WEAKER_BONDS=b.defineInRange("weakerBondSensitivityPerExtraLevel",.25,0.0,10.0);BASE_RANGE=b.defineInRange("sensingBaseRange",32.0,1.0,4096.0);RANGE_STEP=b.defineInRange("sensingRangePerTapLevel",24.0,0.0,1024.0);RANGE_CAP=b.defineInRange("maximumSensingRange",512.0,1.0,4096.0);MAX_SENSED=b.defineInRange("maximumSensedPlayers",8,1,8);
        SPEC=b.build();
    }
    private ConnectionConfig(){}
}
