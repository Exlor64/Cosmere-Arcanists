package dev.cosmerearcanists.compounding;

import dev.cosmerearcanists.Metal;
import dev.cosmerearcanists.feruchemy.*;
import dev.cosmerearcanists.filing.*;
import dev.cosmerearcanists.nicrosil.*;
import dev.cosmerearcanists.power.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Direct reserves-to-charge storage and edible charged nuggets. No seed or secondary activation. */
public final class CompoundingService {
    private record Boost(long end,double bonus){}
    private static final Map<ServerPlayer,Map<String,Boost>> BOOSTS=new WeakHashMap<>();
    private static final Map<UUID,Long> TICKS=new HashMap<>();
    private static ResourceLocation modifier(String metal){return ResourceLocation.fromNamespaceAndPath("cosmerearcanists","nugget_boost_"+metal);}
    private static double nuggetMultiplier(ServerPlayer p,String metal){var a=PowerAttributes.instance(p,"a:"+metal);var mod=a==null?null:a.getModifier(modifier(metal));return mod==null?1:1+mod.amount();}
    public static boolean configured(String metal){var enabled=CompoundingConfig.METALS.get(metal);return enabled!=null&&enabled.get()&&CompoundingConfig.ENABLED.get()&&PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&PowerData.powerEnabled("a:"+metal)&&PowerData.powerEnabled("f:"+metal)&&EnhancementService.featureEnabled(metal)&&(metal.equals("copper")?dev.cosmerearcanists.copper.CopperService.configured():metal.equals("nicrosil")?NicrosilService.configured():FeruchemicalEnhancement.enabled(metal));}
    public static boolean eligible(ServerPlayer p,String metal){if(!p.isAlive()||p.isRemoved()||p.isSpectator()||p.getAbilities().invulnerable||!configured(metal)||PowerAttributes.strength(p,"a:"+metal)<=0||PowerAttributes.strength(p,"f:"+metal)<=0)return false;var pair=PowerSources.pair(p,metal);return pair.allomancy()>0&&pair.feruchemy()>0;}
    public static int storeLevels(ServerPlayer p,String power){return power.startsWith("a:")&&CompoundingConfig.FORWARD.get()&&eligible(p,power.substring(2))?(PowerConfig.FLARING.get()?3:2):0;}
    public static boolean active(ServerPlayer p,String metal){return CompoundingConfig.FORWARD.get()&&eligible(p,metal)&&PowerData.controlState(p,"a:"+metal)<0;}
    public static void tick(ServerPlayer p){tickAt(p,p.level().getGameTime());}
    public static void tickAt(ServerPlayer p,long tick){if(tick<=TICKS.getOrDefault(p.getUUID(),Long.MIN_VALUE))return;TICKS.put(p.getUUID(),tick);StorageActivity.tick(p);for(var m:Metal.values()){
        String metal=m.id();if(!active(p,metal)||StorageActivity.blocked(p))continue;
        int mode=-PowerData.controlState(p,"a:"+metal);double gain=CompoundingConfig.gain();double rate=CompoundingConfig.FUEL.get()*PowerConfig.burnMultiplier(metal)*PowerRules.levelMultiplier(mode,PowerConfig.LOW_DRAIN.get(),1,PowerConfig.FLARE_DRAIN.get())*PowerSources.pair(p,metal).allomancy()*nuggetMultiplier(p,metal)*Metalminds.storageRate(p)/20;
        var profile=EnhancementService.profile(p);rate*=profile.burst()?Math.max(1,profile.strengthMultiplier()):profile.consumptionMultiplier();
        double reserve=PowerData.reserve(p,metal),moved;
        if(metal.equals("nicrosil"))moved=NicrosilService.compound(p,Math.min(reserve,rate)*gain);
        else {String channel=metal.equals("tin")?TinFeruchemy.channel(p):metal;double input=CompoundingMath.input(reserve,rate,Metalminds.accessibleSpace(p,channel),gain);moved=input<=0?0:Metalminds.transferAccessible(p,channel,input*gain);}
        if(moved>0){PowerData.spendReserve(p,metal,moved/gain);if(p.tickCount%5==0)dev.cosmerearcanists.investiture.InvestitureSignals.emit(p.serverLevel(),new dev.cosmerearcanists.investiture.InvestitureSignals.Signal(dev.cosmerearcanists.investiture.InvestitureSignals.ALLOMANCY,ResourceLocation.fromNamespaceAndPath("cosmerearcanists",metal),p.getUUID(),p.position(),Math.max(.01,Math.min(4096,moved)),1,20,1,true),10);}
    }}
    public static boolean nugget(ItemStack stack){var source=Filings.source(stack);return CompoundingConfig.EDIBLE.get()&&!stack.isEmpty()&&!Filings.filed(stack)&&source!=null&&source.nuggetUnits()==1;}
    public static boolean canEat(ServerPlayer p,ItemStack stack){if(!nugget(stack)||!p.isAlive()||p.isSpectator())return false;if(!Metalminds.bound(stack))return !NicrosilService.specialized(stack);var source=Filings.source(stack);String channel=Metalminds.storedMetal(stack);if(stack.getCount()!=1||!CompoundingConfig.REVERSE.get()||!eligible(p,source.metal())||!Metalminds.usableBy(stack,p,channel))return false;return !NicrosilService.specialized(stack)||NicrosilService.canAccess(p,stack)&&NicrosilService.contents(p,stack)!=null;}
    /** Returns true only after consuming one nugget. Shared nicrosil data and copper PNGs are erased too. */
    public static boolean eat(ServerPlayer p,ItemStack stack){if(!canEat(p,stack))return false;String metal=Filings.source(stack).metal();boolean charged=Metalminds.bound(stack);double fraction=CompoundingMath.fullness(Metalminds.charge(stack),Metalminds.capacity(stack));double share=1;if(charged){var pair=PowerSources.pair(p,metal);share=Math.min(1,pair.allomancy()/Math.max(1e-12,PowerAttributes.strength(p,"a:"+metal)/nuggetMultiplier(p,metal)));if(!Metalminds.wipe(stack))return false;}
        net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(p,stack);p.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(stack.getItem()));stack.shrink(1);
        if(charged){PowerData.reserve(p,metal,PowerConfig.RESERVE_CAP.get());var boosts=BOOSTS.computeIfAbsent(p,k->new HashMap<>());boosts.put(metal,new Boost(p.level().getGameTime()+CompoundingMath.ticks(fraction,1,CompoundingConfig.SECONDS.get()),CompoundingConfig.BOOST.get()*fraction*share));updateBoosts(p);}
        else if(PowerAttributes.keys().contains("a:"+metal)&&PowerData.powerEnabled("a:"+metal))PowerData.reserve(p,metal,PowerData.reserve(p,metal)+FilingConfig.PER_NUGGET.get()*FilingConfig.RESERVE.get());
        p.hurt(p.damageSources().magic(),CompoundingConfig.DAMAGE.get().floatValue());p.getInventory().setChanged();PowerNetwork.sync(p,List.of());return true;
    }
    public static void updateBoosts(ServerPlayer p){updateBoostsAt(p,p.level().getGameTime());}
    public static void updateBoostsAt(ServerPlayer p,long tick){var map=BOOSTS.get(p);if(map==null)return;for(var it=map.entrySet().iterator();it.hasNext();){var e=it.next();var a=PowerAttributes.instance(p,"a:"+e.getKey());if(a!=null)a.removeModifier(modifier(e.getKey()));if(tick>=e.getValue().end()||!CompoundingConfig.REVERSE.get()||!eligible(p,e.getKey())){it.remove();continue;}if(a!=null)a.addTransientModifier(new AttributeModifier(modifier(e.getKey()),e.getValue().bonus(),AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}if(map.isEmpty())BOOSTS.remove(p);}
    public static void clear(ServerPlayer p){var map=BOOSTS.remove(p);if(map!=null)for(String metal:map.keySet()){var a=PowerAttributes.instance(p,"a:"+metal);if(a!=null)a.removeModifier(modifier(metal));}TICKS.remove(p.getUUID());}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void clone(PlayerEvent.Clone e){if(e.getOriginal() instanceof ServerPlayer p)clear(p);if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void death(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void stopped(ServerStoppedEvent e){for(var p:List.copyOf(BOOSTS.keySet()))clear(p);TICKS.clear();}
}
