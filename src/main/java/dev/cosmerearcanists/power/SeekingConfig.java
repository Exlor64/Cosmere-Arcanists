package dev.cosmerearcanists.power;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SeekingConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, BRONZE, COPPER, DETECTION, MASKING, OTHER_ARTS, PIERCING, EMOTIONAL, EMOTION_PIERCING, IDENTIFY, FOCUS;
    public static final ModConfigSpec.DoubleValue BRONZE_DRAIN, COPPER_DRAIN, RANGE, RANGE_CAP, CLOUD_RADIUS, CLOUD_CAP, PIERCE_RATIO, EMOTION_RATIO, ID_STRENGTH, DETAIL_STRENGTH, MIN_SIGNAL;
    public static final ModConfigSpec.IntValue INTERVAL, MAX_SIGNALS, MAX_EMISSIONS;
    static {
        var b=new ModConfigSpec.Builder();ENABLED=b.define("enabled",true);BRONZE=b.define("bronzeEnabled",true);COPPER=b.define("copperEnabled",true);
        BRONZE_DRAIN=b.defineInRange("bronzeReservePerSecond",.5,0.0,100.0);COPPER_DRAIN=b.defineInRange("copperReservePerSecond",.25,0.0,100.0);
        b.push("bronze");DETECTION=b.define("detectSignals",true);OTHER_ARTS=b.define("detectOtherInvestedArts",true);IDENTIFY=b.define("identifyArtsAndEffects",true);FOCUS=b.define("heldFocusDetails",true);
        RANGE=b.defineInRange("range",24.0,1.0,128.0);RANGE_CAP=b.defineInRange("maxRange",64.0,1.0,128.0);
        ID_STRENGTH=b.comment("Effective bronze strength divided by a signal's difficulty to identify its effect. Does not grant progression XP.").defineInRange("effectIdentificationStrength",1.0,.01,100.0);
        DETAIL_STRENGTH=b.defineInRange("burnDetailStrength",2.0,.01,100.0);MIN_SIGNAL=b.defineInRange("minimumSignal",.03,.001,.9);
        PIERCING=b.define("pierceCopperclouds",true);PIERCE_RATIO=b.comment("Bronze strength must exceed combined copper strength times this ratio. Clouds containing either source or listener count once each.").defineInRange("copperPiercingRatio",3.0,.1,100.0);b.pop();
        b.push("copper");MASKING=b.define("hideSignals",true);CLOUD_RADIUS=b.defineInRange("radius",12.0,1.0,64.0);CLOUD_CAP=b.defineInRange("maxRadius",32.0,1.0,64.0);
        EMOTIONAL=b.comment("Protect only the active copper burner via the shared EmotionalInfluenceEvent. Does not remove unrelated potion effects.").define("emotionalProtection",true);
        EMOTION_PIERCING=b.define("extremeEmotionalStrengthCanPierce",true);EMOTION_RATIO=b.comment("Incoming emotional metal effective strength must be AT LEAST this multiple of the copper burner strength. Includes attribute and burn level, before distance falloff. Replaces legacy emotionalPiercingRatio.").defineInRange("minimumEmotionalStrengthRatio",1.25,1.0,100.0);b.pop();
        INTERVAL=b.defineInRange("scanIntervalTicks",4,1,20);MAX_SIGNALS=b.defineInRange("maxDetectedSignals",32,1,64);MAX_EMISSIONS=b.comment("Server-side collection budget per dimension, including external arts. Never loads chunks.").defineInRange("maxEmissionsPerDimension",1024,32,4096);
        SPEC=b.build();
    }
    private SeekingConfig() { }
}
