package dev.cosmerearcanists.feruchemy;

import dev.cosmerearcanists.*;
import dev.cosmerearcanists.power.*;
import dev.cosmerearcanists.mixin.BronzeEffectAccess;
import java.util.*;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Stored determination funds mitigation, steadiness and a low-health second wind. */
public final class ElectrumFeruchemy {
    private static final String POWER="f:electrum";
    private static final ResourceLocation MODIFIER=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"feruchemical_electrum_determination");
    private record Output(int state,long tick,double magnitude){}
    private record Clock(MobEffectInstance effect,double remainder){}
    private static final Map<ServerPlayer,Output> ACTIVE=new WeakHashMap<>();
    private static final Map<ServerPlayer,Clock> SLOW_CLOCKS=new WeakHashMap<>();
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&FeruchemyConfig.ELECTRUM.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.ELECTRUM).get();}
    private static boolean eligible(ServerPlayer p){return configured()&&p.isAlive()&&!p.isSpectator()&&!p.getAbilities().invulnerable&&PowerAttributes.strength(p,POWER)>0;}
    public static int storeLevels(ServerPlayer p){return FeruchemyLevels.storeLevels(PowerAttributes.strength(p,POWER),FeruchemyConfig.ELECTRUM_STORE_STEP.get(),FeruchemyConfig.ELECTRUM_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get());}
    public static int tapLevels(ServerPlayer p){return FeruchemyLevels.tapLevels(PowerAttributes.strength(p,POWER),FeruchemyConfig.ELECTRUM_TAP_LEVELS.get());}
    /** A paid interval lasts through the next player update, including damage earlier in that server tick. */
    private static Output output(ServerPlayer p){var out=ACTIVE.get(p);if(out==null||!eligible(p)||PowerData.state(p,POWER)!=out.state())return null;long age=p.level().getGameTime()-out.tick();if(age<0||age>1)return null;for(var a:Metalminds.touching(p))if(Metalminds.usableBy(a.stack(),p,"electrum"))return out;return null;}
    public static double fundedLevels(ServerPlayer p){var out=output(p);return out!=null&&out.state()>0?out.magnitude()/FeruchemyConfig.ELECTRUM_TAP_STEP.get():0;}
    public static double strength(ServerPlayer p){var out=output(p);return out==null?0:PowerAttributes.strength(p,POWER)*out.magnitude();}
    public static double damageReduction(ServerPlayer p){return FeruchemyConfig.ELECTRUM_DAMAGE.get()?(FeruchemicalEnhancement.capMultiplier(p,"electrum")>1?Math.min(FeruchemicalEnhancement.fractionCap(p,"electrum",FeruchemyConfig.ELECTRUM_DAMAGE_CAP.get()),fundedLevels(p)*FeruchemyConfig.ELECTRUM_DAMAGE_STEP.get()):ElectrumMath.damageReduction(fundedLevels(p),FeruchemyConfig.ELECTRUM_DAMAGE_STEP.get(),FeruchemyConfig.ELECTRUM_DAMAGE_CAP.get())):0;}
    public static double knockbackMultiplier(Entity e){if(!(e instanceof ServerPlayer p)||!FeruchemyConfig.ELECTRUM_KNOCKBACK.get())return 1;var out=output(p);if(out==null)return 1;return out.state()<0?1+out.magnitude()*FeruchemyConfig.ELECTRUM_STORE_KNOCKBACK.get():1-Math.min(FeruchemicalEnhancement.fractionCap(p,"electrum",FeruchemyConfig.ELECTRUM_KNOCKBACK_CAP.get()),fundedLevels(p)*FeruchemyConfig.ELECTRUM_KNOCKBACK_STEP.get());}
    @SubscribeEvent public void mitigate(LivingDamageEvent.Pre e){if(e.getEntity() instanceof ServerPlayer p&&!e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)&&Float.isFinite(e.getNewDamage())&&e.getNewDamage()>0)e.setNewDamage((float)(e.getNewDamage()*(1-damageReduction(p))));}
    private static void modifier(ServerPlayer p,net.minecraft.core.Holder<Attribute> attribute,double factor){var a=p.getAttribute(attribute);if(a==null)return;var old=a.getModifier(MODIFIER);double amount=factor-1;if(old!=null&&Math.abs(old.amount()-amount)<1e-10)return;a.removeModifier(MODIFIER);if(Math.abs(amount)>1e-10)a.addTransientModifier(new AttributeModifier(MODIFIER,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
    private static void apply(ServerPlayer p,int state,double magnitude){
        double mining=1,attack=1;
        if(state<0&&FeruchemyConfig.ELECTRUM_STORE_MINING.get())mining=1-Math.min(.95,magnitude*FeruchemyConfig.ELECTRUM_MINING_PENALTY.get());
        if(state>0&&FeruchemyConfig.ELECTRUM_SECOND_WIND.get()&&p.getMaxHealth()>0&&p.getHealth()/p.getMaxHealth()<=FeruchemyConfig.ELECTRUM_LOW_HEALTH.get()+1e-6&&magnitude/FeruchemyConfig.ELECTRUM_TAP_STEP.get()+1e-9>=FeruchemyConfig.ELECTRUM_WIND_LEVEL.get()){double bonus=Math.min(FeruchemicalEnhancement.cap(p,"electrum",FeruchemyConfig.ELECTRUM_WIND_CAP.get()),magnitude/FeruchemyConfig.ELECTRUM_TAP_STEP.get()*FeruchemyConfig.ELECTRUM_WIND_STEP.get());mining+=bonus;attack+=bonus;}
        modifier(p,Attributes.BLOCK_BREAK_SPEED,mining);modifier(p,Attributes.ATTACK_SPEED,attack);
    }
    private static void shortenSlowness(ServerPlayer p,double level){
        var effect=p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);double fraction=FeruchemyConfig.ELECTRUM_SLOW.get()?Math.min(FeruchemicalEnhancement.fractionCap(p,"electrum",FeruchemyConfig.ELECTRUM_SLOW_CAP.get()),level*FeruchemyConfig.ELECTRUM_SLOW_STEP.get()):0;
        if(effect==null||effect.isInfiniteDuration()||fraction<=0){SLOW_CLOCKS.remove(p);return;}var previous=SLOW_CLOCKS.get(p);double clock=fraction/(1-fraction)+(previous!=null&&previous.effect()==effect?previous.remainder():0);int steps=(int)Math.floor(clock+1e-9);SLOW_CLOCKS.put(p,new Clock(effect,Math.max(0,clock-steps)));if(steps<=0)return;
        boolean changed=false;var seen=Collections.newSetFromMap(new IdentityHashMap<MobEffectInstance,Boolean>());for(var current=effect;current!=null&&seen.size()<32&&seen.add(current);current=((BronzeEffectAccess)current).cosmerearcanists$hiddenEffect()){if(current.isInfiniteDuration())continue;int duration=Math.max(1,current.getDuration()-steps);if(duration!=current.getDuration()){((BronzeEffectAccess)current).cosmerearcanists$duration(duration);changed=true;}}
        if(changed&&(p.tickCount%20==0||effect.getDuration()<=1))p.connection.send(new ClientboundUpdateMobEffectPacket(p.getId(),effect,false));
    }
    public static void clear(ServerPlayer p){ACTIVE.remove(p);SLOW_CLOCKS.remove(p);apply(p,0,0);}
    public static void tick(ServerPlayer p){
        int state=PowerData.state(p,POWER);if(!eligible(p)||state==0){clear(p);return;}boolean storing=state<0;double magnitude=storing?FeruchemyLevels.storedFraction(PowerAttributes.strength(p,POWER),-state,FeruchemyConfig.ELECTRUM_STORE_STEP.get(),FeruchemyConfig.ELECTRUM_STORE_PER_ATTRIBUTE.get(),FeruchemyConfig.MAX_STORE.get()):FeruchemyLevels.extraMass(state,FeruchemyConfig.ELECTRUM_TAP_STEP.get(),FeruchemyConfig.ELECTRUM_CAP.get());
        double cost=magnitude*(storing?1:Math.pow(Math.max(1,magnitude),FeruchemyConfig.ELECTRUM_LOSS.get()))/20;if(!storing)magnitude=FeruchemicalEnhancement.effect(p,"electrum",magnitude);double moved=storing?Metalminds.storePerTick(p,"electrum",cost):FeruchemicalEnhancement.tap(p,"electrum",cost);double actual=cost>0?magnitude*Math.min(1,Math.abs(moved)/cost):0;
        if(actual<=0){PowerData.state(p,POWER,0);clear(p);return;}ACTIVE.put(p,new Output(state,p.level().getGameTime(),actual));apply(p,state,actual);shortenSlowness(p,storing?0:actual/FeruchemyConfig.ELECTRUM_TAP_STEP.get());
    }
    @SubscribeEvent public void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,POWER,0);}}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);PowerData.state(p,POWER,0);}}
}
