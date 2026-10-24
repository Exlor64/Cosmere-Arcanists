package dev.cosmerearcanists.power;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class EnhancementConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,ALUMINUM,DURALUMIN,PURGE,CLEANSE,FERUCHEMY,OVERCAPS;
    public static final ModConfigSpec.DoubleValue LOW_POWER,BURN_POWER,FLARE_POWER,LOW_DRAIN,BURN_DRAIN,DRAIN,POWER_GROWTH,EFFICIENCY_GROWTH;
    public static final ModConfigSpec.IntValue FLARE_TICKS;
    static {var b=new ModConfigSpec.Builder();ENABLED=b.define("enabled",true);ALUMINUM=b.define("aluminumEnabled",true);DURALUMIN=b.define("duraluminEnabled",true);
        FERUCHEMY=b.define("duraluminAmplifiesFeruchemicalTapping",true);OVERCAPS=b.define("duraluminExceedsOrdinaryEffectCaps",true);
        PURGE=b.define("aluminumEmptiesReserves",true);CLEANSE=b.define("aluminumCleansesForeignInvestiture",true);
        LOW_POWER=b.defineInRange("lowBurnStrengthMultiplier",1.75,1.0,100.0);BURN_POWER=b.defineInRange("burnStrengthMultiplier",2.5,1.0,100.0);FLARE_POWER=b.defineInRange("flareStrengthMultiplier",5.0,1.0,100.0);
        LOW_DRAIN=b.defineInRange("lowBurnDrainMultiplier",5.0,1.0,1000.0);BURN_DRAIN=b.defineInRange("burnDrainMultiplier",10.0,1.0,1000.0);
        FLARE_TICKS=b.comment("Per active metal: ticks times its current reserves divided by max reserves. Minimum1tick. A committed burst keeps consuming its captured fuel if toggled off.").defineInRange("fullReserveFlareTicks",100,1,1200);
        POWER_GROWTH=b.defineInRange("bonusPowerPerAttributeDoubling",.10,0.0,1.0);EFFICIENCY_GROWTH=b.defineInRange("efficiencyPerAttributeDoubling",.5,0.0,5.0);
        DRAIN=b.comment("Duralumin own baseline drain per second during low/normal use. Other metals use their own burn rates times the configured multipliers.").defineInRange("duraluminReservePerSecond",1.0,0.0,100.0);SPEC=b.build();}
    private EnhancementConfig() { }
}
