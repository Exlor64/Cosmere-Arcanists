package dev.cosmerearcanists.feruchemy;

import dev.cosmerearcanists.power.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Flaring captures charge once into a private, nonrefundable budget: item moves/copies cannot duplicate fuel. */
public final class FeruchemicalEnhancement {
    private static final String TOKEN="cosmerearcanists_duralumin_commitment";
    private static final EnhancementService.Profile NONE=new EnhancementService.Profile(1,1,0,false);
    private static final class Burst {
        final double fuel,boost;final int ticks;final long start;final String dimension;final Set<UUID> pieces;double remaining;long elapsed;
        Burst(double fuel,double boost,int ticks,ServerPlayer p,Set<UUID> pieces){this.fuel=fuel;this.remaining=fuel;this.boost=boost;this.ticks=ticks;this.start=p.level().getGameTime();this.dimension=p.level().dimension().location().toString();this.pieces=pieces;}
    }
    private static final Map<UUID,Map<String,Burst>> BURSTS=new HashMap<>();
    public static boolean enabled(String metal){return switch(metal.split(":")[0]){
        case "iron"->IronFeruchemy.configured();case "steel"->SteelFeruchemy.configured();case "pewter"->PewterFeruchemy.configured();case "gold"->GoldFeruchemy.configured();case "tin"->TinFeruchemy.configured();case "brass"->BrassFeruchemy.configured();case "bronze"->BronzeFeruchemy.configured();case "bendalloy"->BendalloyFeruchemy.configured();case "cadmium"->CadmiumFeruchemy.configured();case "aluminum"->AluminumFeruchemy.configured();case "zinc"->ZincFeruchemy.configured();case "chromium"->ChromiumFeruchemy.configured();case "duralumin"->DuraluminFeruchemy.configured();case "electrum"->ElectrumFeruchemy.configured();default->false;};}
    private static String power(String metal){return "f:"+metal.split(":")[0];}
    private static Burst burst(ServerPlayer p,String metal){var map=BURSTS.get(p.getUUID());return map==null?null:map.get(metal);}
    public static double committedFuel(ServerPlayer p,String metal){var b=burst(p,metal);return b!=null&&b.dimension.equals(p.level().dimension().location().toString())?Math.max(0,b.remaining):0;}
    public static int committedTicks(ServerPlayer p,String metal){var b=burst(p,metal);return b==null?0:(int)Math.max(0,b.start+b.ticks-p.level().getGameTime());}
    public static boolean committed(ServerPlayer p,String metal){return burst(p,metal)!=null;}
    private static boolean valid(ServerPlayer p,String metal,Burst b){
        if(b==null||b.remaining<=1e-10||!EnhancementConfig.FERUCHEMY.get()||!EnhancementService.configured("duralumin")||PowerAttributes.strength(p,"a:duralumin")<=0||!p.isAlive()||p.isSpectator()||!enabled(metal)||PowerAttributes.strength(p,power(metal))<=0||PowerData.state(p,power(metal))<=0||!b.dimension.equals(p.level().dimension().location().toString())||p.level().getGameTime()>=b.start+b.ticks||metal.startsWith("tin:")&&!TinFeruchemy.channel(p).equals(metal))return false;
        for(var a:Metalminds.touching(p)){var root=a.stack().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(Metalminds.usableBy(a.stack(),p,metal)&&root.hasUUID(TOKEN)&&b.pieces.contains(root.getUUID(TOKEN)))return true;}return false;
    }
    public static boolean flaring(ServerPlayer p,String metal){return valid(p,metal,burst(p,metal));}
    public static EnhancementService.Profile profile(ServerPlayer p,String metal){if(!EnhancementConfig.FERUCHEMY.get()||!enabled(metal)||PowerData.state(p,power(metal))<=0||PowerAttributes.strength(p,power(metal))<=0)return NONE;var b=burst(p,metal);if(b!=null)return valid(p,metal,b)?new EnhancementService.Profile(b.boost,1,b.ticks,true):NONE;var profile=EnhancementService.profile(p);return profile.burst()?NONE:profile;}
    public static double boost(ServerPlayer p,String metal){return profile(p,metal).strengthMultiplier();}
    public static double drain(ServerPlayer p,String metal){return profile(p,metal).consumptionMultiplier();}
    public static double capMultiplier(ServerPlayer p,String metal){return EnhancementConfig.OVERCAPS.get()?boost(p,metal):1;}
    public static double cap(ServerPlayer p,String metal,double cap){return EnhancedCaps.scaled(cap,capMultiplier(p,metal));}
    public static double fractionCap(ServerPlayer p,String metal,double cap){return EnhancedCaps.fraction(cap,capMultiplier(p,metal));}
    public static double effect(ServerPlayer p,String metal,double base){double value=base*boost(p,metal);if(EnhancementConfig.OVERCAPS.get())return value;double cap=switch(metal.split(":")[0]){case "iron"->FeruchemyConfig.MAX_MASS.get()-1;case "steel"->FeruchemyConfig.STEEL_CAP.get()-1;case "pewter"->FeruchemyConfig.PEWTER_CAP.get()-1;case "tin"->FeruchemyConfig.TIN_CAP.get()-1;case "brass"->FeruchemyConfig.BRASS_CAP.get()-1;case "bronze"->FeruchemyConfig.BRONZE_CAP.get()-1;case "aluminum"->FeruchemyConfig.ALUMINUM_CAP.get()-1;case "zinc"->FeruchemyConfig.ZINC_CAP.get()-1;case "chromium"->dev.cosmerearcanists.fortune.FortuneConfig.CAP.get()-1;case "duralumin"->dev.cosmerearcanists.connection.ConnectionConfig.CAP.get()-1;case "electrum"->FeruchemyConfig.ELECTRUM_CAP.get()-1;case "cadmium"->FeruchemyConfig.CADMIUM_AIR_CAP.get();case "bendalloy"->FeruchemyConfig.BENDALLOY_CAP.get();case "gold"->FeruchemyConfig.GOLD_CAP.get();default->base;};return Math.min(cap,value);}
    /** Signed base-cost equivalent, so existing partial-payment logic stays conserved. */
    public static double tap(ServerPlayer p,String metal,double baseCost){var b=burst(p,metal);if(b!=null)return valid(p,metal,b)?-baseCost:0;double drain=drain(p,metal);return Metalminds.transferAccessible(p,metal,-baseCost*drain)/drain;}
    public static double available(ServerPlayer p,String metal){var b=burst(p,metal);return b!=null?(valid(p,metal,b)?1e12:0):Metalminds.accessibleCharge(p,metal);}
    public static double physicalSpend(ServerPlayer p,String metal,double cost){var b=burst(p,metal);return b!=null?(valid(p,metal,b)?cost:0):-Metalminds.transferAccessible(p,metal,-cost);}
    public static double emergencyAvailable(ServerPlayer p,String metal){var b=burst(p,metal);return b!=null?(valid(p,metal,b)?b.remaining:0):Metalminds.accessibleCharge(p,metal);}
    public static boolean emergencySpend(ServerPlayer p,String metal,double cost){var b=burst(p,metal);if(!valid(p,metal,b)||cost>b.remaining+1e-9)return false;b.remaining=Math.max(0,b.remaining-cost);return true;}
    /** Called once when the Allomantic duralumin flare commits; copper memories have no consumptive output. */
    public static int begin(ServerPlayer p,EnhancementService.Profile profile){
        if(!EnhancementConfig.FERUCHEMY.get()||!profile.burst())return 0;var bursts=BURSTS.computeIfAbsent(p.getUUID(),unused->new HashMap<>());int longest=0;
        for(String power:PowerData.available(p)){if(!power.startsWith("f:")||PowerData.state(p,power)<=0||!PowerRules.implemented(power))continue;String metal=power.equals("f:tin")?TinFeruchemy.channel(p):power.substring(2);if(!enabled(metal)||bursts.containsKey(metal))continue;
            double fuel=0,maximum=0;var pieces=new ArrayList<Metalminds.Access>();var seen=Collections.newSetFromMap(new IdentityHashMap<ItemStack,Boolean>());
            for(var a:Metalminds.touching(p))if(seen.add(a.stack())&&Metalminds.usableBy(a.stack(),p,metal)){maximum+=Metalminds.capacity(a.stack());if(Metalminds.charge(a.stack())>0){fuel+=Math.min(Metalminds.charge(a.stack()),Metalminds.capacity(a.stack()));pieces.add(a);}}
            int ticks=profile.durationTicks(fuel,maximum);if(ticks<=0||fuel<=1e-10)continue;var tokens=new HashSet<UUID>();double captured=0;
            for(var a:pieces){double took=-Metalminds.transferFor(a.stack(),p,metal,-Math.min(Metalminds.charge(a.stack()),Metalminds.capacity(a.stack())));if(took<=0)continue;captured+=took;UUID token=UUID.randomUUID();tokens.add(token);var root=a.stack().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();root.putUUID(TOKEN,token);a.stack().set(DataComponents.CUSTOM_DATA,CustomData.of(root));a.changed().run();}
            if(captured>0){bursts.put(metal,new Burst(captured,profile.strengthMultiplier(),ticks,p,tokens));longest=Math.max(longest,ticks);}
        }if(bursts.isEmpty())BURSTS.remove(p.getUUID());return longest;
    }
    public static void finish(ServerPlayer p){var map=BURSTS.get(p.getUUID());if(map==null)return;long now=p.level().getGameTime();for(var it=map.entrySet().iterator();it.hasNext();){var e=it.next();var b=e.getValue();long elapsed=Math.max(0,Math.min(b.ticks,now-b.start+1));if(elapsed>b.elapsed){b.remaining=Math.max(0,b.remaining-b.fuel*(elapsed-b.elapsed)/b.ticks);b.elapsed=elapsed;}if(elapsed>=b.ticks||b.remaining<=1e-10||!b.dimension.equals(p.level().dimension().location().toString())){if(e.getKey().startsWith("tin:")?TinFeruchemy.channel(p).equals(e.getKey()):true)PowerData.state(p,power(e.getKey()),0);it.remove();}}if(map.isEmpty())BURSTS.remove(p.getUUID());}
    public static void clear(ServerPlayer p){BURSTS.remove(p.getUUID());}
    public static void clear(){BURSTS.clear();}
    private FeruchemicalEnhancement(){}
}
