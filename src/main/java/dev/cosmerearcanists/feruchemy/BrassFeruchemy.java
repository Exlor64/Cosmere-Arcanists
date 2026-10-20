package dev.cosmerearcanists.feruchemy;

import dev.cosmerearcanists.*;
import dev.cosmerearcanists.power.*;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Paid warmth-seconds, independent of Allomantic brass and ordinary fire duration. */
public final class BrassFeruchemy {
    private static final ResourceLocation SLOW=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"feruchemical_brass_cold");
    public static final ResourceKey<DamageType> FIRE_MELEE=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"brass_fire_melee"));
    public static final ResourceKey<DamageType> OVERHEAT=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"brass_overheat"));
    // Transient: saved/relogged players never inherit an unpaid effect or old damage clock.
    private static final Map<ServerPlayer,Status> ACTIVE=new WeakHashMap<>();
    private record Status(double factor,double cold,int coldTicks,int hotTicks,double heatDamage){}
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&FeruchemyConfig.BRASS.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.BRASS).get();}
    public static int storeLevels(ServerPlayer p){return FeruchemyLevels.storeLevels(PowerAttributes.strength(p,"f:brass"),FeruchemyConfig.BRASS_STORE_STEP.get(),FeruchemyConfig.BRASS_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());}
    public static int tapLevels(ServerPlayer p){return FeruchemyLevels.tapLevels(PowerAttributes.strength(p,"f:brass"),FeruchemyConfig.BRASS_TAP_LEVELS.get());}
    public static double levels(ServerPlayer p){var status=ACTIVE.get(p);int state=PowerData.state(p,"f:brass");if(status==null||!configured()||!p.isAlive()||p.isSpectator()||PowerAttributes.strength(p,"f:brass")<=0||state==0)return 0;double f=status.factor();return state<0&&f<1?-(1-f)/FeruchemyConfig.BRASS_STORE_STEP.get():state>0&&f>1?(f-1)/FeruchemyConfig.BRASS_TAP_STEP.get():0;}
    public static double strength(ServerPlayer p){double level=levels(p);return PowerAttributes.strength(p,"f:brass")*Math.abs(level)*(level<0?FeruchemyConfig.BRASS_STORE_STEP.get():FeruchemyConfig.BRASS_TAP_STEP.get());}
    public static double fireReduction(ServerPlayer p){return FeruchemyConfig.BRASS_FIRE.get()?Math.clamp(-levels(p)/FeruchemyConfig.BRASS_IMMUNE_LEVEL.get(),0,1):0;}
    public static boolean warm(ServerPlayer p){return FeruchemyConfig.BRASS_WARMTH.get()&&levels(p)>0;}
    public static double excessHeat(ServerPlayer p){return Math.max(0,levels(p)-FeruchemyConfig.BRASS_HOT_LEVEL.get()-1e-9);}
    private static void slow(ServerPlayer p,double fraction){var a=p.getAttribute(Attributes.MOVEMENT_SPEED);if(a==null)return;double amount=-Math.min(.95,fraction*FeruchemyConfig.BRASS_SLOW.get());var old=a.getModifier(SLOW);if(old!=null&&Math.abs(old.amount()-amount)<1e-10)return;a.removeModifier(SLOW);if(amount<0)a.addTransientModifier(new AttributeModifier(SLOW,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
    public static void clear(ServerPlayer p){ACTIVE.remove(p);slow(p,0);}
    public static void tick(ServerPlayer p){
        int state=PowerData.state(p,"f:brass");double attribute=PowerAttributes.strength(p,"f:brass");
        if(!configured()||!p.isAlive()||p.isSpectator()||state==0||attribute<=0){clear(p);return;}
        boolean storing=state<0;
        double magnitude=storing?FeruchemyLevels.storedFraction(attribute,-state,FeruchemyConfig.BRASS_STORE_STEP.get(),FeruchemyConfig.BRASS_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get()):FeruchemyLevels.extraMass(state,FeruchemyConfig.BRASS_TAP_STEP.get(),FeruchemyConfig.BRASS_CAP.get());
        double cost=magnitude*(storing?1:Math.pow(Math.max(1,magnitude),FeruchemyConfig.BRASS_LOSS.get()))/20;
        double moved=Metalminds.transferAccessible(p,"brass",storing?cost:-cost),paid=cost>0?Math.min(1,Math.abs(moved)/cost):0;
        if(paid<=0){PowerData.state(p,"f:brass",0);clear(p);return;}
        double actual=magnitude*paid,factor=storing?1-actual:1+actual;
        var previous=ACTIVE.get(p);double cold=storing&&FeruchemyConfig.BRASS_COLD.get()?(previous==null?0:previous.cold())+actual:0;
        int coldTicks=cold>=140?(previous==null?0:previous.coldTicks())+1:0;
        double excess=storing?0:Math.max(0,actual/FeruchemyConfig.BRASS_TAP_STEP.get()-FeruchemyConfig.BRASS_HOT_LEVEL.get()-1e-9);
        boolean hot=excess>0&&FeruchemyConfig.BRASS_OVERHEAT.get();int hotTicks=hot?(previous==null?0:previous.hotTicks())+1:0;
        double damage=hot?(previous==null?0:previous.heatDamage())+excess*FeruchemyConfig.BRASS_SELF_DAMAGE.get()/20:0;
        ACTIVE.put(p,new Status(factor,Math.min(140,cold),coldTicks%40,hotTicks%20,hotTicks>=20?0:damage));
        slow(p,storing&&FeruchemyConfig.BRASS_COLD.get()?actual:0);
        if(!storing&&FeruchemyConfig.BRASS_WARMTH.get()){p.setTicksFrozen(0);var movement=p.getAttribute(Attributes.MOVEMENT_SPEED);if(movement!=null)movement.removeModifier(ResourceLocation.withDefaultNamespace("powder_snow"));}
        if(coldTicks>=40)p.hurt(p.damageSources().freeze(),(float)(actual*FeruchemyConfig.BRASS_COLD_DAMAGE.get()));
        if(hotTicks>=20&&damage>0)p.hurt(new DamageSource(p.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(OVERHEAT)),(float)Math.min(1000,damage));
        if(paid<1-1e-9)PowerData.state(p,"f:brass",0);
    }
    @SubscribeEvent public void incoming(LivingIncomingDamageEvent e){if(e.getEntity() instanceof ServerPlayer p&&(e.getSource().is(DamageTypeTags.IS_FIRE)&&fireReduction(p)>=1-1e-9||e.getSource().is(DamageTypeTags.IS_FREEZING)&&warm(p)))e.setCanceled(true);}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGH) public void damage(LivingDamageEvent.Pre e){if(!(e.getEntity() instanceof ServerPlayer p))return;if(e.getSource().is(DamageTypeTags.IS_FIRE))e.setNewDamage((float)(e.getNewDamage()*(1-fireReduction(p))));else if(e.getSource().is(DamageTypeTags.IS_FREEZING)&&warm(p))e.setNewDamage(0);}
    @SubscribeEvent public void melee(LivingDamageEvent.Post e){
        if(e.getNewDamage()<=0||!e.getEntity().isAlive()||!e.getSource().is(DamageTypes.PLAYER_ATTACK)||!(e.getSource().getEntity() instanceof ServerPlayer p)||e.getSource().getDirectEntity()!=p||!FeruchemyConfig.BRASS_MELEE.get())return;
        double extra=excessHeat(p)*FeruchemyConfig.BRASS_FIRE_DAMAGE.get();if(extra<=0)return;
        // Separate fire-tagged hit: respects fire immunity/resistance, armor, PvP and other damage hooks.
        // Only this extra component bypasses the just-landed melee hit's cooldown; it cannot recurse.
        e.getEntity().hurt(new DamageSource(p.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FIRE_MELEE),p),(float)Math.min(100,extra));
    }
    @SubscribeEvent public void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,"f:brass",0);}}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,"f:brass",0);}}
}
