package dev.cosmerearcanists.power;

import java.util.*;
import dev.cosmerearcanists.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TinService {
    private static final String OWN_NIGHT="tin_night_vision";
    private static final ThreadLocal<Entity> SOUND_SOURCE=new ThreadLocal<>();
    private static final Set<UUID> NIGHT_WRITES=new HashSet<>();
    private static final Map<UUID,Budget> BUDGETS=new HashMap<>();
    private static final class Budget { long tick=-1;int count;final Map<Long,Long> recent=new HashMap<>(); }
    /** Source identity is scoped around the actual sound call, never guessed from nearby silent entities. */
    public static void withSoundSource(Entity entity,Runnable action){var old=SOUND_SOURCE.get();SOUND_SOURCE.set(entity);try{action.run();}finally{if(old==null)SOUND_SOURCE.remove();else SOUND_SOURCE.set(old);}}
    static Entity soundSource(){return SOUND_SOURCE.get();}
    public static double strength(ServerPlayer p) {
        if(!PowerConfig.ENABLED.get() || !TinConfig.ENABLED.get() || !ServerConfig.ALLOMANCY.enabled().get() || !ServerConfig.ALLOMANCY.metals().get(Metal.TIN).get()
            || !p.isAlive() || p.isSpectator() || PowerConfig.CONSUMPTION.get() && PowerData.reserve(p,"tin")<=0)return 0;
        int state=PowerData.state(p,"a:tin");if(state==3&&!PowerConfig.FLARING.get())state=2;
        return PowerAttributes.effectiveStrength(p,"a:tin")*PowerRules.levelMultiplier(state,PowerConfig.LOW_STRENGTH.get(),PowerConfig.BURN_STRENGTH.get(),PowerConfig.FLARE_STRENGTH.get());
    }
    public static double revealStrength(ServerPlayer p){return TinMath.reveal(strength(p),TinConfig.REVEAL_THRESHOLD.get(),TinConfig.FULL_REVEAL.get());}
    private static boolean looksOwned(MobEffectInstance effect){return effect!=null && effect.getAmplifier()==0 && effect.isAmbient() && !effect.isVisible() && !effect.showIcon() && effect.getDuration()<=320;}
    public static void tick(ServerPlayer p) {
        double power=strength(p);
        if(power>0 && PowerConfig.CONSUMPTION.get()) {
            int state=PowerData.state(p,"a:tin");if(state==3&&!PowerConfig.FLARING.get()){state=2;PowerData.state(p,"a:tin",2);}
            EnhancementService.consume(p,"tin",TinConfig.DRAIN.get()/20*PowerRules.levelMultiplier(state,PowerConfig.LOW_DRAIN.get(),1,PowerConfig.FLARE_DRAIN.get()));
            power=strength(p);
        }
        if(PowerConfig.CONSUMPTION.get() && PowerData.state(p,"a:tin")>0 && PowerData.reserve(p,"tin")<=0)PowerData.state(p,"a:tin",0);
        var data=PowerData.data(p);var effect=p.getEffect(MobEffects.NIGHT_VISION);boolean owned=data.getBoolean(OWN_NIGHT) && looksOwned(effect);
        if(power>0 && TinConfig.NIGHT_VISION.get()) {
            if(effect==null || owned && effect.getDuration()<300){
                NIGHT_WRITES.add(p.getUUID());try{p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,320,0,true,false,false));}finally{NIGHT_WRITES.remove(p.getUUID());}
                data.putBoolean(OWN_NIGHT,true);
            }
            else if(!owned)data.putBoolean(OWN_NIGHT,false);
        } else {
            if(owned)p.removeEffect(MobEffects.NIGHT_VISION);data.putBoolean(OWN_NIGHT,false);
        }
    }
    /** Direct ray only: no chunk loads or speculative paths around walls. Endpoint voxels do not block their own sounds. */
    public static double obstruction(ServerLevel level,Vec3 from,Vec3 to) {
        if(!TinConfig.OBSTRUCTION.get())return 0;
        var first=BlockPos.containing(from);var last=BlockPos.containing(to);var delta=to.subtract(from);
        int steps=(int)Math.ceil(delta.length()*4);var visited=new HashSet<BlockPos>();double result=0;
        for(int i=1;i<steps;i++) {
            var pos=BlockPos.containing(from.add(delta.scale((double)i/steps)));
            if(pos.equals(first)||pos.equals(last)||!visited.add(pos))continue;
            if(!level.hasChunkAt(pos))return Double.POSITIVE_INFINITY;
            var state=level.getBlockState(pos);var shape=state.getCollisionShape(level,pos);
            if(shape.isEmpty() || shape.clip(from,to,pos)==null)continue;
            float hardness=state.getDestroySpeed(level,pos);
            result+=TinConfig.BLOCK_COST.get()+Math.min(hardness<0?TinConfig.HARDNESS_CAP.get():hardness,TinConfig.HARDNESS_CAP.get())*TinConfig.HARDNESS_COST.get();
            if(state.is(BlockTags.WOOL))result+=TinConfig.WOOL_COST.get();
            if(result>TinConfig.MAX_RANGE.get())break;
        }
        return result;
    }
    public static List<AABB> surfaces(ServerLevel level,Vec3 center,double radius,int limit) {
        var boxes=new ArrayList<AABB>();var origin=BlockPos.containing(center);
        for(var offset:PowerService.scanOffsets((int)Math.ceil(radius))) {
            var pos=origin.offset(offset);if(Vec3.atCenterOf(pos).distanceToSqr(center)>radius*radius || !level.hasChunkAt(pos))continue;
            var state=level.getBlockState(pos);var shape=state.getCollisionShape(level,pos);if(shape.isEmpty())continue;
            boolean exposed=!shape.bounds().equals(new AABB(0,0,0,1,1,1));
            if(!exposed)for(var direction:Direction.values()){var neighbor=pos.relative(direction);if(level.hasChunkAt(neighbor)&&!level.getBlockState(neighbor).isCollisionShapeFullBlock(level,neighbor)){exposed=true;break;}}
            if(!exposed)continue;
            for(var box:shape.toAabbs()){boxes.add(box.move(pos));if(boxes.size()>=limit)return List.copyOf(boxes);}
        }
        return List.copyOf(boxes);
    }
    public static PowerNetwork.TinEcho echo(ServerPlayer listener,Vec3 position,Entity source,float volume) {
        double power=strength(listener);if(power<=0 || !TinConfig.HEARING.get() || !Float.isFinite(volume) || volume<=0)return null;
        double potential=TinMath.reveal(power,TinConfig.REVEAL_THRESHOLD.get(),TinConfig.FULL_REVEAL.get());if(potential<=0)return null;
        double range=TinMath.range(power,volume,TinConfig.RANGE.get(),TinConfig.MAX_RANGE.get())*(.5+.5*potential);double distance=listener.getEyePosition().distanceTo(position);
        if(distance>=range)return null;
        double audible=TinMath.audibility(distance,obstruction(listener.serverLevel(),listener.getEyePosition(),position),range);
        if(audible<TinConfig.MIN_AUDIBILITY.get())return null;
        double radius=Math.min(TinConfig.MAX_RADIUS.get(),TinConfig.RADIUS.get()*Math.sqrt(power)*Math.sqrt(Math.min(4,volume)))*Math.sqrt(audible*potential);
        int ticks=(int)Math.clamp(TinConfig.FADE.get()*(.75+.25*Math.sqrt(power))*Math.sqrt(audible)*(.35+.65*potential)*TinConfig.PERSISTENCE.get(),4,Math.max(4,Math.min(200,TinConfig.MAX_FADE.get()*TinConfig.PERSISTENCE.get())));
        int detail=Math.max(4,(int)(TinConfig.SHAPES.get()*potential*Math.sqrt(audible)));
        boolean identified=source!=null && source.level()==listener.level() && !source.isSilent() && source.position().distanceToSqr(position)<9;
        return new PowerNetwork.TinEcho(listener.level().dimension().location().toString(),position,identified?source.getId():-1,
            identified?source.getBoundingBox():new AABB(position,position),ticks,(float)(audible*potential),surfaces(listener.serverLevel(),position,radius,detail));
    }
    private static void emit(Level level,Vec3 position,Entity source,float volume,SoundSource category) {
        if(!(level instanceof ServerLevel server) || !TinConfig.ENABLED.get() || !TinConfig.HEARING.get() || volume<=0 || category==SoundSource.MUSIC || category==SoundSource.RECORDS || category==SoundSource.MASTER)return;
        long tick=server.getGameTime();long key=source==null?BlockPos.containing(position).asLong():Long.MIN_VALUE+source.getId();
        for(var player:server.players()) {
            if(revealStrength(player)<=0)continue;var budget=BUDGETS.computeIfAbsent(player.getUUID(),id->new Budget());
            if(budget.tick!=tick){budget.tick=tick;budget.count=0;budget.recent.entrySet().removeIf(e->tick-e.getValue()>40);}
            if(budget.count>=TinConfig.EVENTS_PER_TICK.get() || tick-budget.recent.getOrDefault(key,-100L)<TinConfig.SOURCE_INTERVAL.get())continue;
            var echo=echo(player,position,source,volume);if(echo==null)continue;
            budget.count++;budget.recent.put(key,tick);PacketDistributor.sendToPlayer(player,echo);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void atPosition(PlayLevelSoundEvent.AtPosition event) {
        if(event.getSound()!=null && !event.isCanceled())emit(event.getLevel(),event.getPosition(),SOUND_SOURCE.get(),event.getNewVolume(),event.getSource());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void atEntity(PlayLevelSoundEvent.AtEntity event) {
        if(event.getSound()!=null && !event.isCanceled() && !event.getEntity().isSilent())emit(event.getLevel(),event.getEntity().position(),event.getEntity(),event.getNewVolume(),event.getSource());
    }
    @SubscribeEvent public void externalNightVision(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added event) {
        if(event.getEntity() instanceof ServerPlayer p && event.getEffectInstance().getEffect().equals(MobEffects.NIGHT_VISION) && !NIGHT_WRITES.contains(p.getUUID()))PowerData.data(p).putBoolean(OWN_NIGHT,false);
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event){BUDGETS.remove(event.getEntity().getUUID());}
    @SubscribeEvent public void stopped(ServerStoppedEvent event){BUDGETS.clear();NIGHT_WRITES.clear();SOUND_SOURCE.remove();}
}
