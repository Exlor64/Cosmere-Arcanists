package dev.cosmerearcanists.feruchemy;

import dev.cosmerearcanists.power.*;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ZincReactions {
    private static final Set<ServerPlayer> SENT=Collections.newSetFromMap(new WeakHashMap<>());
    public static void clear(ServerPlayer p){if(SENT.remove(p))PacketDistributor.sendToPlayer(p,new ZincReactionPacket(p.level().dimension().location().toString(),1,List.of()));}
    public static List<ZincReactionPacket.Threat> threats(ServerPlayer p,double level){
        if(!FeruchemyConfig.ZINC_REACTIONS.get()||level+1e-9<FeruchemyConfig.ZINC_CUE_LEVEL.get())return List.of();
        double range=FeruchemyConfig.ZINC_RANGE.get();int horizon=FeruchemyConfig.ZINC_HORIZON.get();boolean paths=FeruchemyConfig.ZINC_PATHS.get()&&level+1e-9>=FeruchemyConfig.ZINC_PATH_LEVEL.get();
        var result=new ArrayList<ZincReactionPacket.Threat>();var entities=p.serverLevel().getEntities(p,p.getBoundingBox().inflate(range),e->e instanceof Projectile||e instanceof LivingEntity);entities.sort(Comparator.comparingDouble(e->e.distanceToSqr(p)));int scanned=0;
        for(var entity:entities){
            if(++scanned>64||result.size()>=8)break;if(entity.distanceToSqr(p)>range*range||!BronzeAlertness.perceptible(p,entity))continue;
            var path=new ArrayList<Vec3>();boolean danger;
            if(entity instanceof Projectile projectile){
                if(projectile.getOwner()==p||projectile.getOwner()!=null&&projectile.getOwner().isAlliedTo(p))continue;var velocity=projectile.getDeltaMovement();var relative=projectile.position().subtract(p.getBoundingBox().getCenter());if(velocity.lengthSqr()<.0025||relative.dot(velocity.subtract(p.getDeltaMovement()))>=0)continue;
                var frames=TemporalService.trace(projectile,velocity,horizon);var previous=projectile.position();if(paths)path.add(previous);danger=false;int tick=0;
                for(var frame:frames){if((frame.warning()&TemporalPacket.UNCERTAIN)!=0)break;tick++;var box=p.getBoundingBox().move(p.getDeltaMovement().scale(tick)).inflate(.35+projectile.getBbWidth()*.5);if(box.contains(previous)||box.clip(previous,frame.position()).isPresent())danger=true;if(paths)path.add(frame.position());previous=frame.position();if(danger)break;}
            }else danger=BronzeAlertness.attack(p,entity);
            if(!danger)continue;var direction=entity.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize();if(direction.lengthSqr()<.9)continue;result.add(new ZincReactionPacket.Threat(direction,path));
        }return List.copyOf(result);
    }
    public static void tick(ServerPlayer p,double level){if(level<=0||!FeruchemyConfig.ZINC_REACTIONS.get()||level+1e-9<FeruchemyConfig.ZINC_CUE_LEVEL.get()){clear(p);return;}if(SENT.contains(p)&&p.tickCount%4!=0)return;SENT.add(p);PacketDistributor.sendToPlayer(p,new ZincReactionPacket(p.level().dimension().location().toString(),6,threats(p,level)));}
    private ZincReactions(){}
}
