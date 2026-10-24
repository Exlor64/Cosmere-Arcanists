package dev.cosmerearcanists.nicrosil;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class NicrosilConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,POWERS,RESOURCES,SELF,TEMPORARY;
    public static final ModConfigSpec.DoubleValue POWER_CHARGE,RESOURCE_BATCH,POWER_RATE,TEMP_COST;
    public static final ModConfigSpec.IntValue ENTRIES,MINDS,INTERVAL;
    static{var b=new ModConfigSpec.Builder();ENABLED=b.define("enabled",true);TEMPORARY=b.define("temporaryCompoundedPowers",true);TEMP_COST=b.comment("Charge consumed per strength-second by a temporary nicrosil power. Losing contact pauses access; it never becomes a permanent base attribute.").defineInRange("temporaryChargePerStrengthSecond",1.0,.001,10000.0);POWERS=b.define("permanentPowerTransfers",true);RESOURCES=b.define("investitureResourceTransfers",true);SELF=b.comment("Storing Feruchemical nicrosil itself requires an unkeyed destination, so its donor can still reclaim it without the power.").define("allowStoringNicrosilItself",true);POWER_CHARGE=b.comment("Capacity cost per unit of a power's unmodified personal strength. Captured contents keep their original cost; tapping does not amplify permanent powers.").defineInRange("capacityPerPowerStrength",100.0,.001,1000000.0);POWER_RATE=b.comment("Personal power strength moved per real second while the chosen power is being stored. At 0.01, one normal-strength power takes 100 seconds. Cadmium local time can accelerate storage without multiplying strength.").defineInRange("powerStrengthStoredPerSecond",.01,.000001,1024.0);RESOURCE_BATCH=b.defineInRange("maximumResourceUnitsPerAction",100.0,.001,1000000.0);ENTRIES=b.defineInRange("maximumEntriesPerMetalmind",32,1,NicrosilLedger.HARD_ENTRIES);MINDS=b.defineInRange("maximumStoredMetalminds",16384,1,1000000);INTERVAL=b.defineInRange("minimumTicksBetweenMenuActions",5,1,100);SPEC=b.build();}
    private NicrosilConfig(){}
}
