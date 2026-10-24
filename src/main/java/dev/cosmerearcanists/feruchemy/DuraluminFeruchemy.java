package dev.cosmerearcanists.feruchemy;

import dev.cosmerearcanists.*;
import dev.cosmerearcanists.connection.*;
import dev.cosmerearcanists.power.*;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Paid Connection-seconds; relationship history is separate from spendable metalmind charge. */
public final class DuraluminFeruchemy {
    private static final String POWER="f:duralumin";
    private record Output(int state,long tick,double magnitude){}
    private static final Map<ServerPlayer,Output> ACTIVE=new WeakHashMap<>();
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&ConnectionConfig.ENABLED.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.DURALUMIN).get();}
    private static boolean eligible(ServerPlayer p){return configured()&&p.isAlive()&&!p.isSpectator()&&!p.getAbilities().invulnerable&&PowerAttributes.strength(p,POWER)>0;}
    public static int storeLevels(ServerPlayer p){return FeruchemyLevels.storeLevels(PowerAttributes.strength(p,POWER),ConnectionConfig.STORE_STEP.get(),ConnectionConfig.STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());}
    public static int tapLevels(ServerPlayer p){return FeruchemyLevels.tapLevels(PowerAttributes.strength(p,POWER),ConnectionConfig.TAP_LEVELS.get());}
    private static Output output(ServerPlayer p){var out=ACTIVE.get(p);if(out==null||!eligible(p)||PowerData.state(p,POWER)!=out.state())return null;long age=p.level().getGameTime()-out.tick();if(age<0||age>1)return null;for(var a:Metalminds.touching(p))if(Metalminds.usableBy(a.stack(),p,"duralumin"))return out;return null;}
    public static double storedFraction(ServerPlayer p){var out=output(p);return out!=null&&out.state()<0?out.magnitude():0;}
    public static double fundedLevels(ServerPlayer p){var out=output(p);return out!=null&&out.state()>0?out.magnitude()/ConnectionConfig.TAP_STEP.get():0;}
    /** Future world-power checks may consume this funded output; this does not grant powers or assign citizenship. */
    public static double connectionMultiplier(ServerPlayer p){var out=output(p);return out==null?1:out.state()<0?1-out.magnitude():1+out.magnitude();}
    public static double strength(ServerPlayer p){var out=output(p);return out==null?0:PowerAttributes.strength(p,POWER)*out.magnitude();}
    public static void clearAll(){ACTIVE.clear();}
    public static void clear(ServerPlayer p){ACTIVE.remove(p);ConnectionService.clearSensing(p);}
    public static void tick(ServerPlayer p){
        int state=PowerData.state(p,POWER);if(!eligible(p)||state==0){if(ACTIVE.containsKey(p))clear(p);return;}
        boolean storing=state<0;double magnitude=storing?FeruchemyLevels.storedFraction(PowerAttributes.strength(p,POWER),-state,ConnectionConfig.STORE_STEP.get(),ConnectionConfig.STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get()):FeruchemyLevels.extraMass(state,ConnectionConfig.TAP_STEP.get(),ConnectionConfig.CAP.get());
        double cost=magnitude*(storing?1:Math.pow(Math.max(1,magnitude),ConnectionConfig.LOSS.get()))/20;
        if(!storing)magnitude=FeruchemicalEnhancement.effect(p,"duralumin",magnitude);
        double moved=storing?Metalminds.storePerTick(p,"duralumin",cost):FeruchemicalEnhancement.tap(p,"duralumin",cost);double actual=cost>0?magnitude*Math.min(1,Math.abs(moved)/cost):0;
        if(actual<=0){PowerData.state(p,POWER,0);clear(p);return;}ACTIVE.put(p,new Output(state,p.level().getGameTime(),actual));
    }
    @SubscribeEvent public void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);ConnectionService.forgetActivity(p);PowerData.state(p,POWER,0);}}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);ConnectionService.forgetActivity(p);PowerData.state(p,POWER,0);}}
}
