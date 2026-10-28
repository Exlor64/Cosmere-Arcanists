package dev.cosmerearcanists.koloss;
import net.neoforged.neoforge.common.ModConfigSpec;
/** Balance can change without replacing registered entities or deleting saved packs. */
public final class KolossConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SPAWNING,EMOTIONAL_CONTROL;
    public static final ModConfigSpec.DoubleValue HEALTH,DAMAGE,SPEED,ARMOR,SWORD_DROP,CONTROL_STRENGTH;
    static {
        var b=new ModConfigSpec.Builder();
        SPAWNING=b.comment("Allow koloss spawning. Dimension is always restricted to Scadrial; existing mobs are preserved.").define("spawning",true);
        EMOTIONAL_CONTROL=b.define("strongEmotionalControl",true);
        CONTROL_STRENGTH=b.comment("Minimum effective compliance-mode strength for either zinc or brass to control a koloss. Existing global domination threshold also applies.").defineInRange("minimumEmotionalControlStrength",4.0,.1,1024.0);
        HEALTH=b.comment("Health at scale1; grows with scale squared.").defineInRange("baseHealth",50.0,1.0,1000.0);
        DAMAGE=b.comment("Unarmed attack at scale1; grows linearly. Leader sword adds its normal weapon damage.").defineInRange("baseAttackDamage",8.0,1.0,100.0);
        SPEED=b.defineInRange("movementSpeed",.25,.05,1.0);
        ARMOR=b.defineInRange("armor",4.0,0.0,30.0);
        SWORD_DROP=b.comment("Leader sword drop probability;1 guarantees one under normal doMobLoot rules.").defineInRange("leaderSwordDropChance",1.0,0.0,1.0);
        SPEC=b.build();
    }
    private KolossConfig(){}
}
