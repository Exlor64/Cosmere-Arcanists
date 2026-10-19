package dev.cosmerearcanists;

import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Loaded before world generation; restart required. Existing chunks are unaffected. */
public final class MetallurgyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue GENERATION;
    public static final ModConfigSpec.BooleanValue ALLOYING, GOD_CONVERSIONS;
    public static final Map<String, ModConfigSpec.BooleanValue> GOD_METALS = new LinkedHashMap<>();
    public static final Map<String, ModConfigSpec.BooleanValue> ALLOYS = new LinkedHashMap<>();
    public static final Map<String, ModConfigSpec.BooleanValue> ORES = new LinkedHashMap<>();
    public static final Map<String, ModConfigSpec.DoubleValue> CHANCES = new LinkedHashMap<>();
    static {
        var b = new ModConfigSpec.Builder();
        b.push("oreGeneration");
        GENERATION = b.comment("Generate added ores in new Overworld chunks. Restart after changes.").define("enabled", true);
        for (String metal : ModMetals.BASE) {
            b.push(metal);
            ORES.put(metal, b.define("enabled", true));
            CHANCES.put(metal, b.comment("Probability each configured vein attempt is allowed; 0 disables generation.").defineInRange("veinSpawnChance", 1.0, 0.0, 1.0));
            b.pop();
        }
        b.pop();
        b.push("alloying");
        ALLOYING = b.comment("Enable alloy crafting recipes. Restart or reload recipes after changes.").define("enabled", true);
        for (String metal : new String[]{"steel", "pewter", "brass", "bronze", "duralumin", "electrum", "bendalloy", "nicrosil"})
            ALLOYS.put(metal, b.define(metal, true));
        b.pop();b.push("godMetals");
        GOD_CONVERSIONS=b.comment("Enable nugget/ingot/block conversion recipes. Restart or reload recipes after changes. Registered items, blocks and saved contents are preserved.").define("conversions",true);
        for(String metal:ModMetals.GOD_METALS)GOD_METALS.put(metal,b.define(metal,true));
        b.pop(); SPEC = b.build();
    }
    private MetallurgyConfig() { }
}
