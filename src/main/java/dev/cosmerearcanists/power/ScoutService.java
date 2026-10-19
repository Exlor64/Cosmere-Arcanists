package dev.cosmerearcanists.power;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** A stationary, vulnerable real body and a collision-constrained private future viewpoint. */
public final class ScoutService {
    private static final class Scout {
        final Vec3 anchor;final String dimension;Vec3 position;double vertical;boolean grounded=true;long lastLook=Long.MIN_VALUE;
        Scout(ServerPlayer p){anchor=position=p.position();dimension=p.level().dimension().location().toString();}
    }
    private static final Map<UUID,Scout> SCOUTS=new HashMap<>();
    private static final Set<UUID> INTERRUPTED=new HashSet<>();
    public static void look(ServerPlayer p,ScoutLook look){var s=SCOUTS.get(p.getUUID());if(s==null || s.lastLook==p.level().getGameTime() || !Float.isFinite(look.yaw()) || !Float.isFinite(look.pitch()))return;s.lastLook=p.level().getGameTime();p.setYRot(net.minecraft.util.Mth.wrapDegrees(look.yaw()));p.setXRot(Math.clamp(look.pitch(),-90,90));}
    public static boolean active(ServerPlayer p){return SCOUTS.containsKey(p.getUUID());}
    public static Vec3 position(ServerPlayer p){var s=SCOUTS.get(p.getUUID());return s==null?p.position():s.position;}
    public static double radius(ServerPlayer p){return Math.min(TemporalConfig.SCOUT_CAP.get(),TemporalConfig.SCOUT_RANGE.get()*Math.sqrt(TemporalService.sightStrength(p,"electrum")/Math.max(.0001,PowerConfig.FLARE_STRENGTH.get())));}
    public static void stop(ServerPlayer p){if(SCOUTS.remove(p.getUUID())!=null)PacketDistributor.sendToPlayer(p,new ScoutPacket(p.level().dimension().location().toString(),false,p.position(),0));}
    public static Vec3 move(ServerPlayer p,Vec3 position,Vec3 motion){
        var box=p.getBoundingBox().move(position.subtract(p.position()));var obstacles=TemporalService.collisions(p.serverLevel(),box.expandTowards(motion),p);if(obstacles==null)return Vec3.ZERO;
        double y=motion.y;for(var b:obstacles)y=TemporalService.clip(box,b,1,y);box=box.move(0,y,0);
        double x=motion.x;for(var b:obstacles)x=TemporalService.clip(box,b,0,x);box=box.move(x,0,0);
        double z=motion.z;for(var b:obstacles)z=TemporalService.clip(box,b,2,z);
        return new Vec3(x,y,z);
    }
    public static void tick(ServerPlayer p){
        boolean alternate=PowerService.holdingAlternateAbility(p,"a:electrum");if(!alternate)INTERRUPTED.remove(p.getUUID());
        boolean allowed=alternate && TemporalConfig.SCOUT.get() && PowerConfig.FLARING.get() && PowerData.state(p,"a:electrum")==3 && TemporalService.sightStrength(p,"electrum")>0 && !p.isPassenger() && !p.isSleeping() && !INTERRUPTED.contains(p.getUUID());
        if(!allowed){stop(p);return;}
        var s=SCOUTS.get(p.getUUID());if(s==null){if(!p.onGround())return;s=new Scout(p);SCOUTS.put(p.getUUID(),s);}
        if(!s.dimension.equals(p.level().dimension().location().toString()) || p.position().distanceToSqr(s.anchor)>4){stop(p);INTERRUPTED.add(p.getUUID());return;}
        // Keep real body anchored; no spectator mode, immunity, or changes to player abilities.
        if(p.position().distanceToSqr(s.anchor)>1e-8)p.connection.teleport(s.anchor.x,s.anchor.y,s.anchor.z,p.getYRot(),p.getXRot());
        p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        int input=PowerService.movementInputs(p,"a:electrum");double forward=((input&4)!=0?1:0)-((input&8)!=0?1:0),left=((input&16)!=0?1:0)-((input&32)!=0?1:0);
        double length=Math.max(1,Math.hypot(forward,left)),yaw=Math.toRadians(p.getYRot()),speed=TemporalConfig.SCOUT_SPEED.get()*((input&128)!=0?.5:1);
        if(s.grounded && (input&64)!=0)s.vertical=.42;else s.vertical=Math.max(-.6,(s.vertical-.08)*.98);
        var desired=new Vec3((-Math.sin(yaw)*forward+Math.cos(yaw)*left)*speed/length,s.vertical,(Math.cos(yaw)*forward+Math.sin(yaw)*left)*speed/length);
        double radius=radius(p);var offset=s.position.add(desired).subtract(s.anchor);if(offset.lengthSqr()>radius*radius)desired=s.anchor.add(offset.normalize().scale(radius)).subtract(s.position);
        var moved=move(p,s.position,desired);s.position=s.position.add(moved);s.grounded=desired.y<0 && moved.y>desired.y+1e-6;if(Math.abs(moved.y-desired.y)>1e-6)s.vertical=0;
        PacketDistributor.sendToPlayer(p,new ScoutPacket(s.dimension,true,s.position,radius));
    }
    private static void interrupt(ServerPlayer p){if(active(p)){stop(p);INTERRUPTED.add(p.getUUID());}}
    @SubscribeEvent public void hurt(LivingIncomingDamageEvent e){if(e.getEntity() instanceof ServerPlayer p && e.getAmount()>0)interrupt(p);}
    @SubscribeEvent public void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)interrupt(p);}
    @SubscribeEvent public void interact(PlayerInteractEvent e){if(e.getEntity() instanceof ServerPlayer p && active(p) && e instanceof net.neoforged.bus.api.ICancellableEvent cancel)cancel.setCanceled(true);}
    @SubscribeEvent public void attack(AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p && active(p))e.setCanceled(true);}
    @SubscribeEvent public void breakBlock(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e){if(e.getPlayer() instanceof ServerPlayer p && active(p))e.setCanceled(true);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){SCOUTS.remove(e.getEntity().getUUID());INTERRUPTED.remove(e.getEntity().getUUID());}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);INTERRUPTED.add(e.getEntity().getUUID());}
    @SubscribeEvent public void stopped(ServerStoppedEvent e){SCOUTS.clear();INTERRUPTED.clear();}
}
