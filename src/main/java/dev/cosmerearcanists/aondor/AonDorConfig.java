package dev.cosmerearcanists.aondor;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class AonDorConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue LIGHT_SECONDS,LIGHT_LEVEL,THAW,CAST_COOLDOWN,MAX_TARGETS;
    public static final ModConfigSpec.DoubleValue RANGE,FORCE,MAX_FORCE_VECTOR;
    public static final ModConfigSpec.DoubleValue SHAPE_ERROR,SHAPE_GAP,FALLBACK_POTENCY,ERROR,GAP,MINIMUM_EXTENT,PATH_WEIGHT,ENDPOINT_WEIGHT,CORNER_WEIGHT,LENGTH_WEIGHT;
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
        b.push("baseEffects");
        MAX_TARGETS=b.defineInRange("maximumTargets",32,1,128);
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
