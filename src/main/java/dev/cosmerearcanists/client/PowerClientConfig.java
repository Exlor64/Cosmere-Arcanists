package dev.cosmerearcanists.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PowerClientConfig {
    public enum PulseInstrument { BASS_DRUM, BASS, HARP, SNARE, HAT, BELL, CHIME, FLUTE }
    public static final ModConfigSpec.EnumValue<PulseInstrument> SEEKING_INSTRUMENT;
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HUD, HIDE_HUD, LINES, THROUGH_WALLS, PEWTER_HEARTS, TIN_ECHOES, TIN_MODELS, TIN_ZOOM, FUTURES, FUTURE_MODELS, FUTURE_WARNINGS, SEEKING_HUD, SEEKING_SOUNDS, EMOTION_HUD, EMOTION_TARGET, TIN_LIGHTMAP;
    public static final ModConfigSpec.IntValue HUD_X, HUD_Y, HOLD_TICKS, TIN_ECHO_LIMIT, SEEKING_LIMIT;
    public static final ModConfigSpec.DoubleValue LINE_WIDTH, TIN_WIDTH, TIN_OPACITY, FUTURE_OPACITY, SEEKING_VOLUME, TIN_BRIGHTNESS, TIN_WIDTH_SCALE, TIN_HOLD, TIN_PULSE_STRENGTH, TIN_WAVE_SPEED, FUTURE_PROMINENCE, HUD_SCALE, TIN_VISION_SCALE, TIN_VISION_FADE;
    static {
        var b = new ModConfigSpec.Builder();
        HUD = b.comment("Show or hide the entire blue power panel, including the secondary panel, text and icons.").define("selectedPowerIcon", true); LINES = b.define("blueLines", true);
        HIDE_HUD=b.comment("Hide the entire blue power indicator group. Selection menus and world emotion labels are separate.").define("hidePowerIndicator",false);
        PEWTER_HEARTS = b.define("pewterDragHearts", true);
        THROUGH_WALLS = b.define("linesThroughWalls", true);
        HUD_X = b.comment("Primary blue-panel anchor in scaled screen pixels; secondary moves with it. -1 docks to the default screen edge.").defineInRange("hudX", -1, -1, 8192);
        HUD_Y = b.defineInRange("hudY", -1, -1, 8192);
        HUD_SCALE=b.comment("Scale the entire blue primary/secondary panel group, including text and item icons.").defineInRange("hudScale",1.0,.25,3.0);
        HOLD_TICKS = b.comment("A shorter press cycles state; hold opens selection.").defineInRange("menuHoldTicks", 4, 1, 20);
        LINE_WIDTH = b.defineInRange("lineWidth", 2.0, 1.0, 5.0);
        b.push("tin");
        TIN_LIGHTMAP=b.define("variableNightVision",true);
        TIN_VISION_SCALE=b.defineInRange("visionBrightnessMultiplier",1.0,0.0,2.0);
        TIN_VISION_FADE=b.comment("Exponential transition time in ticks for changing vision strength.").defineInRange("visionTransitionTicks",6.0,1.0,40.0);
        TIN_ECHOES=b.define("soundWireframes",true);TIN_MODELS=b.define("entityModelWireframes",true);TIN_ZOOM=b.define("heldZoom",true);
        TIN_ECHO_LIMIT=b.defineInRange("maxVisibleEchoes",12,1,32);TIN_WIDTH=b.defineInRange("lineWidth",1.2,1.0,3.0);TIN_OPACITY=b.defineInRange("opacity",.65,.05,1.0);
        TIN_BRIGHTNESS=b.defineInRange("echoBrightnessMultiplier",1.35,.25,3.0);TIN_WIDTH_SCALE=b.defineInRange("echoLineWidthMultiplier",1.25,.5,3.0);
        TIN_PULSE_STRENGTH=b.defineInRange("pulseIntensityMultiplier",2.0,.25,4.0);TIN_WAVE_SPEED=b.comment("Blocks per tick for expanding sound waves.").defineInRange("waveSpeed",.65,.1,4.0);
        TIN_HOLD=b.comment("Keep full echo brightness for this fraction of its lifetime, then smoothly fade.").defineInRange("echoHoldFraction",.2,0.0,.75);
        b.pop();b.push("futureSight");
        FUTURES=b.define("shadows",true);FUTURE_MODELS=b.define("entitySilhouettes",true);FUTURE_WARNINGS=b.define("injuryWarnings",true);
        FUTURE_PROMINENCE=b.comment("Additional shadow/path visibility scale. Does not affect warning text.").defineInRange("prominenceMultiplier",.45,.05,1.0);
        FUTURE_OPACITY=b.defineInRange("opacity",.5,.05,1.0);
        b.pop();b.push("seeking");
        SEEKING_HUD=b.define("pulseCompassAndCopperStatus",true);SEEKING_SOUNDS=b.define("privateRhythms",true);
        SEEKING_INSTRUMENT=b.comment("Vanilla note-block instrument for private bronze pulses; rhythms/pitch still distinguish signals.").defineEnum("pulseInstrument",PulseInstrument.BASS_DRUM);
        SEEKING_LIMIT=b.defineInRange("maxVisiblePulses",16,1,64);SEEKING_VOLUME=b.defineInRange("rhythmVolume",.35,0.0,1.0);
        b.pop();EMOTION_TARGET=b.define("targetEmotionIndicator",true);EMOTION_HUD=b.define("emotionControlsAndStatus",true);SPEC = b.build();
    }
    private PowerClientConfig() { }
}
