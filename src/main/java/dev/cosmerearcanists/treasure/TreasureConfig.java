package dev.cosmerearcanists.treasure;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class TreasureConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, ATIUM, METALMINDS, SPIKES;
    public static final ModConfigSpec.IntValue ONE_IN;
    public static final ModConfigSpec.DoubleValue MIN_FILL, MAX_FILL, MIN_STRENGTH, MAX_STRENGTH;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> TABLES, DIMENSIONS;
    static {
        var b = new ModConfigSpec.Builder();
        ENABLED = b.define("enabled", true);
        ONE_IN = b.comment("One chance per first loot generation of an eligible chest. A success adds at most one item, chosen equally among available categories.")
            .defineInRange("oneInEligibleChests", 2000, 1, 10000000);
        TABLES = b.defineListAllowEmpty("chestLootTables", List.of(
            "minecraft:chests/stronghold_library", "minecraft:chests/stronghold_corridor",
            "minecraft:chests/stronghold_crossing", "minecraft:chests/ancient_city",
            "minecraft:chests/end_city_treasure", "minecraft:chests/woodland_mansion",
            "minecraft:chests/bastion_treasure"), () -> "minecraft:chests/stronghold_library",
            value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
        DIMENSIONS = b.comment("Empty permits the listed chest tables in every dimension; use cosmerearcanists:scadrial for Scadrial only.")
            .defineListAllowEmpty("dimensions", List.of(), () -> "cosmerearcanists:scadrial",
                value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
        ATIUM = b.define("atiumElectrumAlloy", true);
        METALMINDS = b.define("chargedMetalminds", true);
        SPIKES = b.define("chargedPowerSpikes", true);
        MIN_FILL = b.comment("Ordinary metalmind charge as a fraction of its capacity. Endpoints are sorted if configured in reverse.")
            .defineInRange("minimumMetalmindFill", .05, .001, 1.0);
        MAX_FILL = b.defineInRange("maximumMetalmindFill", 1.0, .001, 1.0);
        MIN_STRENGTH = b.comment("Spike power strength and permanent nicrosilmind power strength. Nicrosil charge is the actual capacity cost of its power, bounded by item capacity.")
            .defineInRange("minimumPowerStrength", .25, .001, 16.0);
        MAX_STRENGTH = b.defineInRange("maximumPowerStrength", 1.0, .001, 16.0);
        SPEC = b.build();
    }
    private TreasureConfig() {}
}
