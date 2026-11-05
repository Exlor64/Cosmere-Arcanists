package dev.cosmerearcanists.aondor;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class AonDorConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
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
        FALLBACK_POTENCY=b.comment("Potency multiplier shown for shape-only matches; this drawing test does not cast spells.").defineInRange("shapeOnlyPotency",.75,.05,1.0);
        GAP=b.comment("Reject competing shapes whose error scores are closer than this.").defineInRange("minimumRecognitionGap",.025,.001,.2);SPEC=b.build();}
    public static AonRecognition.Tolerance tolerance(){return new AonRecognition.Tolerance(ERROR.get(),GAP.get(),MINIMUM_EXTENT.get(),PATH_WEIGHT.get(),ENDPOINT_WEIGHT.get(),CORNER_WEIGHT.get(),LENGTH_WEIGHT.get());}
    private AonDorConfig(){}
}
