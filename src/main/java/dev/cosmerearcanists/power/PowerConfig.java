package dev.cosmerearcanists.power;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PowerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, IRON_STEEL, DETECTION_BLOCKS, DETECTION_ITEMS, DETECTION_GEAR, RECIPE_DETECTION, CONSUMPTION, FLARING, RECOIL, PLAYER_TARGETS;
    public static final ModConfigSpec.IntValue RANGE, MAX_TARGETS, SCAN_BUDGET, SYNC_INTERVAL, FERUCHEMY_LEVELS;
    public static final ModConfigSpec.DoubleValue FORCE, FLARE_STRENGTH, FLARE_DRAIN, BURN_DRAIN, RESERVE_CAP, VIAL_RESERVE, MAX_SPEED, PLAYER_MASS, AIM_ANGLE;
    static {
        var b = new ModConfigSpec.Builder();
        b.push("controls");
        ENABLED = b.define("enabled", true);
        FERUCHEMY_LEVELS = b.comment("Selector levels only; Feruchemical effects are not implemented yet.").defineInRange("feruchemyLevels", 5, 1, 20);
        b.pop(); b.push("ironSteel");
        IRON_STEEL = b.define("enabled", true);
        RANGE = b.defineInRange("range", 16, 4, 32);
        FORCE = b.comment("Force per game tick, divided by target mass.").defineInRange("force", 7.0, 0.1, 100.0);
        PLAYER_MASS = b.defineInRange("playerMass", 80.0, 1.0, 1000.0);
        MAX_SPEED = b.defineInRange("maxSpeed", 2.0, 0.1, 10.0);
        AIM_ANGLE = b.defineInRange("targetConeDegrees", 12.0, 1.0, 45.0);
        RECOIL = b.define("reactionForce", true);
        PLAYER_TARGETS = b.define("affectOtherPlayers", true);
        FLARING = b.define("allowFlaring", true);
        FLARE_STRENGTH = b.defineInRange("flareStrengthMultiplier", 2.0, 1.0, 10.0);
        FLARE_DRAIN = b.defineInRange("flareDrainMultiplier", 3.0, 1.0, 20.0);
        CONSUMPTION = b.define("consumeReserves", true);
        BURN_DRAIN = b.comment("Reserve units consumed per second while burning, whether applying force or sensing.").defineInRange("burnDrainPerSecond", 1.0, 0.01, 20.0);
        RESERVE_CAP = b.defineInRange("reserveCapacity", 300.0, 1.0, 100000.0);
        VIAL_RESERVE = b.defineInRange("vialReserve", 60.0, 1.0, 10000.0);
        b.pop(); b.push("metalDetection");
        DETECTION_BLOCKS = b.define("blocks", true); DETECTION_ITEMS = b.define("droppedItems", true); DETECTION_GEAR = b.define("equippedItems", true);
        RECIPE_DETECTION = b.comment("Trace every crafting ingredient recursively. Any possible aluminum ingredient excludes the result.").define("recursiveRecipes", true);
        MAX_TARGETS = b.defineInRange("maxTargets", 96, 8, 256);
        SCAN_BUDGET = b.comment("Loaded block positions checked per player per tick; no chunk loading.").defineInRange("blockChecksPerTick", 2048, 128, 8192);
        SYNC_INTERVAL = b.defineInRange("syncIntervalTicks", 5, 2, 40);
        b.pop(); SPEC = b.build();
    }
    private PowerConfig() { }
}
