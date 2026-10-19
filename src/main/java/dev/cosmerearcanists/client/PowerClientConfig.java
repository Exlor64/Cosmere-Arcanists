package dev.cosmerearcanists.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PowerClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HUD, LINES, THROUGH_WALLS, PEWTER_HEARTS;
    public static final ModConfigSpec.IntValue HUD_X, HUD_Y, HOLD_TICKS;
    public static final ModConfigSpec.DoubleValue LINE_WIDTH;
    static {
        var b = new ModConfigSpec.Builder();
        HUD = b.define("selectedPowerIcon", true); LINES = b.define("blueLines", true);
        PEWTER_HEARTS = b.define("pewterDragHearts", true);
        THROUGH_WALLS = b.define("linesThroughWalls", true);
        HUD_X = b.comment("HUD coordinates in scaled pixels; -1 uses the default.").defineInRange("hudX", -1, -1, 8192);
        HUD_Y = b.defineInRange("hudY", -1, -1, 8192);
        HOLD_TICKS = b.comment("A shorter press cycles state; hold opens selection.").defineInRange("menuHoldTicks", 4, 1, 20);
        LINE_WIDTH = b.defineInRange("lineWidth", 2.0, 1.0, 5.0);
        SPEC = b.build();
    }
    private PowerClientConfig() { }
}
