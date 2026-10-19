package dev.cosmerearcanists.power;

import java.util.*;
import dev.cosmerearcanists.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TemporalService {
    private record Motion(Vec3 position,Vec3 velocity,long tick,String dimension) { }
    private static final Map<UUID,Motion> MOTION=new HashMap<>();
    private static final Set<UUID> SENT=new HashSet<>();
    /** One entry per future tick, at most 60 ticks, no entities or blocks are mutated. */
    public record Frame(Vec3 position,int warning) { }
    public static double strength(ServerPlayer p,String metal) {
        if(!TemporalConfig.ENABLED.get() || !PowerConfig.ENABLED.get() || !ServerConfig.ALLOMANCY.enabled().get() || !p.isAlive() || p.isSpectator())return 0;
        if(metal.equals("atium")?!TemporalConfig.ATIUM.get():!metal.equals("electrum") || !TemporalConfig.ELECTRUM.get() || !ServerConfig.ALLOMANCY.metals().get(Metal.ELECTRUM).get())return 0;
        if(PowerConfig.CONSUMPTION.get() && PowerData.reserve(p,metal)<=0)return 0;
        int state=PowerData.state(p,"a:"+metal);if(state==3&&!PowerConfig.FLARING.get())state=2;
        return PowerAttributes.effectiveStrength(p,"a:"+metal)*PowerRules.levelMultiplier(state,PowerConfig.LOW_STRENGTH.get(),PowerConfig.BURN_STRENGTH.get(),PowerConfig.FLARE_STRENGTH.get());
    }
    public static boolean interferes(Entity e) {return TemporalConfig.COUNTERS.get() && e instanceof ServerPlayer p && (strength(p,"atium")>0 || strength(p,"electrum")>0);}
    public static int horizon(double strength){return TemporalMath.horizon(strength,TemporalConfig.BASE_HORIZON.get(),TemporalConfig.MAX_HORIZON.get());}
    private static Vec3 velocity(Entity e) {
        if(e instanceof ServerPlayer) {
            var m=MOTION.get(e.getUUID());
            if(m!=null && m.dimension.equals(e.level().dimension().location().toString()) && e.level().getGameTime()-m.tick<=2)return m.velocity;
        }
        return e.getDeltaMovement();
    }
    public static void tick(ServerPlayer p) {
        long time=p.level().getGameTime();String dim=p.level().dimension().location().toString();var previous=MOTION.get(p.getUUID());var v=p.getDeltaMovement();
        if(previous!=null && dim.equals(previous.dimension) && time==previous.tick+1) {
            var measured=p.position().subtract(previous.position);
            if(measured.lengthSqr()<64)v=measured;
        }
        MOTION.put(p.getUUID(),new Motion(p.position(),v,time,dim));
        for(String metal:List.of("atium","electrum")) {
            if(strength(p,metal)>0 && PowerConfig.CONSUMPTION.get()) {
                int state=PowerData.state(p,"a:"+metal);if(state==3&&!PowerConfig.FLARING.get()){state=2;PowerData.state(p,"a:"+metal,2);}
                double rate=metal.equals("atium")?TemporalConfig.ATIUM_DRAIN.get():TemporalConfig.ELECTRUM_DRAIN.get();
                EnhancementService.consume(p,metal,rate/20*PowerRules.levelMultiplier(state,PowerConfig.LOW_DRAIN.get(),1,PowerConfig.FLARE_DRAIN.get()));
            }
            if(PowerConfig.CONSUMPTION.get() && PowerData.reserve(p,metal)<=0 && PowerData.state(p,"a:"+metal)>0)PowerData.state(p,"a:"+metal,0);
        }
        boolean active=strength(p,"atium")>0 || strength(p,"electrum")>0;
        if(!active) {if(SENT.remove(p.getUUID()))PacketDistributor.sendToPlayer(p,new TemporalPacket(dim,1,List.of()));return;}
        if(time%TemporalConfig.INTERVAL.get()==0 || !SENT.contains(p.getUUID())) {
            PacketDistributor.sendToPlayer(p,forecast(p));SENT.add(p.getUUID());
        }
    }
    private static double gravity(Entity e) {
        if(e.isNoGravity() || e instanceof net.minecraft.world.entity.player.Player p && (p.getAbilities().flying || p.isFallFlying()))return 0;
        if(e instanceof LivingEntity living && living.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING) && e.getDeltaMovement().y<=0)return .01;
        return e instanceof AbstractArrow?.05:e instanceof ThrowableProjectile?.03:e instanceof Projectile?0:e instanceof ItemEntity?.04:.08;
    }
    private static List<AABB> collisions(ServerLevel level,AABB swept,Entity entity) {
        var first=BlockPos.containing(swept.minX-1e-6,swept.minY-1e-6,swept.minZ-1e-6);var last=BlockPos.containing(swept.maxX+1e-6,swept.maxY+1e-6,swept.maxZ+1e-6);
        if((long)(last.getX()-first.getX()+1)*(last.getY()-first.getY()+1)*(last.getZ()-first.getZ()+1)>216)return null;
        var boxes=new ArrayList<AABB>();
        for(var pos:BlockPos.betweenClosed(first,last)) {
            if(!level.hasChunkAt(pos))return null;
            for(var box:level.getBlockState(pos).getCollisionShape(level,pos,net.minecraft.world.phys.shapes.CollisionContext.of(entity)).toAabbs())boxes.add(box.move(pos));
        }
        return boxes;
    }
    private static double clip(AABB box,AABB obstacle,int axis,double delta) {
        double bmin=axis==0?box.minX:axis==1?box.minY:box.minZ,bmax=axis==0?box.maxX:axis==1?box.maxY:box.maxZ;
        double omin=axis==0?obstacle.minX:axis==1?obstacle.minY:obstacle.minZ,omax=axis==0?obstacle.maxX:axis==1?obstacle.maxY:obstacle.maxZ;
        boolean overlap=axis==0?box.maxY>obstacle.minY && box.minY<obstacle.maxY && box.maxZ>obstacle.minZ && box.minZ<obstacle.maxZ:
            axis==1?box.maxX>obstacle.minX && box.minX<obstacle.maxX && box.maxZ>obstacle.minZ && box.minZ<obstacle.maxZ:
            box.maxX>obstacle.minX && box.minX<obstacle.maxX && box.maxY>obstacle.minY && box.minY<obstacle.maxY;
        if(!overlap)return delta;
        if(delta>0 && bmax<=omin+1e-7)return Math.min(delta,Math.max(0,omin-bmax));
        if(delta<0 && bmin>=omax-1e-7)return Math.max(delta,Math.min(0,omax-bmin));return delta;
    }
    private static Vec3 move(AABB box,Vec3 motion,List<AABB> obstacles) {
        double y=motion.y,x=motion.x,z=motion.z;for(var b:obstacles)y=clip(box,b,1,y);box=box.move(0,y,0);
        for(var b:obstacles)x=clip(box,b,0,x);box=box.move(x,0,0);for(var b:obstacles)z=clip(box,b,2,z);return new Vec3(x,y,z);
    }
    private static int hazard(ServerLevel level,AABB box,Entity entity) {
        for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY-.05,box.minZ),BlockPos.containing(box.maxX-1e-6,box.maxY-1e-6,box.maxZ-1e-6))) {
            if(!level.hasChunkAt(pos))return TemporalPacket.UNCERTAIN;
            var state=level.getBlockState(pos);
            boolean fireProtected=entity.fireImmune() || entity instanceof LivingEntity l && l.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE);
            if((!fireProtected && (state.getFluidState().is(FluidTags.LAVA)||state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE)||state.is(Blocks.MAGMA_BLOCK)))
                || state.is(Blocks.CACTUS)||state.is(Blocks.SWEET_BERRY_BUSH)||state.is(Blocks.WITHER_ROSE)||state.is(Blocks.POWDER_SNOW))return TemporalPacket.HAZARD;
        }return 0;
    }
    public static List<Frame> trace(Entity entity,Vec3 initialVelocity,int ticks) {
        if(!(entity.level() instanceof ServerLevel level))return List.of();
        var frames=new ArrayList<Frame>();var position=entity.position();var box=entity.getBoundingBox();var v=initialVelocity;double fallen=entity.fallDistance,g=gravity(entity);
        if(!Double.isFinite(v.x) || !Double.isFinite(v.y) || !Double.isFinite(v.z) || v.lengthSqr()>256 || entity.getBbWidth()>8 || entity.getBbHeight()>8)return List.of(new Frame(position,TemporalPacket.UNCERTAIN));
        for(int tick=0;tick<Math.min(60,ticks);tick++) {
            var obstacles=collisions(level,box.expandTowards(v),entity);if(obstacles==null){frames.add(new Frame(position,TemporalPacket.UNCERTAIN));break;}
            var step=move(box,v,obstacles);boolean landed=v.y<0 && step.y>v.y+1e-6;
            position=position.add(step);box=box.move(step);int flags=hazard(level,box,entity);
            boolean water=level.getFluidState(BlockPos.containing(position)).is(FluidTags.WATER);
            if(water)fallen=0;else if(step.y<0)fallen-=step.y;
            if(landed) {
                if(fallen>3 && g>=.05 && !(entity instanceof LivingEntity l && l.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)))flags|=TemporalPacket.FALL;
                fallen=0;
            }
            if(position.y<level.getMinBuildHeight()-32)flags|=TemporalPacket.HAZARD;
            frames.add(new Frame(position,flags));
            if(entity instanceof Projectile && step.subtract(v).lengthSqr()>1e-8)break;
            double drag=entity instanceof AbstractArrow || entity instanceof ThrowableProjectile?.99:entity instanceof ItemEntity?.98:1;
            v=new Vec3(Math.abs(step.x-v.x)>1e-6?0:v.x*drag,(Math.abs(step.y-v.y)>1e-6?0:v.y)*drag-g,Math.abs(step.z-v.z)>1e-6?0:v.z*drag);
        }return List.copyOf(frames);
    }
    private static TemporalPacket.Shadow shadow(Entity e,int kind,List<Frame> frames,boolean warnings,int extra) {
        var samples=new ArrayList<TemporalPacket.Point>();int count=Math.min(TemporalConfig.SAMPLES.get(),frames.size());int previous=0;
        for(int i=0;i<count;i++) {
            int index=(i+1)*frames.size()/count-1,flags=extra;
            for(int j=previous;j<=index;j++)flags|=frames.get(j).warning;previous=index+1;
            if(TemporalConfig.SHADOWS.get() || warnings && (flags&15)!=0)samples.add(new TemporalPacket.Point(frames.get(index).position,warnings?flags:flags&TemporalPacket.UNCERTAIN));
        }
        return new TemporalPacket.Shadow(e.getId(),kind,e.getBbWidth(),e.getBbHeight(),List.copyOf(samples));
    }
    public static boolean visible(ServerPlayer observer,Entity e) {
        if(e.isRemoved() || e.isSpectator() || e.isInvisibleTo(observer) || e.getBbWidth()>8 || e.getBbHeight()>8)return false;
        // Vanilla ray tracing may read chunks; prove every traversed voxel is loaded first.
        var from=observer.getEyePosition();var delta=e.getEyePosition().subtract(from);int steps=(int)Math.ceil(delta.length()*4);
        if(steps>512)return false;
        for(int i=0;i<=steps;i++)if(!observer.serverLevel().hasChunkAt(BlockPos.containing(from.add(delta.scale(steps==0?0:(double)i/steps)))))return false;
        return observer.hasLineOfSight(e);
    }
    private static boolean hostile(Entity entity,ServerPlayer observer) {
        if(!TemporalConfig.ATTACKS.get())return false;
        return entity instanceof Mob mob && mob.getTarget()==observer && mob.isAggressive() || entity instanceof Creeper creeper && creeper.getSwellDir()>0;
    }
    /** Interference deliberately replaces a useful line with divergent possibilities, not a hidden exact line beneath branches. */
    private static List<Frame> branch(Entity e,Vec3 velocity,int ticks,int sign) {
        double speed=Math.max(.12,Math.min(.4,velocity.horizontalDistance()));double yaw=Math.toRadians(e.getYRot()+sign*70);
        return trace(e,new Vec3(-Math.sin(yaw)*speed,velocity.y,Math.cos(yaw)*speed),ticks);
    }
    public static TemporalPacket forecast(ServerPlayer observer) {
        String dim=observer.level().dimension().location().toString();double atium=strength(observer,"atium"),electrum=strength(observer,"electrum");
        var shadows=new ArrayList<TemporalPacket.Shadow>();int horizon=horizon(Math.max(atium,electrum));
        if(horizon==0)return new TemporalPacket(dim,1,List.of());
        var self=new ArrayList<>(trace(observer,velocity(observer),horizon));boolean warnings=TemporalConfig.WARNINGS.get();
        double range=Math.min(TemporalConfig.MAX_RANGE.get(),TemporalConfig.RANGE.get()*Math.sqrt(Math.max(1,atium)));
        // Electrum scans threats only to forecast consequences for self, never sends their identity or path.
        var candidates=observer.serverLevel().getEntities(observer,observer.getBoundingBox().inflate(range),e->(e instanceof LivingEntity || TemporalConfig.PROJECTILES.get() && e instanceof Projectile && e.getDeltaMovement().lengthSqr()>1e-6 || atium>0 && e instanceof ItemEntity) && visible(observer,e));
        candidates.sort(Comparator.<Entity>comparingInt(e->e instanceof Projectile?0:hostile(e,observer)?1:2).thenComparingDouble(e->e.distanceToSqr(observer)));
        int targets=0;
        for(var entity:candidates) {
            if(targets++>=TemporalConfig.TARGETS.get())break;
            var frames=trace(entity,velocity(entity),horizon(atium>0?atium:electrum));if(frames.isEmpty())continue;
            boolean split=interferes(entity);int threat=0;
            if(warnings && !split && entity instanceof Projectile projectile && projectile.getOwner()!=observer) {
                for(int i=0;i<Math.min(frames.size(),self.size());i++) {
                    Vec3 from=i==0?entity.position():frames.get(i-1).position,to=frames.get(i).position;
                    var own=self.get(i).position;var prev=i==0?observer.position():self.get(i-1).position;
                    var step=to.subtract(from).subtract(own.subtract(prev));
                    double radius=observer.getBbWidth()*.5+entity.getBbWidth()*.5+.2;
                    // Tall player body is approximated by several spheres along its height.
                    boolean hit=false;for(double y=0;y<=observer.getBbHeight();y+=.4) {
                        var r=from.subtract(prev.add(0,y,0));if(TemporalMath.closestDistanceSquared(r.x,r.y,r.z,step.x,step.y,step.z)<=radius*radius){hit=true;break;}
                    }
                    if(hit){var frame=self.get(i);self.set(i,new Frame(frame.position,frame.warning|TemporalPacket.IMPACT));threat=TemporalPacket.IMPACT;break;}
                }
            }
            if(warnings && !split && hostile(entity,observer)) {
                for(int i=0;i<Math.min(frames.size(),self.size());i++)if(frames.get(i).position.distanceToSqr(self.get(i).position)<(entity instanceof Creeper?36:9)) {
                    var f=self.get(i);self.set(i,new Frame(f.position,f.warning|TemporalPacket.ATTACK));threat|=TemporalPacket.ATTACK;break;
                }
            }
            if(atium<=0 || !TemporalConfig.SHADOWS.get() && threat==0)continue;
            if(split) {
                if(TemporalConfig.SHADOWS.get())for(int sign:new int[]{-1,1}) {
                    var possibilities=branch(entity,velocity(entity),horizon(atium),sign);
                    if(!possibilities.isEmpty())shadows.add(shadow(entity,TemporalPacket.BRANCH,possibilities,false,TemporalPacket.UNCERTAIN));
                }
            } else shadows.add(shadow(entity,entity instanceof Projectile?TemporalPacket.PROJECTILE:TemporalPacket.OTHER,frames,warnings,threat));
        }
        if(electrum>0 && !self.isEmpty() && (TemporalConfig.SHADOWS.get() || warnings)) {
            int n=Math.min(self.size(),horizon(electrum));shadows.add(shadow(observer,TemporalPacket.SELF,self.subList(0,n),warnings,0));
            if(TemporalConfig.SHADOWS.get() && TemporalConfig.JUMP.get() && observer.onGround() && PowerService.holdingAbility(observer,"a:electrum")) {
                var v=velocity(observer);var jump=trace(observer,new Vec3(v.x,.42,v.z),horizon(electrum));
                if(!jump.isEmpty())shadows.add(shadow(observer,TemporalPacket.JUMP,jump,warnings,TemporalPacket.UNCERTAIN));
            }
        }
        shadows.removeIf(s->s.points().isEmpty());
        return new TemporalPacket(dim,TemporalConfig.INTERVAL.get()+3,List.copyOf(shadows));
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){MOTION.remove(e.getEntity().getUUID());SENT.remove(e.getEntity().getUUID());}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){MOTION.remove(e.getEntity().getUUID());SENT.remove(e.getEntity().getUUID());}
    @SubscribeEvent public void stopped(ServerStoppedEvent e){MOTION.clear();SENT.clear();}
}
