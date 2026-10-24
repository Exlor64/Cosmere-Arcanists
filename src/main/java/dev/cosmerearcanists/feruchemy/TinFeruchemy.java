package dev.cosmerearcanists.feruchemy;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.power.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
/** Separate sight and hearing sense-seconds live in tinminds; temporal clocks are not changed. */
public final class TinFeruchemy {
    private static final ResourceLocation MODIFIER=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"feruchemical_tin_speed");
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&FeruchemyConfig.TIN.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.TIN).get();}
    public static int storeLevels(ServerPlayer p){return FeruchemyLevels.storeLevels(PowerAttributes.strength(p,"f:tin"),FeruchemyConfig.TIN_STORE_STEP.get(),FeruchemyConfig.TIN_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());}
    public static int tapLevels(ServerPlayer p){return FeruchemyLevels.tapLevels(PowerAttributes.strength(p,"f:tin"),FeruchemyConfig.TIN_TAP_LEVELS.get());}
    public static int sense(ServerPlayer p){return Math.clamp(PowerData.data(p).getInt("tin_sense"),0,2);}
    public static String channel(ServerPlayer p){return switch(sense(p)){case 1->"tin:hearing";case 2->"tin:touch";default->"tin:sight";};}
    public static void sense(ServerPlayer p,int sense){if(sense<0||sense>2)return;PowerData.state(p,"f:tin",0);clear(p);PowerData.data(p).putInt("tin_sense",sense);}
    public static double factor(Player p,boolean sight){var a=p.getAttribute(sight?PowerAttributes.SIGHT:PowerAttributes.HEARING);return a==null?1:a.getValue();}
    public static double touch(Player p){var a=p.getAttribute(PowerAttributes.TOUCH);return a==null?1:a.getValue();}
    public static double touchLevels(Player p){double factor=touch(p);return factor>=1?(factor-1)/FeruchemyConfig.TIN_TAP_STEP.get():-(1-factor)/FeruchemyConfig.TIN_STORE_STEP.get();}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGH) public void touchDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event){if(event.getEntity() instanceof ServerPlayer p&&configured()&&FeruchemyConfig.TIN_TOUCH.get()&&PowerData.state(p,"f:tin")!=0&&!p.isSpectator()){double level=touchLevels(p);double multiplier=level<0?1-Math.min(.95,-level*FeruchemyConfig.TIN_TOUCH_DAMAGE.get()):1+level*FeruchemyConfig.TIN_TOUCH_DAMAGE.get();event.setNewDamage((float)(event.getNewDamage()*multiplier));}}
    public static double strength(ServerPlayer p){return configured()&&p.isAlive()&&!p.isSpectator()&&PowerData.state(p,"f:tin")!=0?PowerAttributes.strength(p,"f:tin")*Math.max(Math.abs(factor(p,true)-1),Math.max(Math.abs(factor(p,false)-1),Math.abs(touch(p)-1))):0;}
    private static void modifier(ServerPlayer p,net.minecraft.core.Holder<Attribute> attribute,double factor){var a=p.getAttribute(attribute);if(a==null)return;double value=factor-1;var old=a.getModifier(MODIFIER);if(old!=null&&Math.abs(old.amount()-value)<1e-10)return;a.removeModifier(MODIFIER);if(Math.abs(value)>1e-10)a.addTransientModifier(new AttributeModifier(MODIFIER,value,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
    private static void apply(ServerPlayer p,double factor){modifier(p,PowerAttributes.SIGHT,sense(p)==0?factor:1);modifier(p,PowerAttributes.HEARING,sense(p)==1?factor:1);modifier(p,PowerAttributes.TOUCH,sense(p)==2?factor:1);}
    public static void clear(ServerPlayer p){modifier(p,PowerAttributes.SIGHT,1);modifier(p,PowerAttributes.HEARING,1);modifier(p,PowerAttributes.TOUCH,1);}
    public static void tick(ServerPlayer p){
        double factor=1;int state=PowerData.state(p,"f:tin");double attribute=PowerAttributes.strength(p,"f:tin");
        if(configured()&&p.isAlive()&&!p.isSpectator()&&attribute>0&&state!=0&&(sense(p)==0?FeruchemyConfig.TIN_SIGHT.get():sense(p)==1?FeruchemyConfig.TIN_HEARING.get():FeruchemyConfig.TIN_TOUCH.get())){
            boolean storing=state<0;double magnitude=storing?FeruchemyLevels.storedFraction(attribute,-state,FeruchemyConfig.TIN_STORE_STEP.get(),FeruchemyConfig.TIN_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get()):FeruchemyLevels.extraMass(state,FeruchemyConfig.TIN_TAP_STEP.get(),FeruchemyConfig.TIN_CAP.get());
            double cost=storing?magnitude/20:magnitude*Math.pow(Math.max(1,magnitude),FeruchemyConfig.TIN_LOSS.get())/20;
        if(!storing)magnitude=FeruchemicalEnhancement.effect(p,channel(p),magnitude);
            double moved=(storing?Metalminds.storePerTick(p,channel(p),cost):FeruchemicalEnhancement.tap(p,channel(p),cost));
            if(cost>0&&Math.abs(moved)>0)factor=storing?1-Math.abs(moved)*20:1+magnitude*Math.min(1,Math.abs(moved)/cost);
            if(cost<=0||Math.abs(moved)<cost-1e-9)PowerData.state(p,"f:tin",0);
        }apply(p,factor);
    }
    @SubscribeEvent public void death(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,"f:tin",0);}}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,"f:tin",0);}}
}
