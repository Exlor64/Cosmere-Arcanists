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
/** Physical strength-seconds live in pewterminds; temporal clocks are not changed. */
public final class PewterFeruchemy {
    private static final ResourceLocation MODIFIER=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"feruchemical_pewter_speed");
    private static final class LeapFlight {
        final long launched;boolean airborne;
        LeapFlight(long launched){this.launched=launched;}
    }
    private static final java.util.Map<ServerPlayer,LeapFlight> LEAPS=new java.util.WeakHashMap<>();
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&FeruchemyConfig.PEWTER.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.PEWTER).get();}
    public static int storeLevels(ServerPlayer p){return FeruchemyLevels.storeLevels(PowerAttributes.strength(p,"f:pewter"),FeruchemyConfig.PEWTER_STORE_STEP.get(),FeruchemyConfig.PEWTER_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());}
    public static int tapLevels(ServerPlayer p){return FeruchemyLevels.tapLevels(PowerAttributes.strength(p,"f:pewter"),FeruchemyConfig.PEWTER_TAP_LEVELS.get());}
    public static double factor(Player p){var a=p.getAttribute(PowerAttributes.PHYSICAL_STRENGTH);var m=a==null?null:a.getModifier(MODIFIER);return m==null?1:1+m.amount();}
    public static double strength(ServerPlayer p){return configured()&&p.isAlive()&&!p.isSpectator()&&PowerData.state(p,"f:pewter")!=0?PowerAttributes.strength(p,"f:pewter")*Math.abs(factor(p)-1):0;}
    private static void modifier(ServerPlayer p,net.minecraft.core.Holder<Attribute> attribute,double factor){var a=p.getAttribute(attribute);if(a==null)return;double value=factor-1;var old=a.getModifier(MODIFIER);if(old!=null&&Math.abs(old.amount()-value)<1e-10)return;a.removeModifier(MODIFIER);if(Math.abs(value)>1e-10)a.addTransientModifier(new AttributeModifier(MODIFIER,value,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
    private static void apply(ServerPlayer p,double factor){
        modifier(p,PowerAttributes.PHYSICAL_STRENGTH,factor);
        modifier(p,Attributes.ATTACK_DAMAGE,factor);
        double bulk=FeruchemyConfig.PEWTER_BULK.get()?Math.min(FeruchemyConfig.PEWTER_BULK_CAP.get(),Math.max(0,factor-FeruchemyConfig.PEWTER_BULK_THRESHOLD.get())*FeruchemyConfig.PEWTER_BULK_RATE.get()):0;
        modifier(p,Attributes.MOVEMENT_SPEED,1-bulk);
        double jump=FeruchemyConfig.PEWTER_JUMPS.get()&&factor>1&&PowerData.state(p,"f:pewter")>0&&PowerService.holdingAbility(p,"f:pewter")?Math.min(FeruchemicalEnhancement.cap(p,"pewter",FeruchemyConfig.PEWTER_JUMP_CAP.get()),Math.sqrt(1+(factor-1)*FeruchemyConfig.PEWTER_JUMP_RATE.get())):1;
        modifier(p,Attributes.JUMP_STRENGTH,jump);
    }
    public static void clear(ServerPlayer p){apply(p,1);}
    @SubscribeEvent public void jump(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event){
        if(!(event.getEntity() instanceof ServerPlayer p)||!configured()||!FeruchemyConfig.PEWTER_JUMPS.get()||PowerData.state(p,"f:pewter")<=0||!PowerService.holdingAbility(p,"f:pewter"))return;
        double extra=Math.max(0,factor(p)-1);if(extra<=0||p.getDeltaMovement().y<=0)return;
        var jumpModifier=p.getAttribute(Attributes.JUMP_STRENGTH).getModifier(MODIFIER);
        boolean enhanced=jumpModifier!=null&&jumpModifier.amount()>0;
        int input=PowerService.movementInputs(p,"f:pewter");double forward=((input&4)!=0?1:0)-((input&8)!=0?1:0),left=((input&16)!=0?1:0)-((input&32)!=0?1:0);double yaw=Math.toRadians(p.getYRot());var direction=new net.minecraft.world.phys.Vec3(-Math.sin(yaw)*forward+Math.cos(yaw)*left,0,Math.cos(yaw)*forward+Math.sin(yaw)*left);
        if(direction.lengthSqr()>=1e-8){double boost=Math.min(FeruchemicalEnhancement.cap(p,"pewter",FeruchemyConfig.PEWTER_LEAP_CAP.get()),Math.sqrt(extra)*FeruchemyConfig.PEWTER_LEAP_RATE.get());if(boost>0){p.setDeltaMovement(p.getDeltaMovement().add(direction.normalize().scale(boost)));p.hurtMarked=true;enhanced=true;}}
        if(enhanced&&FeruchemyConfig.PEWTER_SAFE_LANDING.get())LEAPS.put(p,new LeapFlight(p.level().getGameTime()));
    }
    private static void updateLeap(ServerPlayer p){
        var flight=LEAPS.get(p);if(flight==null)return;
        if(!p.isAlive()||p.isSpectator()||!configured()||!FeruchemyConfig.PEWTER_SAFE_LANDING.get()||p.isInWater()||p.isInLava()||p.onClimbable()||p.isPassenger()||p.isFallFlying()||p.getAbilities().flying){LEAPS.remove(p);return;}
        if(!p.onGround())flight.airborne=true;
        else if(flight.airborne||p.level().getGameTime()>flight.launched+1)LEAPS.remove(p);
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public void land(net.neoforged.neoforge.event.entity.living.LivingFallEvent event){
        if(!(event.getEntity() instanceof ServerPlayer p)||LEAPS.remove(p)==null)return;
        if(configured()&&FeruchemyConfig.PEWTER_SAFE_LANDING.get())event.setDamageMultiplier(0);
    }

    public static void tick(ServerPlayer p){
        updateLeap(p);
        double factor=1;int state=PowerData.state(p,"f:pewter");double attribute=PowerAttributes.strength(p,"f:pewter");
        if(configured()&&p.isAlive()&&!p.isSpectator()&&attribute>0&&state!=0){
            boolean storing=state<0;double magnitude=storing?FeruchemyLevels.storedFraction(attribute,-state,FeruchemyConfig.PEWTER_STORE_STEP.get(),FeruchemyConfig.PEWTER_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get()):FeruchemyLevels.extraMass(state,FeruchemyConfig.PEWTER_TAP_STEP.get(),FeruchemyConfig.PEWTER_CAP.get());
            double cost=storing?magnitude/20:magnitude*Math.pow(Math.max(1,magnitude),FeruchemyConfig.PEWTER_LOSS.get())/20;
        if(!storing)magnitude=FeruchemicalEnhancement.effect(p,"pewter",magnitude);
            double moved=(storing?Metalminds.storePerTick(p,"pewter",cost):FeruchemicalEnhancement.tap(p,"pewter",cost));
            if(cost>0&&Math.abs(moved)>0)factor=storing?1-Math.abs(moved)*20:1+magnitude*Math.min(1,Math.abs(moved)/cost);
            if(cost<=0||Math.abs(moved)<cost-1e-9)PowerData.state(p,"f:pewter",0);
        }apply(p,factor);
    }
    @SubscribeEvent public void death(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer p){LEAPS.remove(p);clear(p);}}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){LEAPS.remove(p);clear(p);PowerData.state(p,"f:pewter",0);}}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p){LEAPS.remove(p);clear(p);PowerData.state(p,"f:pewter",0);}}
}
