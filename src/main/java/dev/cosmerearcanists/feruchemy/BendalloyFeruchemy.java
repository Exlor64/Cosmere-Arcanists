package dev.cosmerearcanists.feruchemy;

import dev.cosmerearcanists.*;
import dev.cosmerearcanists.power.*;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** One stored nutrition point pays for one hunger or saturation point, before compression.
 * Hunger uses a bounded unpaid scheduling clock because vanilla food level is an integer.
 * No food is granted or taken until an entire hunger point can be transferred. */
public final class BendalloyFeruchemy {
    private static final String POWER="f:bendalloy";
    private record Clock(int state,double pending,double signal){}
    private static final Map<ServerPlayer,Clock> CLOCKS=new WeakHashMap<>();
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&FeruchemyConfig.BENDALLOY.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.BENDALLOY).get();}
    public static int storeLevels(ServerPlayer p){return FeruchemyLevels.storeLevels(PowerAttributes.strength(p,POWER),FeruchemyConfig.BENDALLOY_STORE_STEP.get(),FeruchemyConfig.BENDALLOY_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());}
    public static int tapLevels(ServerPlayer p){return FeruchemyLevels.tapLevels(PowerAttributes.strength(p,POWER),FeruchemyConfig.BENDALLOY_TAP_LEVELS.get());}
    public static double tapRate(int level){return Math.min(FeruchemyConfig.BENDALLOY_CAP.get(),FeruchemyConfig.BENDALLOY_TAP_RATE.get()*Math.pow(Math.max(0,level),FeruchemyConfig.BENDALLOY_TAP_EXPONENT.get()));}
    public static void clear(ServerPlayer p){CLOCKS.remove(p);}
    private static void stop(ServerPlayer p){PowerData.state(p,POWER,0);clear(p);}
    public static double strength(ServerPlayer p){var c=CLOCKS.get(p);return c!=null&&configured()&&p.isAlive()&&!p.isSpectator()&&PowerData.state(p,POWER)==c.state()?PowerAttributes.strength(p,POWER)*c.signal():0;}
    public static void tick(ServerPlayer p){
        int state=PowerData.state(p,POWER);double attribute=PowerAttributes.strength(p,POWER);
        if(!configured()||!p.isAlive()||p.isSpectator()||p.getAbilities().invulnerable||state==0||attribute<=0){clear(p);return;}
        var food=p.getFoodData();var old=CLOCKS.get(p);double pending=old!=null&&old.state()==state?old.pending():0;
        if(state<0){
            if(StorageActivity.blocked(p)){stop(p);return;}
            double rate=FeruchemyLevels.storedFraction(attribute,-state,FeruchemyConfig.BENDALLOY_STORE_STEP.get(),FeruchemyConfig.BENDALLOY_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get())*FeruchemyConfig.BENDALLOY_STORE_RATE.get();
            double scale=Metalminds.storageRate(p),space=Metalminds.accessibleSpace(p,"bendalloy")/scale;
            if(space<=1e-9){stop(p);return;}
            if(food.getSaturationLevel()>0){
                double budget=Math.min(1,pending+rate/20),request=Math.min(space,Math.min(food.getSaturationLevel(),budget));
                float after=(float)Math.max(0,food.getSaturationLevel()-request);
                if(food.getSaturationLevel()-after>request)after=Math.nextUp(after);
                double removable=Math.max(0,food.getSaturationLevel()-after);
                double moved=removable>0?Metalminds.storePerTick(p,"bendalloy",removable):0;
                food.setSaturation((float)Math.max(0,food.getSaturationLevel()-moved));CLOCKS.put(p,new Clock(state,Math.max(0,budget-moved),moved*20));
                if(space<budget-1e-7||moved<removable-1e-9)stop(p);return;
            }
            if(food.getFoodLevel()<=FeruchemyConfig.BENDALLOY_FOOD_FLOOR.get()||space<1-1e-9){stop(p);return;}
            pending+=rate/20;int points=(int)Math.min(Math.floor(pending+1e-9),Math.min(food.getFoodLevel()-FeruchemyConfig.BENDALLOY_FOOD_FLOOR.get(),Math.floor(space+1e-9)));
            double moved=points>0?Metalminds.storePerTick(p,"bendalloy",points):0;
            int paid=(int)Math.floor(moved+1e-9);if(paid>0)food.setFoodLevel(food.getFoodLevel()-paid);
            CLOCKS.put(p,new Clock(state,Math.min(1,Math.max(0,pending-paid)),paid*20));return;
        }
        double rate=tapRate(state),compression=Math.pow(Math.max(1,rate/FeruchemyConfig.BENDALLOY_STORE_RATE.get()),FeruchemyConfig.BENDALLOY_LOSS.get());
        double charge=Metalminds.accessibleCharge(p,"bendalloy");if(charge<=1e-9){stop(p);return;}
        if(food.getFoodLevel()<20){
            if(charge+1e-9<compression){stop(p);return;}
            pending+=rate/20;int points=(int)Math.min(Math.floor(pending+1e-9),Math.min(20-food.getFoodLevel(),Math.floor(charge/compression+1e-9)));
            double spent=points>0?-Metalminds.transferAccessible(p,"bendalloy",-points*compression):0;
            int paid=(int)Math.floor(spent/compression+1e-9);if(paid>0)food.setFoodLevel(food.getFoodLevel()+paid);
            CLOCKS.put(p,new Clock(state,Math.min(1,Math.max(0,pending-paid)),paid*20));return;
        }
        double missing=food.getFoodLevel()-food.getSaturationLevel();
        if(missing<=1e-7){clear(p);return;}
        double budget=Math.min(5,pending+rate/20),request=Math.min(charge/compression,Math.min(missing,budget));
        float after=(float)Math.min(food.getFoodLevel(),food.getSaturationLevel()+request);
        if(after-food.getSaturationLevel()>request)after=Math.nextDown(after);
        double gain=Math.max(0,after-food.getSaturationLevel()),cost=gain*compression;
        if(gain>0&&charge-cost<1e-9)cost=charge; // Consume rounding dust on an otherwise funded final saturation transfer.
        double spent=gain>0?-Metalminds.transferAccessible(p,"bendalloy",-cost):0;
        food.setSaturation((float)Math.min(food.getFoodLevel(),food.getSaturationLevel()+spent/compression));CLOCKS.put(p,new Clock(state,Math.max(0,budget-spent/compression),spent/compression*20));
        if(charge/compression<budget-1e-7||spent<gain*compression-1e-9)stop(p);
    }
    @SubscribeEvent public void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
}
