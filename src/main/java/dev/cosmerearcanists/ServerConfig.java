package dev.cosmerearcanists;

import java.util.EnumMap;
import net.neoforged.neoforge.common.ModConfigSpec;

/** World-specific, server-authoritative settings. Changes do not reroll existing players. */
public final class ServerConfig {
    public enum AssignmentMode { RANDOM, CHOICE, ANCESTRY }
    public record ArtSettings(ModConfigSpec.BooleanValue enabled,
            ModConfigSpec.DoubleValue startingChance,
            EnumMap<Metal, ModConfigSpec.BooleanValue> metals) { }
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ORIGINS_ENABLED;
    public static final ModConfigSpec.EnumValue<AssignmentMode> ASSIGNMENT_MODE;
    public static final ModConfigSpec.BooleanValue ALLOW_TWINBORN;
    public static final ModConfigSpec.BooleanValue ALLOW_REROLLS;
    public static final ModConfigSpec.DoubleValue SCADRIAN_ANCESTRY_MULTIPLIER;
    public static final ArtSettings ALLOMANCY;
    public static final ArtSettings FERUCHEMY;
    public static final ModConfigSpec.BooleanValue HEMALURGY_ENABLED;
    static {
        var b = new ModConfigSpec.Builder();
        b.push("origins");
        ORIGINS_ENABLED = b.comment("Enable origin assignment and player origin commands.").define("enabled", true);
        ASSIGNMENT_MODE = b.comment("RANDOM: roll; CHOICE: select; ANCESTRY: roll using ancestry multiplier.").defineEnum("assignmentMode", AssignmentMode.RANDOM);
        ALLOW_TWINBORN = b.comment("Allow one natural power from each art on the same player.").define("allowTwinborn", true);
        ALLOW_REROLLS = b.comment("Allow players to replace an assigned origin. Operators can always reset.").define("allowRerolls", false);
        SCADRIAN_ANCESTRY_MULTIPLIER = b.comment("Scadrian human probability multiplier in ANCESTRY mode. Other origins have no natural Metallic Arts.").defineInRange("scadrianAncestryMultiplier", 1.0, 0.0, 100.0);
        b.pop();
        ALLOMANCY = art(b, "allomancy", 0.25);
        FERUCHEMY = art(b, "feruchemy", 0.25);
        b.push("hemalurgy");
        HEMALURGY_ENABLED = b.comment("Feature gate for future spike mechanics. Hemalurgy is acquired, never rolled as a birth power.").define("enabled", true);
        b.pop();
        SPEC = b.build();
    }
    private static ArtSettings art(ModConfigSpec.Builder b, String name, double chance) {
        b.push(name);
        var enabled = b.define("enabled", true);
        var probability = b.comment("Independent chance of one natural power for a Scadrian origin; provisional balance.").defineInRange("startingChance", chance, 0.0, 1.0);
        b.push("metals");
        var metals = new EnumMap<Metal, ModConfigSpec.BooleanValue>(Metal.class);
        for (var metal : Metal.values()) metals.put(metal, b.define(metal.id(), true));
        b.pop(); b.pop();
        return new ArtSettings(enabled, probability, metals);
    }
    private ServerConfig() { }
}
