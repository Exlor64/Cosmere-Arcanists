package dev.cosmerearcanists.power;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TinConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, NIGHT_VISION, ZOOM, HEARING, OBSTRUCTION, REQUIRE_FLARING;
    public static final ModConfigSpec.DoubleValue DRAIN, RANGE, MAX_RANGE, RADIUS, MAX_RADIUS, BLOCK_COST, HARDNESS_COST, HARDNESS_CAP, WOOL_COST, MIN_AUDIBILITY, ZOOM_STRENGTH, MAX_ZOOM, REVEAL_THRESHOLD, FULL_REVEAL, PERSISTENCE, ECHO_RANGE;
    public static final ModConfigSpec.IntValue FADE, MAX_FADE, SHAPES, EVENTS_PER_TICK, SOURCE_INTERVAL;
    static {
        var b=new ModConfigSpec.Builder();
        ENABLED=b.define("enabled",true);NIGHT_VISION=b.define("nightVision",true);ZOOM=b.define("heldZoom",true);HEARING=b.define("soundWireframes",true);
        DRAIN=b.defineInRange("reserveDrainPerSecond",1.0,0.0,100.0);
        ZOOM_STRENGTH=b.defineInRange("zoomPerStrength",1.5,0.0,10.0);MAX_ZOOM=b.defineInRange("maxZoom",8.0,1.0,20.0);
        b.push("hearing");
        ECHO_RANGE=b.comment("Multiply hearing distance/cap and revealed surface radius/cap. Existing saved base settings are retained.").defineInRange("echoRangeMultiplier",2.0,.25,4.0);
        REQUIRE_FLARING=b.define("wireframesRequireFlaring",true);
        REVEAL_THRESHOLD=b.comment("Effective strength must exceed this to reveal sound wireframes. Attribute 1 Low/Burning are below 1.5; Flaring reaches 2 for a weak glimpse.").defineInRange("wireframeStrengthThreshold",1.5,0.0,100.0);
        FULL_REVEAL=b.comment("Effective attribute times burn strength for full wireframe potential. Default 6: attribute 3 Flaring. Range/detail still obey their caps.").defineInRange("fullWireframeStrength",6.0,.01,1024.0);
        RANGE=b.comment("Base hearing distance at normal strength and volume. Scales with square roots of strength and volume.").defineInRange("range",24.0,1.0,128.0);
        MAX_RANGE=b.defineInRange("maxRange",64.0,1.0,128.0);
        RADIUS=b.defineInRange("surfaceRadius",3.0,.5,8.0);MAX_RADIUS=b.defineInRange("maxSurfaceRadius",6.0,1.0,8.0);
        PERSISTENCE=b.comment("Multiply sound echo lifetime and its cap; packets remain bounded to200ticks. Applies to existing fadeTicks settings too.").defineInRange("echoPersistenceMultiplier",1.4,.25,4.0);
        FADE=b.defineInRange("fadeTicks",24,4,100);MAX_FADE=b.defineInRange("maxFadeTicks",60,4,200);
        SHAPES=b.comment("Maximum exposed collision-shape boxes per sound; scales down at low strength. Loaded chunks only.").defineInRange("maxShapes",96,8,256);
        EVENTS_PER_TICK=b.defineInRange("maxEventsPerPlayerPerTick",2,1,8);SOURCE_INTERVAL=b.defineInRange("sameSourceIntervalTicks",4,1,40);
        OBSTRUCTION=b.define("blockObstruction",true);
        BLOCK_COST=b.comment("Every intervening colliding block adds this much effective hearing distance.").defineInRange("distanceCostPerBlock",2.0,0.0,100.0);
        HARDNESS_COST=b.defineInRange("distanceCostPerHardness",.75,0.0,20.0);HARDNESS_CAP=b.defineInRange("hardnessCap",10.0,0.0,100.0);
        WOOL_COST=b.defineInRange("extraWoolDistanceCost",12.0,0.0,128.0);
        MIN_AUDIBILITY=b.comment("Discard echoes weaker than this. Audible echoes lose opacity, radius, persistence and detail as obstruction increases.").defineInRange("minimumAudibility",.08,.001,.9);
        b.pop();SPEC=b.build();
    }
    private TinConfig() { }
}
