package dev.cosmerearcanists.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PowerClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HUD, LINES, THROUGH_WALLS, PEWTER_HEARTS, TIN_ECHOES, TIN_MODELS, TIN_ZOOM, FUTURES, FUTURE_MODELS, FUTURE_WARNINGS;
    public static final ModConfigSpec.IntValue HUD_X, HUD_Y, HOLD_TICKS, TIN_ECHO_LIMIT;
    public static final ModConfigSpec.DoubleValue LINE_WIDTH, TIN_WIDTH, TIN_OPACITY, FUTURE_OPACITY;
    static {
        var b = new ModConfigSpec.Builder();
        HUD = b.define("selectedPowerIcon", true); LINES = b.define("blueLines", true);
        PEWTER_HEARTS = b.define("pewterDragHearts", true);
        THROUGH_WALLS = b.define("linesThroughWalls", true);
        HUD_X = b.comment("HUD coordinates in scaled pixels; -1 uses the default.").defineInRange("hudX", -1, -1, 8192);
        HUD_Y = b.defineInRange("hudY", -1, -1, 8192);
        HOLD_TICKS = b.comment("A shorter press cycles state; hold opens selection.").defineInRange("menuHoldTicks", 4, 1, 20);
        LINE_WIDTH = b.defineInRange("lineWidth", 2.0, 1.0, 5.0);
        b.push("tin");
        TIN_ECHOES=b.define("soundWireframes",true);TIN_MODELS=b.define("entityModelWireframes",true);TIN_ZOOM=b.define("heldZoom",true);
        TIN_ECHO_LIMIT=b.defineInRange("maxVisibleEchoes",12,1,32);TIN_WIDTH=b.defineInRange("lineWidth",1.2,1.0,3.0);TIN_OPACITY=b.defineInRange("opacity",.65,.05,1.0);
        b.pop();b.push("futureSight");
        FUTURES=b.define("shadows",true);FUTURE_MODELS=b.define("entitySilhouettes",true);FUTURE_WARNINGS=b.define("injuryWarnings",true);
        FUTURE_OPACITY=b.defineInRange("opacity",.5,.05,1.0);
        b.pop();SPEC = b.build();
    }
    private PowerClientConfig() { }
}
