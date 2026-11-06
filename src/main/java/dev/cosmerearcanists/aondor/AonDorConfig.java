package dev.cosmerearcanists.aondor;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class AonDorConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,ACCESS,HEAT_IGNITE,HEAT_DAMAGE,HEAT_COOK,HEAT_SMELT;
    public static final ModConfigSpec.IntValue LIGHT_SECONDS,LIGHT_LEVEL,GLOW_SECONDS,THAW,CAST_COOLDOWN,MAX_TARGETS,MEMORY_SECONDS,MEMORY_ENTRIES,HEAT_ITEMS;
    public static final ModConfigSpec.DoubleValue RANGE,FORCE,MAX_FORCE_VECTOR,HEAT_MAX_POWER,HEAT_IGNITE_POWER,HEAT_DAMAGE_POWER,HEAT_COOK_POWER,HEAT_SMELT_POWER,HEAT_DAMAGE_RATE,HEAT_DAMAGE_CAP,CONNECTION_RATIO,CONNECTION_CAP,CONNECTION_SECONDS,COST_SCALE;
    public static final ModConfigSpec.DoubleValue SHAPE_ERROR,SHAPE_GAP,FALLBACK_POTENCY,ERROR,GAP,MINIMUM_EXTENT,PATH_WEIGHT,ENDPOINT_WEIGHT,CORNER_WEIGHT,LENGTH_WEIGHT;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DOR_DIMENSIONS;
    static {var b=new ModConfigSpec.Builder();ENABLED=b.define("enabled",true);
        ERROR=b.comment("Maximum normalized stroke error; for the precise stroke-order/direction recognition pass.").defineInRange("maximumRecognitionError",.12,.01,.4);
        MINIMUM_EXTENT=b.comment("Smallest glyph extent as a fraction of its tile; this rejects taps, not differences in drawing size.").defineInRange("minimumGlyphExtent",.01,.001,.1);
        PATH_WEIGHT=b.defineInRange("pathWeight",.55,.1,10.0);
        ENDPOINT_WEIGHT=b.defineInRange("endpointWeight",.25,0.0,10.0);
        CORNER_WEIGHT=b.defineInRange("cornerWeight",.1,0.0,10.0);
        LENGTH_WEIGHT=b.comment("Compares relative stroke lengths rather than absolute drawing size.").defineInRange("relativeStrokeLengthWeight",.1,0.0,10.0);
        SHAPE_ERROR=b.comment("Maximum combined-ink error for shape-only fallback.").defineInRange("maximumShapeError",.065,.005,.2);
        SHAPE_GAP=b.defineInRange("minimumShapeGap",.018,.001,.2);
        FALLBACK_POTENCY=b.comment("Potency multiplier shown for shape-only matches; applies to standalone spell effects.").defineInRange("shapeOnlyPotency",.75,.05,1.0);
        b.push("investiture");
        ACCESS=b.comment("Require Sel ambient Dor or both high Connection and a registered external source. Disable only for explicit development/testing worlds.").define("enforceAccess",true);
        DOR_DIMENSIONS=b.defineListAllowEmpty("ambientDorDimensions",java.util.List.of("cosmerearcanists:sel"),()->"cosmerearcanists:sel",v->v instanceof String s&&net.minecraft.resources.ResourceLocation.tryParse(s)!=null);
        CONNECTION_RATIO=b.defineInRange("offworldConnectionRatio",3.0,1.0,1000.0);CONNECTION_CAP=b.defineInRange("maximumAonConnectionMultiplier",32.0,1.0,1000.0);CONNECTION_SECONDS=b.defineInRange("maximumAonConnectionSeconds",3600.0,1.0,86400.0);COST_SCALE=b.defineInRange("workCostScale",1.0,.000001,1000000.0);b.pop();
        b.push("memory");MEMORY_SECONDS=b.defineInRange("temporarySeconds",60,1,3600);MEMORY_ENTRIES=b.defineInRange("maximumEntries",64,1,256);b.pop();
        b.push("heat");
        HEAT_IGNITE=b.define("ignition",true);HEAT_DAMAGE=b.define("fireDamage",true);HEAT_COOK=b.define("cooking",true);HEAT_SMELT=b.define("smelting",true);
        HEAT_MAX_POWER=b.defineInRange("maximumEffectivePower",0.0,0.0,Double.MAX_VALUE);
        HEAT_IGNITE_POWER=b.defineInRange("ignitionPower",1.0,.01,1024.0);HEAT_DAMAGE_POWER=b.defineInRange("damagePower",2.0,.01,1024.0);
        HEAT_COOK_POWER=b.defineInRange("cookingPower",4.0,.01,1024.0);HEAT_SMELT_POWER=b.defineInRange("smeltingPower",8.0,.01,1024.0);
        HEAT_DAMAGE_RATE=b.defineInRange("damagePerEffectivePower",1.0,0.0,100.0);HEAT_DAMAGE_CAP=b.defineInRange("maximumDirectDamage",0.0,0.0,Float.MAX_VALUE);
        HEAT_ITEMS=b.comment("Maximum entire dropped stack processed per target per cast; oversized outputs reject before any effects.").defineInRange("maximumStackProcessed",64,1,99);b.pop();
        b.push("baseEffects");
        MAX_TARGETS=b.defineInRange("maximumTargets",32,1,128);
        GLOW_SECONDS=b.defineInRange("entityGlowingSeconds",20,1,600);
        LIGHT_SECONDS=b.defineInRange("lightSeconds",20,1,120);
        LIGHT_LEVEL=b.defineInRange("lightLevel",12,1,15);
        RANGE=b.defineInRange("range",8.0,1.0,32.0);
        MAX_FORCE_VECTOR=b.comment("Reject larger explicit Force vectors before affecting any target; vectors are never silently normalized.").defineInRange("maximumForceVectorMagnitude",8.0,.1,64.0);
        FORCE=b.defineInRange("forceImpulse",.45,.01,2.0);
        THAW=b.defineInRange("thawTicks",140,1,1000);
        CAST_COOLDOWN=b.defineInRange("castCooldownTicks",20,1,1200);b.pop();
        GAP=b.comment("Reject competing shapes whose error scores are closer than this.").defineInRange("minimumRecognitionGap",.025,.001,.2);SPEC=b.build();}
    public static AonRecognition.Tolerance tolerance(){return new AonRecognition.Tolerance(ERROR.get(),GAP.get(),MINIMUM_EXTENT.get(),PATH_WEIGHT.get(),ENDPOINT_WEIGHT.get(),CORNER_WEIGHT.get(),LENGTH_WEIGHT.get());}
    private AonDorConfig(){}
}
