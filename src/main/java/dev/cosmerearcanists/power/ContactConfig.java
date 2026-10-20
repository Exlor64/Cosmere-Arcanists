package dev.cosmerearcanists.power;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class ContactConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED,CHROMIUM,NICROSIL,PLAYERS,MOBS,PVP,PURGE,FOREIGN,FEEDBACK;
    public static final ModConfigSpec.DoubleValue RANGE,DRAIN,COST,LOW_POWER,BURN_POWER,FLARE_POWER,POWER_GROWTH,EFFICIENCY_GROWTH;
    public static final ModConfigSpec.IntValue COOLDOWN,TICKS;
    static{var b=new ModConfigSpec.Builder();ENABLED=b.define("enabled",true);CHROMIUM=b.define("chromium",true);NICROSIL=b.define("nicrosil",true);
        PLAYERS=b.define("affectPlayers",true);MOBS=b.comment("Currently for integrations: ordinary mobs do not have player metal reserves.").define("affectMobs",true);PVP=b.define("respectPvpRules",true);
        RANGE=b.defineInRange("touchRange",3.0,.5,5.0);COOLDOWN=b.defineInRange("successfulContactCooldownTicks",20,1,200);
        DRAIN=b.defineInRange("burnReservePerSecond",1.0,0.0,100.0);COST=b.defineInRange("successfulContactReserveCost",10.0,0.0,1000.0);
        PURGE=b.define("chromiumEmptiesMetalReserves",true);FOREIGN=b.define("foreignArtHooks",true);FEEDBACK=b.define("contactMessages",true);
        b.push("nicrosilBurst");LOW_POWER=b.defineInRange("lowBurnMultiplier",1.75,1.0,100.0);BURN_POWER=b.defineInRange("burnMultiplier",2.5,1.0,100.0);FLARE_POWER=b.defineInRange("flareMultiplier",5.0,1.0,100.0);
        TICKS=b.defineInRange("fullReserveTicks",100,1,1200);POWER_GROWTH=b.defineInRange("bonusPowerPerAttributeDoubling",.10,0.0,1.0);EFFICIENCY_GROWTH=b.defineInRange("efficiencyPerAttributeDoubling",.5,0.0,5.0);b.pop();SPEC=b.build();}
    private ContactConfig(){}
}
