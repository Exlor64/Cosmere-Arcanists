package dev.cosmerearcanists.feruchemy;

import dev.cosmerearcanists.*;
import dev.cosmerearcanists.power.*;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Charge is breath-seconds: one point replaces20 native air ticks before compression. */
public final class CadmiumFeruchemy {
    private static final String POWER="f:cadmium";
    private static final ResourceLocation MODIFIER=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"feruchemical_cadmium_breath");
    private record Status(int state,double airRemainder,double output){}
    private static final Map<ServerPlayer,Status> ACTIVE=new WeakHashMap<>();
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&FeruchemyConfig.CADMIUM.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.CADMIUM).get();}
    private static boolean eligible(ServerPlayer p){return configured()&&p.isAlive()&&!p.isSpectator()&&!p.getAbilities().invulnerable&&PowerAttributes.strength(p,POWER)>0;}
    public static int storeLevels(ServerPlayer p){return FeruchemyLevels.storeLevels(PowerAttributes.strength(p,POWER),FeruchemyConfig.CADMIUM_STORE_STEP.get(),FeruchemyConfig.CADMIUM_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());}
    public static int tapLevels(ServerPlayer p){return FeruchemyLevels.tapLevels(PowerAttributes.strength(p,POWER),FeruchemyConfig.CADMIUM_TAP_LEVELS.get());}
    public static double breathingLevel(){return 1/FeruchemyConfig.CADMIUM_AIR_PER_LEVEL.get();}
    public static double paidLevels(ServerPlayer p){var s=ACTIVE.get(p);return s!=null&&s.state()>0&&PowerData.state(p,POWER)==s.state()&&eligible(p)?s.output()/FeruchemyConfig.CADMIUM_AIR_PER_LEVEL.get():0;}
    public static double excess(ServerPlayer p){return Math.max(0,paidLevels(p)-breathingLevel()-1e-9);}
    public static double strength(ServerPlayer p){var s=ACTIVE.get(p);return s!=null&&eligible(p)&&PowerData.state(p,POWER)==s.state()?PowerAttributes.strength(p,POWER)*s.output():0;}
    public static float activityExhaustion(Player p,float amount){if(!(p instanceof ServerPlayer server)||!FeruchemyConfig.CADMIUM_ENDURANCE.get()||amount<=0)return amount;return (float)(amount*(1-Math.min(FeruchemicalEnhancement.fractionCap(server,"cadmium",FeruchemyConfig.CADMIUM_ENDURANCE_CAP.get()),excess(server)*FeruchemyConfig.CADMIUM_ENDURANCE_STEP.get())));}
    public static float naturalHealing(Player p,float amount){if(!(p instanceof ServerPlayer server)||!FeruchemyConfig.CADMIUM_HEALING.get()||amount<=0)return amount;return (float)(amount*(1+Math.min(FeruchemicalEnhancement.cap(server,"cadmium",FeruchemyConfig.CADMIUM_HEAL_CAP.get()),excess(server)*FeruchemyConfig.CADMIUM_HEAL_STEP.get())));}
    private static void modifier(AttributeInstance a,double amount){if(a==null)return;var old=a.getModifier(MODIFIER);if(old!=null&&Math.abs(old.amount()-amount)<1e-10)return;a.removeModifier(MODIFIER);if(Math.abs(amount)>1e-10)a.addTransientModifier(new AttributeModifier(MODIFIER,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
    private static void bonuses(ServerPlayer p,double excess){
        modifier(p.getAttribute(Attributes.MOVEMENT_SPEED),FeruchemyConfig.CADMIUM_MOVEMENT.get()?Math.min(FeruchemicalEnhancement.cap(p,"cadmium",FeruchemyConfig.CADMIUM_MOVEMENT_CAP.get()),excess*FeruchemyConfig.CADMIUM_MOVEMENT_STEP.get()):0);
        modifier(p.getAttribute(NeoForgeMod.SWIM_SPEED),FeruchemyConfig.CADMIUM_SWIMMING.get()?Math.min(FeruchemicalEnhancement.cap(p,"cadmium",FeruchemyConfig.CADMIUM_SWIM_CAP.get()),excess*FeruchemyConfig.CADMIUM_SWIM_STEP.get()):0);
        var mining=p.getAttribute(Attributes.SUBMERGED_MINING_SPEED);if(mining==null)return;var own=mining.getModifier(MODIFIER);double baseline=own==null?mining.getValue():mining.getValue()/(1+own.amount());
        double progress=FeruchemyConfig.CADMIUM_MINING.get()?Math.min(1,excess*FeruchemyConfig.CADMIUM_MINING_STEP.get()):0;
        modifier(mining,baseline>0&&baseline<1?((baseline+(1-baseline)*progress)/baseline-1):0);
    }
    public static void clear(ServerPlayer p){ACTIVE.remove(p);bonuses(p,0);}
    private static void stop(ServerPlayer p){PowerData.state(p,POWER,0);clear(p);}
    @SubscribeEvent public void breathe(LivingBreatheEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p)||!eligible(p)||PowerData.state(p,POWER)>=0||StorageActivity.blocked(p))return;
        // Keep native underwater consumption, Respiration and Water Breathing; only suppress ambient refill while storage can accept air.
        if(p.getAirSupply()>Math.ceil(p.getMaxAirSupply()*FeruchemyConfig.CADMIUM_AIR_FLOOR.get())&&Metalminds.accessibleSpace(p,"cadmium")>1e-9)e.setRefillAirAmount(0);
    }
    public static void tick(ServerPlayer p){
        int state=PowerData.state(p,POWER);if(!eligible(p)||state==0){clear(p);return;}
        var old=ACTIVE.get(p);double remainder=old!=null&&old.state()==state?old.airRemainder():0;
        if(state<0){
            bonuses(p,0);if(StorageActivity.blocked(p)){stop(p);return;}
            int available=Math.max(0,p.getAirSupply()-(int)Math.ceil(p.getMaxAirSupply()*FeruchemyConfig.CADMIUM_AIR_FLOOR.get()));
            double space=Metalminds.accessibleSpace(p,"cadmium")/Metalminds.storageRate(p)*20;
            if(available<=0||space<1-1e-9){stop(p);return;}
            double fraction=FeruchemyLevels.storedFraction(PowerAttributes.strength(p,POWER),-state,FeruchemyConfig.CADMIUM_STORE_STEP.get(),FeruchemyConfig.CADMIUM_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());
            double budget=remainder+fraction;int requested=(int)Math.min(Math.floor(budget+1e-9),Math.min(available,Math.floor(space+1e-9)));
            double moved=requested>0?Metalminds.storePerTick(p,"cadmium",requested/20.0):0;int paid=(int)Math.floor(moved*20+1e-9);
            if(paid>0)p.setAirSupply(p.getAirSupply()-paid);ACTIVE.put(p,new Status(state,Math.min(1,Math.max(0,budget-paid)),moved*20));
            if(available-paid<=0||space-paid<1-1e-9)stop(p);return;
        }
        double output=Math.min(FeruchemyConfig.CADMIUM_AIR_CAP.get(),state*FeruchemyConfig.CADMIUM_AIR_PER_LEVEL.get());
        double cost=output*Math.pow(Math.max(1,output),FeruchemyConfig.CADMIUM_LOSS.get())/20;
        output=FeruchemicalEnhancement.effect(p,"cadmium",output);
        double spent=-FeruchemicalEnhancement.tap(p,"cadmium",cost);if(spent<=0){stop(p);return;}
        double actual=output*Math.min(1,spent/cost),budget=remainder+actual;int air=(int)Math.floor(budget+1e-9);
        if(air>0)p.setAirSupply(Math.min(p.getMaxAirSupply(),p.getAirSupply()+air));
        // Only a sub-tick remainder survives; full lungs cannot bank future oxygen, and perks require paid output even on land.
        ACTIVE.put(p,new Status(state,Math.max(0,budget-air),actual));bonuses(p,excess(p));
        if(spent<cost-1e-9)stop(p);
    }
    @SubscribeEvent public void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
}
