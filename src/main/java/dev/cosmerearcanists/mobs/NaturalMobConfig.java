package dev.cosmerearcanists.mobs;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class NaturalMobConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,BOTH,SETS;
    public static final ModConfigSpec.IntValue ONE_IN,SET_ONE_IN;
    public static final ModConfigSpec.DoubleValue STRENGTH;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS;
    static {
        var b=new ModConfigSpec.Builder();ENABLED=b.define("enabled",true);
        ONE_IN=b.comment("One chance per newly initialized eligible non-undead humanoid. Reloading/conversion never rerolls.").defineInRange("oneInChance",160,1,1000000);
        BOTH=b.define("allowBothArts",true);SETS=b.define("allowFullSets",true);
        SET_ONE_IN=b.comment("Per selected art, chance of its full enabled16-metal set instead of a single power, after the initial roll succeeds.").defineInRange("fullSetOneInChance",16,1,1000000);
        STRENGTH=b.defineInRange("powerStrength",1.0,.001,1024.0);
        DIMENSIONS=b.comment("Empty allows all dimensions. Use cosmerearcanists:scadrial to restrict new assignments.").defineListAllowEmpty("dimensions",List.of(),()->"cosmerearcanists:scadrial",v->v instanceof String s&&net.minecraft.resources.ResourceLocation.tryParse(s)!=null);
        SPEC=b.build();
    }
    private NaturalMobConfig(){}
}
