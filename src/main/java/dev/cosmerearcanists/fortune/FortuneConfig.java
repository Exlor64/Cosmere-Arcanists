package dev.cosmerearcanists.fortune;

import java.util.*;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class FortuneConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,FISHING,CHESTS,MOBS,MISHAPS,MESSAGES,DEFLECTION,TNT_DUDS;
    public static final ModConfigSpec.DoubleValue STORE_PER_ATTRIBUTE,STORE_STEP,TAP_LEVELS,TAP_STEP,CAP,LOSS,FISHING_STEP,FISHING_CAP,FISHING_PENALTY,BONUS_STEP,BONUS_CAP,LOOT_PENALTY,PERIODIC_CHANCE,ACTION_CHANCE,DETECTION_RANGE,DEFLECT_STEP,DEFLECT_CAP,DEFLECT_RANGE,DEFLECT_ANGLE,TNT_STEP,TNT_CAP,TNT_RANGE_CAP;
    public static final ModConfigSpec.IntValue GLOBAL_COOLDOWN,DEFLECT_HORIZON;
    public record MishapSettings(ModConfigSpec.BooleanValue enabled,ModConfigSpec.DoubleValue minimum,ModConfigSpec.IntValue cooldown,ModConfigSpec.IntValue weight){}
    public static final Map<Misfortune,MishapSettings> EVENTS=new EnumMap<>(Misfortune.class);
    static {
        var b=new ModConfigSpec.Builder();
        ENABLED=b.define("enabled",true);FISHING=b.define("fishingLuck",true);CHESTS=b.define("generatedChestLoot",true);MOBS=b.define("creditedMobLoot",true);MISHAPS=b.define("storageMisfortunes",true);MESSAGES=b.define("misfortuneActionBarMessages",true);
        STORE_PER_ATTRIBUTE=b.defineInRange("storageFractionPerAttribute",.3,.001,10.0);STORE_STEP=b.defineInRange("storeFractionPerLevel",.1,.001,1.0);TAP_LEVELS=b.defineInRange("tapLevelsPerAttribute",10.0,.1,1000.0);TAP_STEP=b.defineInRange("fortunePerTapLevel",.15,.001,10.0);CAP=b.defineInRange("maximumFortuneMultiplier",20.0,1.0,100.0);LOSS=b.defineInRange("tapCompressionExponent",.25,0.0,2.0);
        FISHING_STEP=b.defineInRange("fishingLuckPerFundedTapLevel",.5,0.0,10.0);FISHING_CAP=b.defineInRange("maximumExtraFishingLuck",5.0,0.0,64.0);FISHING_PENALTY=b.defineInRange("fishingBadLuckAtFullStorage",10.0,0.0,64.0);
        BONUS_STEP=b.comment("Bonus loot-table attempts per funded tap level; fractional amounts are a chance. Each successful attempt contributes at most one eligible stack from that table.").defineInRange("bonusLootAttemptsPerTapLevel",.05,0.0,1.0);BONUS_CAP=b.comment("Ordinary cap; amplification can exceed this, but a hard four-attempt CPU limit always applies.").defineInRange("maximumBonusLootAttempts",2.0,0.0,4.0);LOOT_PENALTY=b.comment("Chance to lose one eligible loot stack, multiplied by the actual stored fraction.").defineInRange("lootLossChanceAtFullStorage",.75,0.0,1.0);
        PERIODIC_CHANCE=b.comment("Checked once per real server second; multiplied by actual stored fraction, with a shared cooldown after success.").defineInRange("mishapChancePerSecondAtFullStorage",.08,0.0,1.0);ACTION_CHANCE=b.defineInRange("actionMishapChanceAtFullStorage",.15,0.0,1.0);GLOBAL_COOLDOWN=b.defineInRange("minimumTicksBetweenMishaps",200,20,72000);DETECTION_RANGE=b.defineInRange("throughWallDetectionRange",24.0,2.0,48.0);
        DEFLECTION=b.define("projectileNearMisses",true);TNT_DUDS=b.define("lastSecondTntDuds",true);
        DEFLECT_STEP=b.defineInRange("projectileDeflectionChancePerTapLevel",.02,0.0,1.0);DEFLECT_CAP=b.defineInRange("maximumProjectileDeflectionChance",.5,0.0,.95);DEFLECT_RANGE=b.defineInRange("projectileProtectionRange",8.0,2.0,16.0);DEFLECT_ANGLE=b.defineInRange("maximumProjectileTurnDegrees",12.0,1.0,20.0);DEFLECT_HORIZON=b.defineInRange("projectileProtectionLookaheadTicks",6,1,8);
        TNT_STEP=b.defineInRange("tntDudChancePerTapLevel",.01,0.0,1.0);TNT_CAP=b.defineInRange("maximumTntDudChance",.4,0.0,.95);TNT_RANGE_CAP=b.comment("Maximum supported explosion damage radius for exposure checks; ordinary TNT reaches8blocks.").defineInRange("maximumTntBlastProtectionRadius",32.0,8.0,64.0);
        b.push("mishaps");for(var effect:Misfortune.values()){b.push(effect.key());EVENTS.put(effect,new MishapSettings(b.define("enabled",true),b.defineInRange("minimumStoredFraction",effect.minimum,.001,.95),b.defineInRange("cooldownTicks",effect.cooldown,20,72000),b.defineInRange("selectionWeight",effect.weight,1,100)));b.pop();}b.pop();SPEC=b.build();
    }
    private FortuneConfig(){}
}
