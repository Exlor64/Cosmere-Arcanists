package dev.cosmerearcanists.power;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PowerConfig {
    public static final ModConfigSpec SPEC;
    public static final java.util.Map<String,ModConfigSpec.DoubleValue> BURN_RATES=new java.util.LinkedHashMap<>();
    public static final ModConfigSpec.BooleanValue METAL_BURN_RATES;
    public static final ModConfigSpec.BooleanValue ENABLED, IRON_STEEL, DETECTION_BLOCKS, DETECTION_ITEMS, DETECTION_GEAR, RECIPE_DETECTION, CONSUMPTION, FLARING, RECOIL, PLAYER_TARGETS, FALL_MITIGATION, BLOCK_RESISTANCE, STEEL_MOVEMENT, IRON_MOVEMENT, RESIST_WALKING, STEEL_PROJECTILES, ALTERNATE_DROP_DOWN, DETECTION_PROJECTILES, SECONDARY_ENABLED;
    public static final ModConfigSpec.IntValue RANGE_CAP, RANGE, MAX_TARGETS, SCAN_BUDGET, SYNC_INTERVAL, FERUCHEMY_LEVELS, NEAR_SCAN_RADIUS, URGENT_SCAN_BUDGET, PROJECTILE_LIFETIME;
    public static final ModConfigSpec.DoubleValue PHYSICAL_BURN_RATE, LOW_RANGE, BURN_RANGE, FLARE_RANGE, RANGE_EXPONENT, FORCE, FLARE_STRENGTH, FLARE_DRAIN, BURN_DRAIN, LEGACY_RESERVE_CAP, RESERVE_CAP, MAX_SPEED, PLAYER_MASS, AIM_ANGLE, FALL_MITIGATION_STRENGTH, RESISTANCE_COST, RESISTANCE_HARDNESS_CAP, MOVEMENT_ANGLE, IRON_STRENGTH, STEEL_STRENGTH, LOW_STRENGTH, BURN_STRENGTH, LOW_DRAIN, NUGGET_SPEED, PROJECTILE_MAX_SPEED, NUGGET_DAMAGE, PROJECTILE_DAMAGE_CAP, NUGGET_MASS, UNKNOWN_METAL_UNITS, PROJECTILE_RESERVE_COST;
    static {
        var b = new ModConfigSpec.Builder();
        b.push("controls");
        ENABLED = b.define("enabled", true);
        SECONDARY_ENABLED=b.define("secondaryActivation",true);
        FERUCHEMY_LEVELS = b.comment("Selector levels only; Feruchemical effects are not implemented yet.").defineInRange("feruchemyLevels", 5, 1, 20);
        b.pop(); b.push("ironSteel");
        IRON_STEEL = b.define("enabled", true);
        RESIST_WALKING = b.comment("Remove vanilla walking acceleration opposing an active Allomantic impulse; retain sideways input and existing inertia.").define("preventWalkingAgainstForce", true);
        IRON_STRENGTH = b.comment("Iron-only force multiplier, applied in addition to the power attribute and flaring. 2 gives base iron the previous strength of attribute 2.").defineInRange("ironStrengthMultiplier", 2.0, 0.1, 20.0);
        STEEL_STRENGTH = b.comment("Steel-only force multiplier, applied in addition to the power attribute and burn level. 2 gives base steel the previous strength of attribute 2.").defineInRange("steelStrengthMultiplier", 2.0, 0.1, 20.0);
        IRON_MOVEMENT = b.define("ironAlternateMovement", true);
        STEEL_MOVEMENT = b.define("alternateMovement", true);
        MOVEMENT_ANGLE = b.comment("Anchors outside this angle from intended movement contribute no force; oblique anchors contribute less.").defineInRange("movementAnchorAngleDegrees", 60.0, 5.0, 80.0);
        RANGE = b.defineInRange("range", 16, 4, 32);
        RANGE_CAP=b.comment("Maximum scaled detection radius; larger scans retain per-tick work budgets.").defineInRange("maxRange",32,4,64);
        LOW_RANGE=b.defineInRange("lowBurnRangeMultiplier",.6,0.0,2.0);
        BURN_RANGE=b.defineInRange("burningRangeMultiplier",.8,0.0,2.0);
        FLARE_RANGE=b.defineInRange("flareRangeMultiplier",1.0,0.0,2.0);
        RANGE_EXPONENT=b.comment("Attribute exponent: default gives 30 percent more range for each doubling. Zero disables attribute scaling.").defineInRange("rangeAttributeExponent",Math.log(1.3)/Math.log(2),0.0,2.0);
        FORCE = b.comment("Force per game tick, divided by target mass.").defineInRange("force", 7.0, 0.1, 100.0);
        PLAYER_MASS = b.defineInRange("playerMass", 80.0, 1.0, 1000.0);
        MAX_SPEED = b.defineInRange("maxSpeed", 2.0, 0.1, 10.0);
        AIM_ANGLE = b.defineInRange("targetConeDegrees", 12.0, 1.0, 45.0);
        RECOIL = b.define("reactionForce", true);
        FALL_MITIGATION = b.comment("Upward iron/steel force reduces accumulated fall damage only when it actually slows a descent.").define("reduceFallDamageWhenBraking", true);
        FALL_MITIGATION_STRENGTH = b.comment("1: full speed-squared reduction; 0: vanilla fall distance.").defineInRange("fallDamageReductionStrength", 1.0, 0.0, 1.0);
        PLAYER_TARGETS = b.define("affectOtherPlayers", true);
        LOW_STRENGTH = b.defineInRange("lowBurnStrengthMultiplier", 0.5, 0.01, 10.0);
        BURN_STRENGTH = b.defineInRange("burningStrengthMultiplier", 1.0, 0.01, 10.0);
        LOW_DRAIN = b.defineInRange("lowBurnDrainMultiplier", 0.5, 0.01, 20.0);
        FLARING = b.define("allowFlaring", true);
        FLARE_STRENGTH = b.defineInRange("flareStrengthMultiplier", 2.0, 1.0, 10.0);
        FLARE_DRAIN = b.defineInRange("flareDrainMultiplier", 3.0, 1.0, 20.0);
        CONSUMPTION = b.define("consumeReserves", true);
        BURN_DRAIN = b.comment("Reserve units consumed per second while burning, whether applying force or sensing.").defineInRange("burnDrainPerSecond", 1.0, 0.01, 20.0);
        LEGACY_RESERVE_CAP = b.comment("Legacy reserve units: retained for saved-data conversion and existing drain/cost settings. Do not change after migration.").defineInRange("reserveCapacity",300.0,1.0,100000.0);
        RESERVE_CAP = b.comment("Current maximum reserve. Burn rates and ability costs scale from legacy reserve units to preserve full-reserve duration.").defineInRange("maxMetalReserve",100.0,1.0,100000.0);
        b.pop(); b.push("metalDetection");
        DETECTION_BLOCKS = b.define("blocks", true); DETECTION_ITEMS = b.define("droppedItems", true); DETECTION_GEAR = b.define("equippedItems", true);
        DETECTION_PROJECTILES=b.comment("Detect and push/pull in-flight steel-launched metal, including shots fired by another player.").define("firedProjectiles",true);
        RECIPE_DETECTION = b.comment("Trace every crafting ingredient recursively. Any possible aluminum ingredient excludes the result.").define("recursiveRecipes", true);
        BLOCK_RESISTANCE = b.define("blockResistance", true);
        RESISTANCE_COST = b.comment("Each intervening solid block adds hardness times this value to detection distance.").defineInRange("resistanceDistancePerHardness", 0.25, 0.0, 10.0);
        RESISTANCE_HARDNESS_CAP = b.comment("Maximum hardness per blocker; also used for unbreakable blocks.").defineInRange("resistanceHardnessCap", 50.0, 1.0, 10000.0);
        MAX_TARGETS = b.defineInRange("maxTargets", 96, 8, 256);
        SCAN_BUDGET = b.comment("Maximum spherical scan positions per player per tick, nearest first; discoveries publish immediately. No chunk loading.").defineInRange("blockChecksPerTick", 2048, 128, 8192);
        NEAR_SCAN_RADIUS = b.comment("Radius refreshed around the current player every active tick, independent of the background scan.").defineInRange("immediateScanRadius", 6, 2, 10);
        URGENT_SCAN_BUDGET = b.comment("Extra near-first positions checked when falling/moving quickly into a new block or starting activation. 32768 covers the full default 16-block sphere; 0 disables this pass. Loaded chunks only.").defineInRange("urgentBlockChecksPerTick", 32768, 0, 262144);
        SYNC_INTERVAL = b.comment("Periodic state/reserve updates. Target-list changes are always sent immediately, regardless of this interval.").defineInRange("syncIntervalTicks", 1, 1, 40);
        b.pop(); b.push("steelProjectiles");
        STEEL_PROJECTILES=b.define("enabled",true);
        ALTERNATE_DROP_DOWN=b.comment("Alt + held steel activation drops metal at the feet, moving straight down instead of firing. Disabling keeps an ordinary vanilla drop in alternate mode.").define("alternateDropDownwards",true);
        NUGGET_SPEED=b.comment("Blocks per tick for one nugget at reference push force 7, before the steel strength multiplier.").defineInRange("nuggetSpeed",3.5,.1,8.0);
        PROJECTILE_MAX_SPEED=b.defineInRange("maxSpeed",8.0,.1,16.0);
        NUGGET_DAMAGE=b.comment("Damage before armor at the reference nugget speed.").defineInRange("nuggetDamage",2.0,.01,99.0);
        PROJECTILE_DAMAGE_CAP=b.comment("Smooth damage ceiling; never exceeds 100 even if configured higher externally.").defineInRange("damageCap",100.0,1.0,100.0);
        NUGGET_MASS=b.comment("Physical mass used for recoil per nugget-equivalent of metal.").defineInRange("nuggetMass",.2,.001,10.0);
        UNKNOWN_METAL_UNITS=b.comment("Per-item nugget equivalents for detectable metal items without a resolvable recipe.").defineInRange("unknownItemMetalUnits",9.0,.001,1000000.0);
        PROJECTILE_RESERVE_COST=b.comment("Additional steel reserve units per thrown stack; regular burning costs still apply.").defineInRange("reserveCostPerShot",1.0,0.0,1000.0);
        PROJECTILE_LIFETIME=b.comment("Ticks before an airborne shot returns to a normal dropped item.").defineInRange("lifetimeTicks",100,10,1200);
        b.pop();b.push("burnRates");
        METAL_BURN_RATES=b.comment("Lore-informed relative passive burn rates. Exact numerical timings are gameplay tuning, not canonical constants. Existing per-power drain settings still multiply these values; flat ability costs and captured bursts are unchanged.").define("enabled",true);
        PHYSICAL_BURN_RATE=b.comment("Additional passive drain factor for iron, steel and pewter. Default0.5 doubles their full-reserve durations, including in existing world configs.").defineInRange("physicalBurnRateMultiplier",.5,0.0,100.0);
        for(var metal:dev.cosmerearcanists.Metal.values())BURN_RATES.put(metal.id(),b.defineInRange(metal.id(),defaultBurnMultiplier(metal.id()),0.0,100.0));
        BURN_RATES.put("atium",b.comment("Atium-electrum alloy, not pure atium.").defineInRange("atium",3.0,0.0,100.0));
        b.pop(); SPEC = b.build();
    }
    public static double defaultBurnMultiplier(String metal){return switch(metal){
        case "copper"->.5;case "tin"->.25;case "zinc","brass"->.5;case "pewter"->2;
        case "gold","atium","bendalloy"->3;case "electrum"->1.5;case "cadmium"->.1875;default->1;
    };}
    public static double burnMultiplier(String metal){var setting=BURN_RATES.get(metal);return METAL_BURN_RATES.get()&&setting!=null?setting.get()*(metal.equals("iron")||metal.equals("steel")||metal.equals("pewter")?PHYSICAL_BURN_RATE.get():1):1;}
    public static double burnCost(String metal,double legacyUnits){return reserveCost(legacyUnits*burnMultiplier(metal));}
    public static double reserveCost(double legacyUnits){return legacyUnits*RESERVE_CAP.get()/LEGACY_RESERVE_CAP.get();}
    private PowerConfig() { }
}
