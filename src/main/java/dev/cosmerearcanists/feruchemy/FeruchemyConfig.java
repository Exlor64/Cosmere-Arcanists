package dev.cosmerearcanists.feruchemy;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class FeruchemyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,IRON,DENOMINATIONS,OWNERSHIP,CURIOS,FALL,DRAG,KNOCKBACK,MASS;
    public static final ModConfigSpec.DoubleValue CAPACITY,STORE_STEP,MAX_STORE,TAP_STEP,MAX_MASS,TAP_LOSS,TERMINAL;
    static{var b=new ModConfigSpec.Builder();ENABLED=b.define("enabled",true);IRON=b.define("iron",true);
        DENOMINATIONS=b.comment("Allow single nuggets, ingots and storage-block items as metalminds. Stacks of two or more are always rejected.").define("denominationMetalminds",true);
        CURIOS=b.define("curiosCompatibility",true);OWNERSHIP=b.define("requireOwner",true);CAPACITY=b.comment("Full-normal-attribute seconds per nugget of metal. Ingot/bracelet/necklace =9 times; block =81 times. Captured item capacity remains stable if settings change.").defineInRange("capacityPerNugget",600.0,1.0,1000000.0);
        STORE_STEP=b.defineInRange("ironStoreFractionPerLevel",.1,.001,.9);MAX_STORE=b.defineInRange("ironMaximumStoredFraction",.9,.01,.99);
        TAP_STEP=b.defineInRange("ironExtraMassPerTapLevel",.5,.001,10.0);MAX_MASS=b.defineInRange("ironMaximumMassMultiplier",20.0,1.0,100.0);
        TAP_LOSS=b.comment("Compression cost = extraMass * max(1,extraMass)^exponent. Zero disables compression loss.").defineInRange("ironTapCompressionExponent",.25,0.0,2.0);
        MASS=b.define("affectAllomanticMass",true);KNOCKBACK=b.define("affectKnockback",true);FALL=b.define("reduceLightweightFallDamage",true);DRAG=b.define("lightweightAirResistance",true);
        TERMINAL=b.comment("Approximate baseline terminal fall speed; lighter bodies reach a lower cap without changing gravitational acceleration.").defineInRange("baselineTerminalFallSpeed",3.92,.1,10.0);SPEC=b.build();}
    private FeruchemyConfig(){}
}
