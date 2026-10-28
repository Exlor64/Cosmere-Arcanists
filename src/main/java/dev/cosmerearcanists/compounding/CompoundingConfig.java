package dev.cosmerearcanists.compounding;
import dev.cosmerearcanists.Metal;
import java.util.*;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class CompoundingConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,FORWARD,REVERSE,EDIBLE;
    public static final ModConfigSpec.DoubleValue GAIN,YIELD,FUEL,BOOST,SECONDS,DAMAGE;
    public static final Map<String,ModConfigSpec.BooleanValue> METALS=new HashMap<>();
    static{var b=new ModConfigSpec.Builder();ENABLED=b.define("enabled",true);FORWARD=b.define("allomancyIntoFeruchemy",true);REVERSE=b.define("chargedNuggetCompounding",true);EDIBLE=b.define("edibleNuggets",true);GAIN=b.comment("Feruchemical charge per Allomantic reserve point; no seed charge is required.").defineInRange("chargePerReserve",10.0,.001,10000.0);YIELD=b.comment("Multiplier on chargePerReserve, including existing world settings.0.1 is a90% output reduction; fuel use is unchanged.").defineInRange("chargeYieldMultiplier",.1,.001,100.0);FUEL=b.defineInRange("reservePerSecond",1.0,.001,10000.0);BOOST=b.comment("Full charged nugget bonus; magnitude and duration both scale with charge/capacity. Re-eating replaces, never stacks.").defineInRange("maximumAllomanticBonus",.5,0,10.0);SECONDS=b.defineInRange("maximumBoostSeconds",10.0,.05,3600.0);DAMAGE=b.defineInRange("nuggetDamage",2.0,0,1000.0);b.push("metals");for(var m:Metal.values())METALS.put(m.id(),b.define(m.id(),true));b.pop();SPEC=b.build();}
    public static double gain(){return GAIN.get()*YIELD.get();}
    private CompoundingConfig(){}
}
