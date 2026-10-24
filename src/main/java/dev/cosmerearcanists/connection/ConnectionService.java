package dev.cosmerearcanists.connection;

import dev.cosmerearcanists.feruchemy.*;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-owned organic bonds and private sensing. Bonds need no power, invitation or acceptance. */
public final class ConnectionService {
    private record SharedDrop(UUID player,long expires){}
    private static final Map<net.minecraft.world.entity.item.ItemEntity,SharedDrop> DROPS=new WeakHashMap<>();
    private record Activity(Vec3 position,long activeTick){}
    private static final Map<UUID,Activity> ACTIVITY=new HashMap<>();
    private static final Set<UUID> SENSING=new HashSet<>();
    private static boolean participant(ServerPlayer p){return p.isAlive()&&!p.isSpectator();}
    private static long now(ServerPlayer p){return p.server.overworld().getGameTime();}
    private static void activity(ServerPlayer p,boolean interaction){long time=now(p);var old=ACTIVITY.get(p.getUUID());boolean moved=old==null||old.position().distanceToSqr(p.position())>=.01;ACTIVITY.put(p.getUUID(),new Activity(moved?p.position():old.position(),interaction||moved?time:old.activeTick()));}
    private static boolean active(ServerPlayer p){var old=ACTIVITY.get(p.getUUID());return old!=null&&now(p)-old.activeTick()>=0&&now(p)-old.activeTick()<=ConnectionConfig.IDLE_SECONDS.get()*20L;}
    public static void forgetActivity(ServerPlayer p){ACTIVITY.remove(p.getUUID());}
    public static void clearSensing(ServerPlayer p){if(SENSING.remove(p.getUUID()))PacketDistributor.sendToPlayer(p,new ConnectionPacket(p.level().dimension().location().toString(),List.of()));}
    private static boolean pairEligible(ServerPlayer a,ServerPlayer b){return DuraluminFeruchemy.configured()&&ConnectionConfig.BONDS.get()&&a!=b&&!a.getUUID().equals(b.getUUID())&&a.server==b.server&&a.level()==b.level()&&participant(a)&&participant(b);}
    /** Called once per real server second; repeated calls in that interval cannot duplicate growth. */
    public static boolean nearbyPair(ServerPlayer a,ServerPlayer b){
        if(!pairEligible(a,b)||a.distanceToSqr(b)>Math.pow(ConnectionConfig.PROXIMITY_RANGE.get(),2)||!active(a)&&!active(b))return false;
        double expressed=Math.min(1,DuraluminFeruchemy.connectionMultiplier(a))*Math.min(1,DuraluminFeruchemy.connectionMultiplier(b));
        var data=ConnectionBonds.get(a.server);boolean changed=data.ledger.grow(a.getUUID(),b.getUUID(),ConnectionConfig.PROXIMITY_GAIN.get()/60*expressed,now(a),false,20,ConnectionConfig.MAX_BONDS.get(),ConnectionConfig.MAX_PAIRS.get());if(changed)data.setDirty();return changed;
    }
    public static boolean interaction(ServerPlayer a,ServerPlayer b){
        if(!pairEligible(a,b)||a.distanceToSqr(b)>4096)return false;activity(a,true);activity(b,true);
        var data=ConnectionBonds.get(a.server);double expressed=Math.min(1,DuraluminFeruchemy.connectionMultiplier(a))*Math.min(1,DuraluminFeruchemy.connectionMultiplier(b));boolean changed=data.ledger.grow(a.getUUID(),b.getUUID(),ConnectionMath.interactionGain(ConnectionConfig.INTERACTION_GAIN.get(),data.ledger.strength(a.getUUID(),b.getUUID()),ConnectionConfig.INTERACTION_DIMINISH.get())*expressed,now(a),true,ConnectionConfig.INTERACTION_COOLDOWN.get(),ConnectionConfig.MAX_BONDS.get(),ConnectionConfig.MAX_PAIRS.get());if(changed)data.setDirty();return changed;
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public void tossed(net.neoforged.neoforge.event.entity.item.ItemTossEvent e){if(!e.isCanceled()&&DROPS.size()<4096&&e.getPlayer() instanceof ServerPlayer p&&DuraluminFeruchemy.configured()&&ConnectionConfig.BONDS.get())DROPS.put(e.getEntity(),new SharedDrop(p.getUUID(),now(p)+6000));}
    public static void itemShared(net.minecraft.world.entity.item.ItemEntity item,net.minecraft.world.entity.player.Player receiver,int amount){if(amount<=0||!(receiver instanceof ServerPlayer p))return;var drop=DROPS.get(item);if(drop==null)return;if(item.isRemoved())DROPS.remove(item);if(now(p)>drop.expires())return;var giver=p.server.getPlayerList().getPlayer(drop.player());if(giver!=null)interaction(giver,p);}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public void interact(PlayerInteractEvent.EntityInteract e){if(!e.isCanceled()&&e.getEntity() instanceof ServerPlayer a&&e.getTarget() instanceof ServerPlayer b)interaction(a,b);}
    /** Rivals can form Connections as well; damage must actually reach the target. */
    @SubscribeEvent public void combat(LivingDamageEvent.Post e){
        if(e.getNewDamage()<=0)return;
        if(e.getEntity() instanceof ServerPlayer victim&&e.getSource().getEntity() instanceof ServerPlayer attacker)interaction(attacker,victim);
        else if(e.getEntity() instanceof Mob mob&&e.getSource().getEntity() instanceof ServerPlayer helper){var target=mob.getTarget();if(target instanceof ServerPlayer ally&&ally!=helper)interaction(helper,ally);}
    }
    public static List<ConnectionPacket.Sensed> sensed(ServerPlayer observer){return sensed(observer,observer.server.getPlayerList()::getPlayer);}
    public static List<ConnectionPacket.Sensed> sensed(ServerPlayer observer,java.util.function.Function<UUID,ServerPlayer> online){
        double level=DuraluminFeruchemy.fundedLevels(observer);if(!ConnectionConfig.BONDS.get()||!ConnectionConfig.SENSING.get()||level<ConnectionConfig.FIRST_SENSE.get()||!participant(observer))return List.of();
        double range=Math.min(4096,Math.min(FeruchemicalEnhancement.cap(observer,"duralumin",ConnectionConfig.RANGE_CAP.get()),ConnectionConfig.BASE_RANGE.get()+level*ConnectionConfig.RANGE_STEP.get()));
        double required=ConnectionMath.requiredBond(level,ConnectionConfig.FIRST_SENSE.get(),ConnectionConfig.REQUIRED_BOND.get(),ConnectionConfig.WEAKER_BONDS.get());var bonds=dev.cosmerearcanists.hemalurgy.IdentityAccess.bonds(observer);var candidates=new ArrayList<ServerPlayer>();
        for(var connected:bonds.keySet()){var target=online.apply(connected);if(target==null||target.level()!=observer.level()||!participant(target)||observer.distanceToSqr(target)>range*range)continue;double expressed=ConnectionMath.expressedBond(bonds.getOrDefault(target.getUUID(),0.),DuraluminFeruchemy.connectionMultiplier(target));if(expressed+1e-9>=required)candidates.add(target);}
        candidates.sort(Comparator.<ServerPlayer>comparingDouble(p->-bonds.getOrDefault(p.getUUID(),0.)).thenComparingDouble(observer::distanceToSqr).thenComparing(ServerPlayer::getUUID));
        var result=new ArrayList<ConnectionPacket.Sensed>();for(var target:candidates.subList(0,Math.min(ConnectionConfig.MAX_SENSED.get(),candidates.size()))){var delta=target.position().subtract(observer.position());double distance=delta.length();String name=target.getGameProfile().getName();result.add(new ConnectionPacket.Sensed(name.substring(0,Math.min(64,name.length())),distance<1e-8?new Vec3(0,0,1):delta.scale(1/distance),distance));}return result;
    }
    /** Reduce only new hostile acquisition; existing targets and direct retaliation are retained. */
    @SubscribeEvent public void targeting(LivingChangeTargetEvent e){if(!ConnectionConfig.DETECTION.get()||!(e.getEntity() instanceof Mob mob)||!(mob instanceof Enemy)||!(e.getNewAboutToBeSetTarget() instanceof ServerPlayer p)||mob.getTarget()==p||mob.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET)&&mob.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET).orElse(null)==p||mob.getLastHurtByMob()==p)return;double stored=DuraluminFeruchemy.storedFraction(p);if(stored<=0)return;double range=Math.max(2,mob.getAttributeValue(Attributes.FOLLOW_RANGE)*ConnectionMath.detectionMultiplier(stored,ConnectionConfig.DETECTION_REDUCTION.get()));if(mob.distanceToSqr(p)>range*range)e.setNewAboutToBeSetTarget(null);}
    public static boolean ignoreAvoidance(PathfinderMob mob,LivingEntity avoided,float originalRange){if(!ConnectionConfig.ANIMALS.get()||!(mob instanceof Animal)||!(avoided instanceof ServerPlayer p)||mob.getLastHurtByMob()!=null&&mob.tickCount-mob.getLastHurtByMobTimestamp()<100)return false;double levels=DuraluminFeruchemy.fundedLevels(p);if(levels<=0)return false;double reduction=Math.min(FeruchemicalEnhancement.fractionCap(p,"duralumin",ConnectionConfig.ANIMAL_CAP.get()),levels*ConnectionConfig.ANIMAL_STEP.get());double range=originalRange*(1-reduction);return mob.distanceToSqr(p)>range*range;}
    @SubscribeEvent public void tick(ServerTickEvent.Post e){
        var server=e.getServer();long time=server.overworld().getGameTime();ConnectionTrades.tick();
        if(time%20==0&&DuraluminFeruchemy.configured()&&ConnectionConfig.BONDS.get()){
            DROPS.entrySet().removeIf(entry->entry.getKey().isRemoved()||time>entry.getValue().expires());
            var players=server.getPlayerList().getPlayers();for(var p:players)if(participant(p))activity(p,false);
            int budget=4096;outer:for(var p:players)if(participant(p)&&active(p)){double range=ConnectionConfig.PROXIMITY_RANGE.get();var nearby=p.serverLevel().getEntitiesOfClass(ServerPlayer.class,p.getBoundingBox().inflate(range),q->q!=p&&participant(q));nearby.sort(Comparator.comparing(ServerPlayer::getUUID));for(var q:nearby.subList(0,Math.min(64,nearby.size()))){if(--budget<0)break outer;nearbyPair(p,q);}}
        }
        if(time%10==0)for(var p:server.getPlayerList().getPlayers()){var list=sensed(p);if(!list.isEmpty()){SENSING.add(p.getUUID());PacketDistributor.sendToPlayer(p,new ConnectionPacket(p.level().dimension().location().toString(),list));}else clearSensing(p);}
    }
    @SubscribeEvent public void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent e){ConnectionTrades.clear();}
    @SubscribeEvent public void stopped(ServerStoppedEvent e){ACTIVITY.clear();SENSING.clear();DROPS.clear();ConnectionTrades.clear();DuraluminFeruchemy.clearAll();}
}
