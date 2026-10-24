package dev.cosmerearcanists.currency;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class CurrencyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue TRADES,ALL_VILLAGERS,EMBED,RECALL,PICKUP,EXCHANGE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS;
    public static final ModConfigSpec.IntValue CLIPS_PER_BOXING,CLIPS_PER_EMERALD,LIFETIME,PICKUP_DELAY;
    public static final ModConfigSpec.DoubleValue BOXING_UNITS,CLIP_UNITS,RETURN_SPEED,RETURN_RANGE;
    static{var b=new ModConfigSpec.Builder();TRADES=b.define("scadrialVillagerTrades",true);ALL_VILLAGERS=b.comment("Opt in to coin trades for villagers in every dimension. Otherwise use listed dimensions or entity tag cosmerearcanists.scadrial.").define("allVillagers",false);
        DIMENSIONS=b.defineListAllowEmpty("scadrialDimensions",List.of("cosmerearcanists:scadrial"),()->"cosmerearcanists:scadrial",v->v instanceof String);
        CLIPS_PER_BOXING=b.comment("Currency value ratio. Default10clips per boxing, as specified for this mod. Limited to128 so a villager can accept the clips in two64-item inputs.").defineInRange("clipsPerBoxing",10,2,128);CLIPS_PER_EMERALD=b.comment("Gameplay conversion of original emerald prices into clip value. Costs round up and payouts round down when a native trade slot cannot represent the exact value.").defineInRange("clipsPerEmerald",10,1,100);
        EXCHANGE=b.comment("Add a fixed-price clips-to-boxing money-changing offer. This is trading, not a crafting recipe.").define("villagerBoxingExchange",true);
        EMBED=b.define("firedCoinsStickToBlocks",true);RECALL=b.define("ironPullReleasesCoinsAsItems",true);PICKUP=b.define("embeddedCoinPickup",true);LIFETIME=b.comment("Coin shots recover as items after this many loaded entity ticks, including time embedded.").defineInRange("coinProjectileLifetimeTicks",12000,100,120000);PICKUP_DELAY=b.defineInRange("embeddedPickupDelayTicks",5,0,100);
        BOXING_UNITS=b.defineInRange("metalNuggetUnitsPerBoxing",1.0,.001,1000.0);CLIP_UNITS=b.defineInRange("metalNuggetUnitsPerClip",1.0,.001,1000.0);RETURN_SPEED=b.comment("Maximum iron recall item speed; actual initial speed also depends on pull strength and metal mass.").defineInRange("maximumCoinReturnSpeed",2.0,.1,8.0);RETURN_RANGE=b.defineInRange("maximumCoinReturnRange",64.0,4.0,128.0);SPEC=b.build();}
    private CurrencyConfig(){}
}
