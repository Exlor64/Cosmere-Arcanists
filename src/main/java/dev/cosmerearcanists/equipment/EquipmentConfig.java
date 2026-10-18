package dev.cosmerearcanists.equipment;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EquipmentConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SPIKES,BALL,GRAPPLE;
    public static final ModConfigSpec.DoubleValue RANGE,REEL_SPEED,HOOK_SPEED,SHORTEN_RATE,SPIKE_DAMAGE;
    public static final ModConfigSpec.IntValue HOOK_CHARGE,SPIKE_LIFETIME;
    static {
        var b=new ModConfigSpec.Builder();
        SPIKES=b.define("spikeCrossbowEnabled",true); BALL=b.define("ballAndRopeEnabled",true); GRAPPLE=b.define("grapplingHookEnabled",true);
        RANGE=b.defineInRange("maxTetherRange",48.0,8.0,128.0);
        REEL_SPEED=b.defineInRange("returnSpeed",2.5,.1,8.0);
        HOOK_SPEED=b.defineInRange("hookLaunchSpeed",2.5,.1,8.0);
        SHORTEN_RATE=b.comment("Rope length removed per tick while Space is held on a latched grapple.").defineInRange("spaceReelPerTick",.1,.01,.5);
        HOOK_CHARGE=b.comment("Full charge in ticks; default is half a normal bow's 20 ticks.").defineInRange("hookChargeTicks",10,1,40);
        SPIKE_DAMAGE=b.comment("Crossbow spike damage per block/tick of impact speed, before armor; capped at 100.").defineInRange("spikeDamagePerSpeed",2.0,.1,20.0);
        SPIKE_LIFETIME=b.defineInRange("spikeLifetimeTicks",6000,100,24000);
        SPEC=b.build();
    }
    private EquipmentConfig(){ }
}
