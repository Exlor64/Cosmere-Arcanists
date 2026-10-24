package dev.cosmerearcanists.feruchemy;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.power.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
/** Paid Identity storage authorizes only newly stored unkeyed reserves; never rewrites existing bindings. */
public final class AluminumFeruchemy {
    private static final String POWER="f:aluminum";
    private static final ResourceLocation MODIFIER=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"feruchemical_aluminum_identity");
    private record Output(int state,long tick,double fraction,double magnitude){}
    private static final Map<ServerPlayer,Output> ACTIVE=new WeakHashMap<>();
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&FeruchemyConfig.ALUMINUM.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.ALUMINUM).get();}
    private static boolean eligible(ServerPlayer p){return configured()&&p.isAlive()&&!p.isSpectator()&&!p.getAbilities().invulnerable&&PowerAttributes.strength(p,POWER)>0;}
    public static double maximumStored(ServerPlayer p){return Math.min(1,PowerAttributes.strength(p,POWER)*FeruchemyConfig.ALUMINUM_STORE_PER_ATTRIBUTE.get());}
    public static int storeLevels(ServerPlayer p){return (int)Math.min(FeruchemyLevels.MAX_LEVELS,Math.ceil(maximumStored(p)/FeruchemyConfig.ALUMINUM_STORE_STEP.get()-1e-9));}
    public static int tapLevels(ServerPlayer p){return FeruchemyLevels.tapLevels(PowerAttributes.strength(p,POWER),FeruchemyConfig.ALUMINUM_TAP_LEVELS.get());}
    public static boolean unkeyedStorage(ServerPlayer p){
        var out=ACTIVE.get(p);if(out==null||out.state()>=0||out.state()!=PowerData.state(p,POWER)||out.tick()!=p.level().getGameTime()||out.fraction()<1-1e-9||!eligible(p)||maximumStored(p)<1-1e-9||StorageActivity.blocked(p)||p.getAttributeValue(PowerAttributes.IDENTITY)>1e-9)return false;
        for(var a:Metalminds.touching(p))if(Metalminds.bound(a.stack())&&Metalminds.usableBy(a.stack(),p,"aluminum")&&!Metalminds.unkeyed(a.stack()))return true;
        return false;
    }
    private static void identity(ServerPlayer p,double factor){var a=p.getAttribute(PowerAttributes.IDENTITY);if(a==null)return;var old=a.getModifier(MODIFIER);double amount=factor-1;if(old!=null&&Math.abs(old.amount()-amount)<1e-10)return;a.removeModifier(MODIFIER);if(Math.abs(amount)>1e-10)a.addTransientModifier(new AttributeModifier(MODIFIER,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
    public static void clear(ServerPlayer p){ACTIVE.remove(p);identity(p,1);}
    public static double strength(ServerPlayer p){var out=ACTIVE.get(p);return out!=null&&eligible(p)&&PowerData.state(p,POWER)==out.state()?PowerAttributes.strength(p,POWER)*out.magnitude():0;}
    /** Read funded output, never the selected tap level or an externally modified Identity attribute. */
    public static double emotionalResistance(ServerPlayer p){
        var out=ACTIVE.get(p);
        if(!FeruchemyConfig.ALUMINUM_EMOTIONS.get()||out==null||out.state()<=0||out.state()!=PowerData.state(p,POWER)||out.tick()!=p.level().getGameTime()||!eligible(p))return 0;
        boolean contact=false;for(var a:Metalminds.touching(p))if(Metalminds.usableBy(a.stack(),p,"aluminum")){contact=true;break;}
        if(!contact)return 0;
        return Math.min(FeruchemicalEnhancement.fractionCap(p,"aluminum",FeruchemyConfig.ALUMINUM_EMOTION_CAP.get()),out.magnitude()/FeruchemyConfig.ALUMINUM_TAP_STEP.get()*FeruchemyConfig.ALUMINUM_EMOTION_STEP.get());
    }
    public static void tick(ServerPlayer p){
        StorageActivity.tick(p);ACTIVE.remove(p);int state=PowerData.state(p,POWER);if(!eligible(p)||state==0){identity(p,1);return;}
        boolean storing=state<0;double magnitude=storing?Math.min(maximumStored(p),-state*FeruchemyConfig.ALUMINUM_STORE_STEP.get()):FeruchemyLevels.extraMass(state,FeruchemyConfig.ALUMINUM_TAP_STEP.get(),FeruchemyConfig.ALUMINUM_CAP.get());
        double cost=magnitude*(storing?1:Math.pow(Math.max(1,magnitude),FeruchemyConfig.ALUMINUM_LOSS.get()))/20;
        if(!storing)magnitude=FeruchemicalEnhancement.effect(p,"aluminum",magnitude);
        // Identity funds its own suppression while still keyed; only subsequent other-attribute storage may be unkeyed.
        double moved=storing?Metalminds.storePerTick(p,"aluminum",cost):FeruchemicalEnhancement.tap(p,"aluminum",cost);
        double actual=cost>0?magnitude*Math.min(1,Math.abs(moved)/cost):0;identity(p,storing?1-actual:1+actual);
        if(actual>0)ACTIVE.put(p,new Output(state,p.level().getGameTime(),storing?actual:0,actual));
        if(cost<=0||Math.abs(moved)<cost-1e-9){PowerData.state(p,POWER,0);clear(p);}
    }
    @SubscribeEvent public void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,POWER,0);}}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,POWER,0);}}
}
