package dev.cosmerearcanists.cognitive;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Early-loaded configuration foundation; Cognitive generation/portals are not implemented yet. */
public final class CognitiveConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue SPREAD_RADIUS, MINIMUM_SPACING, CENTER_X, CENTER_Z;
    public static final ModConfigSpec.IntValue SUBASTRAL_RADIUS, TRANSITION_WIDTH;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WORLD_RADII, WORLD_BIOME_TAGS;
    static {
        var b = new ModConfigSpec.Builder();
        b.comment("Configuration for upcoming Cognitive Realm generation. These settings do not create terrain or portals yet. Restart after changes.");
        b.push("perpendicularities");
        SPREAD_RADIUS = b.comment("Radius in blocks around centerX/centerZ, not diameter or minimum separation.")
            .defineInRange("spreadRadius", 10000, 16, 1000000);
        MINIMUM_SPACING = b.comment("Minimum horizontal distance between perpendicularities. Provisional default; placement must report impossible layouts rather than silently ignore this distance.")
            .defineInRange("minimumSpacing", 2000, 0, 2000000);
        CENTER_X = b.defineInRange("centerX", 0, -29000000, 29000000);
        CENTER_Z = b.defineInRange("centerZ", 0, -29000000, 29000000);
        b.pop(); b.push("subastrals");
        SUBASTRAL_RADIUS = b.comment("Default radius of the world-specific biome region surrounding a perpendicularity. Provisional default.")
            .defineInRange("defaultRadius", 1000, 16, 1000000);
        TRANSITION_WIDTH = b.comment("Desired transition width between subastral and surrounding Cognitive terrain; zero requests a sharp boundary. Generation must clamp it to the region's actual radius.")
            .defineInRange("transitionWidth", 128, 0, 1000000);
        WORLD_RADII = b.comment("Optional dimension_id=radius overrides. Unlisted worlds use defaultRadius. Duplicate world IDs use the last entry.")
            .defineListAllowEmpty("worldRadii", List.of(), () -> "cosmerearcanists:scadrial=1000", CognitiveConfig::validRadius);
        WORLD_BIOME_TAGS = b.comment("Optional destination_dimension_id=biome_tag_id associations for future world-specific subastral biome pools. Biome tags must be supplied by the future generator/datapack; no tag memberships or named subastral/world assignments are invented here. Duplicate IDs use the last entry.")
            .defineListAllowEmpty("worldBiomeTags", List.of(), () -> "cosmerearcanists:scadrial=cosmerearcanists:subastrals/scadrial", CognitiveConfig::validTag);
        b.pop(); SPEC = b.build();
    }
    private static String[] pair(Object value) {
        if (!(value instanceof String line)) return null;
        var parts = line.split("=", -1);
        return parts.length == 2 && ResourceLocation.tryParse(parts[0].trim()) != null ? parts : null;
    }
    private static boolean validRadius(Object value) {
        var parts = pair(value); if (parts == null) return false;
        try { int radius = Integer.parseInt(parts[1].trim()); return radius >= 16 && radius <= 1000000; }
        catch (NumberFormatException ignored) { return false; }
    }
    private static boolean validTag(Object value) {
        var parts = pair(value); return parts != null && ResourceLocation.tryParse(parts[1].trim()) != null;
    }
    /** Lookup API for future generation; it does not load chunks or resolve unregistered biome tags. */
    public static int radiusFor(ResourceLocation world) {
        int radius = SUBASTRAL_RADIUS.get();
        for (String line : WORLD_RADII.get()) {
            var parts = pair(line);
            if (validRadius(line) && ResourceLocation.parse(parts[0].trim()).equals(world)) radius = Integer.parseInt(parts[1].trim());
        }
        return radius;
    }
    /** Null means the destination's future built-in subastral profile should choose its biome pool. */
    public static ResourceLocation biomeTagFor(ResourceLocation world) {
        ResourceLocation tag = null;
        for (String line : WORLD_BIOME_TAGS.get()) {
            var parts = pair(line);
            if (validTag(line) && ResourceLocation.parse(parts[0].trim()).equals(world)) tag = ResourceLocation.parse(parts[1].trim());
        }
        return tag;
    }
    private CognitiveConfig() {}
}
