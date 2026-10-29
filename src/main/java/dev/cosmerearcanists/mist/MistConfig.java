package dev.cosmerearcanists.mist;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MistConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DIMENSIONS;
    public static final ModConfigSpec.BooleanValue ENABLED, BENEFITS;
    public static final ModConfigSpec.DoubleValue CHANCE, DISTANCE, CLEAR_STRENGTH, FREE_CONNECTION, INFINITE_CONNECTION, FREE_TAP, INFINITE_TAP;
    static {
        var b = new ModConfigSpec.Builder();
        ENABLED = b.define("enabled", true);
        DIMENSIONS = b.defineListAllowEmpty("dimensions", java.util.List.of(dev.cosmerearcanists.ScadrialWorld.ID), () -> dev.cosmerearcanists.ScadrialWorld.ID, value -> value instanceof String id && net.minecraft.resources.ResourceLocation.tryParse(id) != null);
        CHANCE = b.comment("Independent chance each Scadrial night; allows clear and misty streaks.")
            .defineInRange("nightlyMistChance", .5, 0, 1);
        DISTANCE = b.comment("Visibility in blocks at maximum mist density, before enhanced sight.")
            .defineInRange("denseMistVisibilityBlocks", 24.0, 8.0, 128.0);
        CLEAR_STRENGTH = b.comment("Combined effective Allomantic/Feruchemical tin sight strength that fully penetrates mist. Lesser strengths provide partial clarity.")
            .defineInRange("sightStrengthForClearMist", 6.0, .1, 100.0);
        BENEFITS = b.define("connectionMistSupply", true);
        FREE_CONNECTION = b.comment("Effective Scadrial Connection needed for free metal use. Ordinary native/stolen home Connection is1; the scadrial_connection attribute can supply stronger Connection.")
            .defineInRange("freeMetalsConnectionStrength", 1.15, .01, 1000.0);
        INFINITE_CONNECTION = b.defineInRange("infiniteReservesConnectionStrength", 4.0, .01, 1000.0);
        FREE_TAP = b.defineInRange("freeMetalsFundedDuraluminTap", 1.0, .01, 1000.0);
        INFINITE_TAP = b.defineInRange("infiniteReservesFundedDuraluminTap", 20.0, .01, 1000.0);
        SPEC = b.build();
    }
    private MistConfig() {}
}
