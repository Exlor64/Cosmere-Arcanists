package dev.cosmerearcanists.feruchemy;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.power.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
/** Weight seconds are conserved; fast tapping adds configurable compression loss. */
public final class IronFeruchemy {
    private static final ResourceLocation MODIFIER=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"feruchemical_iron_mass");
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&FeruchemyConfig.IRON.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.IRON).get();}
    public static int storeLevels(ServerPlayer p){return FeruchemyLevels.storeLevels(PowerAttributes.strength(p,"f:iron"),FeruchemyConfig.STORE_STEP.get(),FeruchemyConfig.STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());}
    public static int tapLevels(ServerPlayer p){return FeruchemyLevels.tapLevels(PowerAttributes.strength(p,"f:iron"),FeruchemyConfig.TAP_LEVELS_PER_ATTRIBUTE.get());}
    public static double massMultiplier(Entity e){if(!(e instanceof Player p)||!p.isAlive()||p.isSpectator())return 1;var instance=p.getAttribute(PowerAttributes.MASS);return instance==null?1:instance.getValue();}
    public static double allomanticMass(ServerPlayer p){return PowerConfig.PLAYER_MASS.get()*(FeruchemyConfig.MASS.get()?massMultiplier(p):1);}
    public static void mass(ServerPlayer p,double factor){
        var gravity=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY);
        if(gravity!=null){double amount=FeruchemyConfig.IRON_GRAVITY.get()?factor-1:0;var old=gravity.getModifier(MODIFIER);if(old==null||Math.abs(old.amount()-amount)>1e-10){gravity.removeModifier(MODIFIER);if(Math.abs(amount)>1e-10)gravity.addTransientModifier(new AttributeModifier(MODIFIER,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}}
        var a=p.getAttribute(PowerAttributes.MASS);if(a==null)return;var old=a.getModifier(MODIFIER);double value=factor-1;if(old!=null&&Math.abs(old.amount()-value)<1e-10)return;a.removeModifier(MODIFIER);if(Math.abs(value)>1e-10)a.addTransientModifier(new AttributeModifier(MODIFIER,value,AttributeModifier.Operation.ADD_VALUE));
    }
    public static double strength(ServerPlayer p){return configured()&&p.isAlive()&&!p.isSpectator()&&PowerData.state(p,"f:iron")!=0&&Math.abs(massMultiplier(p)-1)>1e-8?PowerAttributes.strength(p,"f:iron")*Math.abs(massMultiplier(p)-1):0;}
    /** Actual paid mass bounds the combat benefit, including partially funded final ticks. */
    public static double combatLevels(Entity entity){
        if(!(entity instanceof ServerPlayer p)||!configured()||!p.isAlive()||p.isSpectator()||PowerData.state(p,"f:iron")<=0||PowerAttributes.strength(p,"f:iron")<=0)return 0;
        return Math.max(0,Math.min(PowerData.state(p,"f:iron"),(massMultiplier(p)-1)/FeruchemyConfig.TAP_STEP.get()));
    }
    public static double knockbackMultiplier(Entity entity){
        if(!FeruchemyConfig.KNOCKBACK.get())return 1;
        double mass=massMultiplier(entity);
        return mass<1?1/mass:1-Math.min(FeruchemyConfig.KNOCKBACK_CAP.get(),combatLevels(entity)*FeruchemyConfig.KNOCKBACK_STEP.get());
    }
    @SubscribeEvent public void critical(net.neoforged.neoforge.event.entity.player.CriticalHitEvent event){
        if(FeruchemyConfig.CRITICAL.get()&&event.isVanillaCritical()&&event.isCriticalHit())event.setDamageMultiplier((float)(event.getDamageMultiplier()*(1+combatLevels(event.getEntity())*FeruchemyConfig.CRITICAL_STEP.get())));
    }
    public static void tick(ServerPlayer p){
        StorageActivity.tick(p);
        if(net.neoforged.fml.ModList.get().isLoaded("curios"))CuriosMetalminds.configureSlots(p);
        double factor=1;int state=PowerData.state(p,"f:iron");double strength=PowerAttributes.strength(p,"f:iron");
        if(configured()&&p.isAlive()&&!p.isSpectator()&&state!=0&&strength>0){
            boolean storing=state<0;
            double magnitude=storing?FeruchemyLevels.storedFraction(strength,-state,FeruchemyConfig.STORE_STEP.get(),FeruchemyConfig.STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get()):FeruchemyLevels.extraMass(state,FeruchemyConfig.TAP_STEP.get(),FeruchemyConfig.MAX_MASS.get());
            double cost=storing?magnitude/20:magnitude*Math.pow(Math.max(1,magnitude),FeruchemyConfig.TAP_LOSS.get())/20;
            double moved=(storing?Metalminds.storePerTick(p,"iron",cost):Metalminds.transferAccessible(p,"iron",-cost));
            if(cost>0&&Math.abs(moved)>0)factor=storing?1-Math.abs(moved)*20:1+magnitude*Math.min(1,Math.abs(moved)/cost);
            if(cost<=0||Math.abs(moved)<cost-1e-9)PowerData.state(p,"f:iron",0);
        }
        mass(p,factor);
    }
    @SubscribeEvent public void fall(LivingFallEvent event){double mass=massMultiplier(event.getEntity());if(FeruchemyConfig.FALL.get()&&mass<1)event.setDamageMultiplier((float)(event.getDamageMultiplier()*(mass<=FeruchemyConfig.NO_FALL_MASS.get()+1e-9?0:Math.clamp((mass-FeruchemyConfig.NO_FALL_MASS.get())/(1-FeruchemyConfig.NO_FALL_MASS.get()),0,1))));}
    @SubscribeEvent public void death(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer p)mass(p,1);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){mass(p,1);PowerData.state(p,"f:iron",0);}}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p){mass(p,1);PowerData.state(p,"f:iron",0);}}
}
