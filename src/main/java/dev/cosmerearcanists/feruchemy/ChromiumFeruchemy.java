package dev.cosmerearcanists.feruchemy;

import dev.cosmerearcanists.*;
import dev.cosmerearcanists.fortune.*;
import dev.cosmerearcanists.power.*;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Stored Fortune funds loot advantages; storage causes bad luck only while actually paid. */
public final class ChromiumFeruchemy {
    private static final String POWER="f:chromium";
    private record Output(int state,long tick,double magnitude){}
    private static final Map<ServerPlayer,Output> ACTIVE=new WeakHashMap<>();
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&FortuneConfig.ENABLED.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.CHROMIUM).get();}
    private static boolean eligible(ServerPlayer p){return configured()&&p.isAlive()&&!p.isSpectator()&&!p.getAbilities().invulnerable&&PowerAttributes.strength(p,POWER)>0;}
    public static int storeLevels(ServerPlayer p){return FeruchemyLevels.storeLevels(PowerAttributes.strength(p,POWER),FortuneConfig.STORE_STEP.get(),FortuneConfig.STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());}
    public static int tapLevels(ServerPlayer p){return FeruchemyLevels.tapLevels(PowerAttributes.strength(p,POWER),FortuneConfig.TAP_LEVELS.get());}
    private static Output output(ServerPlayer p){var out=ACTIVE.get(p);if(out==null||!eligible(p)||PowerData.state(p,POWER)!=out.state())return null;long age=p.level().getGameTime()-out.tick();if(age<0||age>1)return null;for(var a:Metalminds.touching(p))if(Metalminds.usableBy(a.stack(),p,"chromium"))return out;return null;}
    public static double storedFraction(ServerPlayer p){var out=output(p);return out!=null&&out.state()<0?out.magnitude():0;}
    public static double fundedLevels(ServerPlayer p){var out=output(p);return out!=null&&out.state()>0?out.magnitude()/FortuneConfig.TAP_STEP.get():0;}
    public static double fortuneMultiplier(ServerPlayer p){var out=output(p);return out==null?1:out.state()<0?1-out.magnitude():1+out.magnitude();}
    public static double fishingLuck(ServerPlayer p){if(!FortuneConfig.FISHING.get())return 0;double levels=fundedLevels(p);return levels>0?Math.min(64,Math.min(FeruchemicalEnhancement.cap(p,"chromium",FortuneConfig.FISHING_CAP.get()),levels*FortuneConfig.FISHING_STEP.get())):-storedFraction(p)*FortuneConfig.FISHING_PENALTY.get();}
    public static double bonusAttempts(ServerPlayer p){return Math.min(4,Math.min(FeruchemicalEnhancement.cap(p,"chromium",FortuneConfig.BONUS_CAP.get()),fundedLevels(p)*FortuneConfig.BONUS_STEP.get()));}
    public static double strength(ServerPlayer p){var out=output(p);return out==null?0:PowerAttributes.strength(p,POWER)*out.magnitude();}
    public static void clearAll(){ACTIVE.clear();}
    public static void clear(ServerPlayer p){ACTIVE.remove(p);}
    public static void tick(ServerPlayer p){
        int state=PowerData.state(p,POWER);if(!eligible(p)||state==0){if(ACTIVE.containsKey(p))clear(p);return;}
        boolean storing=state<0;double magnitude=storing?FeruchemyLevels.storedFraction(PowerAttributes.strength(p,POWER),-state,FortuneConfig.STORE_STEP.get(),FortuneConfig.STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get()):FeruchemyLevels.extraMass(state,FortuneConfig.TAP_STEP.get(),FortuneConfig.CAP.get());
        double cost=magnitude*(storing?1:Math.pow(Math.max(1,magnitude),FortuneConfig.LOSS.get()))/20;
        if(!storing)magnitude=FeruchemicalEnhancement.effect(p,"chromium",magnitude);
        double moved=storing?Metalminds.storePerTick(p,"chromium",cost):FeruchemicalEnhancement.tap(p,"chromium",cost);double actual=cost>0?magnitude*Math.min(1,Math.abs(moved)/cost):0;
        if(actual<=0){PowerData.state(p,POWER,0);clear(p);return;}ACTIVE.put(p,new Output(state,p.level().getGameTime(),actual));
        if(storing)MisfortuneService.tick(p);
    }
    @SubscribeEvent public void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e){clearAll();}
    @SubscribeEvent public void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,POWER,0);}}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,POWER,0);}}
}
