package dev.cosmerearcanists.power;

import java.util.*;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.investiture.InvestitureContactEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Contact powers consume resources server-side; external surges never edit permanent attributes. */
public final class ContactService {
    private record Use(UUID player,String power){}
    private static final Map<Use,Long> LAST=new HashMap<>();
    private static final class Burst {
        final double fuel,multiplier;final int ticks;final long start;final String dimension;double spent;
        Burst(double fuel,double multiplier,int ticks,ServerPlayer target){this.fuel=fuel;this.multiplier=multiplier;this.ticks=ticks;this.start=target.level().getGameTime();this.dimension=target.level().dimension().location().toString();}
    }
    private static final Map<UUID,Map<String,Burst>> BURSTS=new HashMap<>();
    public static boolean configured(String metal){return ContactConfig.ENABLED.get() && (metal.equals("chromium")?ContactConfig.CHROMIUM.get():metal.equals("nicrosil") && ContactConfig.NICROSIL.get()) && PowerConfig.ENABLED.get() && ServerConfig.ALLOMANCY.enabled().get() && ServerConfig.ALLOMANCY.metals().get(Metal.valueOf(metal.toUpperCase(Locale.ROOT))).get();}
    private static int state(ServerPlayer p,String metal){int value=PowerData.state(p,"a:"+metal);return value==3&&!PowerConfig.FLARING.get()?2:value;}
    public static double strength(ServerPlayer p,String metal){if(!configured(metal)||!p.isAlive()||p.isSpectator()||PowerConfig.CONSUMPTION.get()&&PowerData.reserve(p,metal)<=0)return 0;return PowerAttributes.effectiveStrength(p,"a:"+metal)*PowerRules.levelMultiplier(state(p,metal),PowerConfig.LOW_STRENGTH.get(),PowerConfig.BURN_STRENGTH.get(),PowerConfig.FLARE_STRENGTH.get());}
    private static boolean boostable(String metal){return !Set.of("aluminum","duralumin","chromium","nicrosil").contains(metal);}
    public static boolean committed(ServerPlayer p,String metal){var map=BURSTS.get(p.getUUID());return map!=null && map.containsKey(metal);}
    public static double multiplier(ServerPlayer p,String power){
        if(!configured("nicrosil")||!p.isAlive()||p.isSpectator()||!power.startsWith("a:")||!boostable(power.substring(2)))return 1;
        var map=BURSTS.get(p.getUUID());var b=map==null?null:map.get(power.substring(2));
        return b!=null && b.dimension.equals(p.level().dimension().location().toString()) && p.level().getGameTime()<b.start+b.ticks && PowerAttributes.strength(p,power)>0 && EnhancementService.featureEnabled(power.substring(2))?b.multiplier:1;
    }
    public static EnhancementService.Profile profile(ServerPlayer p){
        int state=state(p,"nicrosil");double burn=PowerRules.levelMultiplier(state,PowerConfig.LOW_STRENGTH.get(),PowerConfig.BURN_STRENGTH.get(),PowerConfig.FLARE_STRENGTH.get());double attribute=strength(p,"nicrosil")/Math.max(.0001,burn);
        double efficiency=EnhancementMath.attributeCurve(attribute,ContactConfig.EFFICIENCY_GROWTH.get());double base=state==1?ContactConfig.LOW_POWER.get():state==2?ContactConfig.BURN_POWER.get():ContactConfig.FLARE_POWER.get();
        double multiplier=1+(base-1)*EnhancementMath.attributeCurve(attribute,ContactConfig.POWER_GROWTH.get());
        return new EnhancementService.Profile(multiplier,1,(int)Math.clamp(Math.ceil(ContactConfig.TICKS.get()*efficiency),1,7200),true);
    }
    /** Resolve the closest intersected body before testing permissions, never reach through another entity. */
    public static LivingEntity target(ServerPlayer p){
        double range=ContactConfig.RANGE.get();var from=p.getEyePosition();var to=from.add(p.getLookAngle().scale(range));LivingEntity target=null;Vec3 hit=null;double closest=range*range;
        for(var e:p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range+1),e->e!=p&&e.isAlive()&&!e.isSpectator())){
            var clip=e.getBoundingBox().clip(from,to);if(clip.isEmpty())continue;double d=clip.get().distanceToSqr(from);if(d<closest){closest=d;target=e;hit=clip.get();}
        }
        if(target==null)return null;var delta=hit.subtract(from);int steps=(int)Math.ceil(delta.length()*4);
        for(int i=0;i<=steps;i++)if(!p.serverLevel().hasChunkAt(BlockPos.containing(from.add(delta.scale(steps==0?0:(double)i/steps)))))return null;
        return p.serverLevel().clip(new ClipContext(from,hit,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS?target:null;
    }
    private static boolean allowed(ServerPlayer source,LivingEntity target){
        if(target==null||target==source||!target.isAlive()||target.isRemoved()||target.isSpectator()||target.level()!=source.level())return false;
        if(target instanceof Player player){if(!ContactConfig.PLAYERS.get()||player.getAbilities().invulnerable)return false;if(ContactConfig.PVP.get()&&(!source.server.isPvpAllowed()||!source.canHarmPlayer(player)))return false;}
        else if(!ContactConfig.MOBS.get())return false;
        return true;
    }
    private static boolean purge(ServerPlayer target){
        if(!ContactConfig.PURGE.get())return false;boolean affected=false;var reserves=PowerData.data(target).getCompound("reserves");
        for(String metal:new HashSet<>(reserves.getAllKeys()))if(PowerData.reserve(target,metal)>0){PowerData.reserve(target,metal,0);affected=true;}
        for(String power:PowerAttributes.keys())if(power.startsWith("a:")&&PowerData.state(target,power)!=0){PowerData.state(target,power,0);affected=true;}
        if(affected){EnhancementService.clear(target);clearTarget(target);}return affected;
    }
    private static boolean surge(ServerPlayer target,EnhancementService.Profile profile){
        var map=BURSTS.computeIfAbsent(target.getUUID(),id->new HashMap<>());boolean affected=false;
        for(String power:PowerData.available(target))if(power.startsWith("a:") && boostable(power.substring(2)) && PowerRules.implemented(power) && SeekingService.activeStrength(target,power)>0){
            String metal=power.substring(2);if(map.containsKey(metal)||EnhancementService.committed(target,metal))continue;
            double fuel=PowerData.reserve(target,metal);int ticks=profile.durationTicks(fuel,PowerConfig.RESERVE_CAP.get());
            if(ticks>0){map.put(metal,new Burst(fuel,profile.strengthMultiplier(),ticks,target));affected=true;}
        }
        if(map.isEmpty())BURSTS.remove(target.getUUID());return affected;
    }
    public static boolean contact(ServerPlayer source,LivingEntity target,String power){
        if(!power.equals("a:chromium")&&!power.equals("a:nicrosil"))return false;String metal=power.substring(2);double strength=strength(source,metal);if(strength<=0||!allowed(source,target)||target(source)!=target)return false;
        long now=source.level().getGameTime();var key=new Use(source.getUUID(),power);var last=LAST.get(key);if(last!=null && now>=last && now-last<ContactConfig.COOLDOWN.get())return false;
        double cost=ContactConfig.COST.get()/EnhancementMath.attributeCurve(strength,ContactConfig.EFFICIENCY_GROWTH.get());if(PowerConfig.CONSUMPTION.get()&&!PowerData.canSpendReserve(source,metal,PowerConfig.reserveCost(cost)))return false;
        var profile=metal.equals("nicrosil")?profile(source):new EnhancementService.Profile(1,1,0,false);
        var event=new InvestitureContactEvent(source,target,metal.equals("chromium")?InvestitureContactEvent.Kind.CHROMIUM:InvestitureContactEvent.Kind.NICROSIL,strength,profile);
        if(ContactConfig.FOREIGN.get())NeoForge.EVENT_BUS.post(event);if(event.isCanceled())return false;
        boolean affected=target instanceof ServerPlayer player && (metal.equals("chromium")?purge(player):surge(player,profile));affected|=event.hasEffects();if(!affected)return false;
        event.applyEffects();if(PowerConfig.CONSUMPTION.get())EnhancementService.consume(source,metal,cost);LAST.put(key,now);
        if(ContactConfig.FEEDBACK.get()){source.displayClientMessage(Component.literal(metal.equals("chromium")?"Chromium: Investiture leeched.":"Nicrosil: active powers surged."),true);if(target instanceof ServerPlayer p)p.displayClientMessage(Component.literal(metal.equals("chromium")?"Your metal reserves were leeched.":"Your burning metals surge with nicrosil."),true);}
        if(target instanceof ServerPlayer p)PowerNetwork.sync(p,List.of());return true;
    }
    public static void activate(ServerPlayer p,String power,AbilityDispatcher.Phase phase){if(phase==AbilityDispatcher.Phase.STOP)return;var target=target(p);if(target!=null)contact(p,target,power);}
    public static void tick(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator()||!configured("nicrosil"))clearTarget(p);
        var map=BURSTS.get(p.getUUID());if(map!=null){var available=PowerData.available(p);long now=p.level().getGameTime();
            for(var it=map.entrySet().iterator();it.hasNext();){var entry=it.next();var b=entry.getValue();String metal=entry.getKey();
                if(!b.dimension.equals(p.level().dimension().location().toString())||!available.contains("a:"+metal)||!EnhancementService.featureEnabled(metal)){it.remove();continue;}
                long age=Math.max(0,now-b.start+1);double due=b.fuel*Math.min(1,age/(double)b.ticks);
                if(PowerConfig.CONSUMPTION.get())PowerData.spendReserve(p,metal,Math.max(0,due-b.spent));b.spent=due;
                if(age>=b.ticks){if(PowerData.reserve(p,metal)<1e-8)PowerData.reserve(p,metal,0);PowerData.state(p,"a:"+metal,0);it.remove();}
            }if(map.isEmpty())BURSTS.remove(p.getUUID());
        }
        for(String metal:List.of("chromium","nicrosil"))if(strength(p,metal)>0){
            if(PowerConfig.CONSUMPTION.get())EnhancementService.burn(p,metal,ContactConfig.DRAIN.get()/20*PowerRules.levelMultiplier(state(p,metal),PowerConfig.LOW_DRAIN.get(),1,PowerConfig.FLARE_DRAIN.get()));
            if(PowerConfig.CONSUMPTION.get()&&PowerData.reserve(p,metal)<=0)PowerData.state(p,"a:"+metal,0);
        }
    }
    public static void clearTarget(ServerPlayer p){BURSTS.remove(p.getUUID());}
    public static void clear(ServerPlayer p){clearTarget(p);LAST.keySet().removeIf(k->k.player.equals(p.getUUID()));}
    @SubscribeEvent public void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void stopped(ServerStoppedEvent e){BURSTS.clear();LAST.clear();}
}
