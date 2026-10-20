package dev.cosmerearcanists.power;

import java.util.*;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.investiture.InvestitureCleanseEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/** Duralumin boosts effective effects, never the saved attributes. Bursts are per-metal fuel commitments. */
public final class EnhancementService {
    private static final class Burst {final double fuel;final int ticks;final long start;double spent;Burst(double fuel,int ticks,long start){this.fuel=fuel;this.ticks=ticks;this.start=start;}}
    private static final Map<UUID,Map<String,Burst>> BURSTS=new HashMap<>();
    public static boolean configured(String metal){return EnhancementConfig.ENABLED.get() && (metal.equals("aluminum")?EnhancementConfig.ALUMINUM.get():metal.equals("duralumin") && EnhancementConfig.DURALUMIN.get()) && PowerConfig.ENABLED.get() && ServerConfig.ALLOMANCY.enabled().get() && ServerConfig.ALLOMANCY.metals().get(Metal.valueOf(metal.toUpperCase(Locale.ROOT))).get();}
    private static int state(ServerPlayer p,String metal){int state=PowerData.state(p,"a:"+metal);return state==3&&!PowerConfig.FLARING.get()?2:state;}
    private static boolean usable(ServerPlayer p,String metal){return configured(metal) && p.isAlive() && !p.isSpectator() && PowerAttributes.strength(p,"a:"+metal)>0 && state(p,metal)>0 && (!PowerConfig.CONSUMPTION.get() || PowerData.reserve(p,metal)>0);}
    public static double strength(ServerPlayer p,String metal){return usable(p,metal)?PowerAttributes.strength(p,"a:"+metal)*PowerRules.levelMultiplier(state(p,metal),PowerConfig.LOW_STRENGTH.get(),PowerConfig.BURN_STRENGTH.get(),PowerConfig.FLARE_STRENGTH.get()):0;}
    public static double multiplier(ServerPlayer p,String power){return Math.max(selfMultiplier(p,power),ContactService.multiplier(p,power));}
    private static double selfMultiplier(ServerPlayer p,String power){
        if(!power.startsWith("a:") || power.equals("a:aluminum") || power.equals("a:duralumin") || !configured("duralumin") || PowerAttributes.strength(p,"a:duralumin")<=0 || !p.isAlive() || p.isSpectator())return 1;
        var bursts=BURSTS.get(p.getUUID());var burst=bursts==null?null:bursts.get(power.substring(2));
        if(burst!=null && p.level().getGameTime()<burst.start+burst.ticks)return boost(EnhancementConfig.FLARE_POWER.get(),PowerAttributes.strength(p,"a:duralumin"));
        if(!usable(p,"duralumin"))return 1;
        return switch(state(p,"duralumin")){case 1->boost(EnhancementConfig.LOW_POWER.get(),PowerAttributes.strength(p,"a:duralumin"));case 2->boost(EnhancementConfig.BURN_POWER.get(),PowerAttributes.strength(p,"a:duralumin"));default->1;};
    }
    public static boolean committed(ServerPlayer p,String metal){var map=BURSTS.get(p.getUUID());return map!=null && map.containsKey(metal);}
    /** Passive consumption only: do not apply relative metal rates to flat ability costs. */
    public static void burn(ServerPlayer p,String metal,double amount){consume(p,metal,amount*PowerConfig.burnMultiplier(metal));}
    public static void consume(ServerPlayer p,String metal,double amount){
        if(ContactService.committed(p,metal))return;
        var bursts=BURSTS.get(p.getUUID());if(bursts!=null && bursts.containsKey(metal))return;
        double multiplier=usable(p,"duralumin") && !metal.equals("duralumin")?switch(state(p,"duralumin")){case 1->EnhancementConfig.LOW_DRAIN.get();case 2->EnhancementConfig.BURN_DRAIN.get();default->1;}:1;
        if(multiplier>1)multiplier=Math.max(1,multiplier/efficiency(PowerAttributes.strength(p,"a:duralumin")));
        PowerData.reserve(p,metal,PowerData.reserve(p,metal)-PowerConfig.reserveCost(amount)*multiplier);
    }
    public static double attributeCurve(double strength,double growth){return EnhancementMath.attributeCurve(strength,growth);}
    private static double efficiency(double strength){return attributeCurve(strength,EnhancementConfig.EFFICIENCY_GROWTH.get());}
    private static double boost(double base,double strength){return 1+(base-1)*attributeCurve(strength,EnhancementConfig.POWER_GROWTH.get());}
    /** Resource-independent integration profile: foreign arts apply these modifiers to their own validated effects/budgets.
     * For a burst they must snapshot fuel/fullness, consume that captured budget over durationTicks, and prevent free retriggers. */
    public record Profile(double strengthMultiplier,double consumptionMultiplier,int fullReserveTicks,boolean burst){
        public int durationTicks(double current,double maximum){return burst?flareTicks(current,maximum,fullReserveTicks):0;}
    }
    public static Profile profile(ServerPlayer p){
        if(!usable(p,"duralumin"))return new Profile(1,1,0,false);
        double strength=PowerAttributes.strength(p,"a:duralumin"),efficiency=efficiency(strength);int state=state(p,"duralumin");
        double base=state==1?EnhancementConfig.LOW_POWER.get():state==2?EnhancementConfig.BURN_POWER.get():EnhancementConfig.FLARE_POWER.get();
        double drain=state==1?EnhancementConfig.LOW_DRAIN.get():state==2?EnhancementConfig.BURN_DRAIN.get():1;
        return new Profile(boost(base,strength),state==3?1:Math.max(1,drain/efficiency),(int)Math.ceil(EnhancementConfig.FLARE_TICKS.get()*efficiency),state==3);
    }
    public static int flareTicks(double reserves,double maximum,int fullTicks){return EnhancementMath.flareTicks(reserves,maximum,fullTicks);}
    public static boolean featureEnabled(String metal){return switch(metal){
        case "bendalloy","cadmium"->BubbleService.configured(metal);case "gold"->GoldService.configured();case "chromium","nicrosil"->ContactService.configured(metal);case "iron","steel"->PowerConfig.IRON_STEEL.get();case "tin"->TinConfig.ENABLED.get();case "pewter"->PewterService.configured();
        case "atium"->TemporalConfig.ENABLED.get() && TemporalConfig.ATIUM.get();case "electrum"->TemporalConfig.ENABLED.get() && TemporalConfig.ELECTRUM.get();
        case "zinc","brass"->EmotionService.configured("a:"+metal);case "bronze"->SeekingConfig.ENABLED.get() && SeekingConfig.BRONZE.get();case "copper"->SeekingConfig.ENABLED.get() && SeekingConfig.COPPER.get();default->true;};}
    public static void begin(ServerPlayer p){
        if(!p.isAlive() || p.isSpectator()){clear(p);return;}
        if(usable(p,"aluminum")){
            clear(p);ContactService.clearTarget(p);
            if(EnhancementConfig.PURGE.get()){var reserves=PowerData.data(p).getCompound("reserves");for(String metal:new HashSet<>(reserves.getAllKeys()))PowerData.reserve(p,metal,0);for(String power:PowerAttributes.keys())if(power.startsWith("a:"))PowerData.state(p,power,0);}
            else PowerData.state(p,"a:aluminum",0);
            if(EnhancementConfig.CLEANSE.get())NeoForge.EVENT_BUS.post(new InvestitureCleanseEvent(p));
            PowerNetwork.sync(p,List.of());return;
        }
        if(!configured("duralumin") || PowerAttributes.strength(p,"a:duralumin")<=0){clear(p);return;}
        var existing=BURSTS.get(p.getUUID());if(existing!=null){var available=PowerData.available(p);existing.keySet().removeIf(m->!available.contains("a:"+m) || !featureEnabled(m));if(existing.isEmpty())BURSTS.remove(p.getUUID());}
        if(!BURSTS.containsKey(p.getUUID()) && usable(p,"duralumin") && state(p,"duralumin")==3){
            var bursts=new HashMap<String,Burst>();int longest=0;long now=p.level().getGameTime();
            for(String power:PowerData.available(p))if(power.startsWith("a:") && !power.equals("a:duralumin") && !power.equals("a:aluminum") && !ContactService.committed(p,power.substring(2)) && PowerRules.implemented(power) && SeekingService.activeStrength(p,power)>0){
                String metal=power.substring(2);double fuel=PowerData.reserve(p,metal);int ticks=flareTicks(fuel,PowerConfig.RESERVE_CAP.get(),(int)Math.ceil(EnhancementConfig.FLARE_TICKS.get()*efficiency(PowerAttributes.strength(p,"a:duralumin"))));if(ticks>0){bursts.put(metal,new Burst(fuel,ticks,now));longest=Math.max(longest,ticks);}
            }
            if(!bursts.isEmpty()){bursts.put("duralumin",new Burst(PowerData.reserve(p,"duralumin"),longest,now));BURSTS.put(p.getUUID(),bursts);}
        }
    }
    public static void finish(ServerPlayer p){
        var bursts=BURSTS.get(p.getUUID());
        if(bursts!=null){long now=p.level().getGameTime();for(var it=bursts.entrySet().iterator();it.hasNext();){var entry=it.next();var b=entry.getValue();long elapsed=Math.max(0,now-b.start+1);double due=b.fuel*Math.min(1,elapsed/(double)b.ticks);if(PowerConfig.CONSUMPTION.get())PowerData.reserve(p,entry.getKey(),PowerData.reserve(p,entry.getKey())-Math.max(0,due-b.spent));b.spent=due;if(elapsed>=b.ticks){if(PowerConfig.CONSUMPTION.get())PowerData.reserve(p,entry.getKey(),PowerData.reserve(p,entry.getKey())<1e-8?0:PowerData.reserve(p,entry.getKey()));PowerData.state(p,"a:"+entry.getKey(),0);it.remove();}}if(bursts.isEmpty())BURSTS.remove(p.getUUID());}
        else if(usable(p,"duralumin") && PowerConfig.CONSUMPTION.get()){int state=state(p,"duralumin");PowerData.reserve(p,"duralumin",PowerData.reserve(p,"duralumin")-PowerConfig.burnCost("duralumin",EnhancementConfig.DRAIN.get())/20/efficiency(PowerAttributes.strength(p,"a:duralumin"))*PowerRules.levelMultiplier(state,PowerConfig.LOW_DRAIN.get(),1,PowerConfig.FLARE_DRAIN.get()));if(PowerData.reserve(p,"duralumin")<=0)PowerData.state(p,"a:duralumin",0);}
    }
    public static void clear(ServerPlayer p){BURSTS.remove(p.getUUID());}
    public static void clear(){BURSTS.clear();}
    private EnhancementService() { }
}
