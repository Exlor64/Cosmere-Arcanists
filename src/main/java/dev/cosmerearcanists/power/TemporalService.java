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
    public static double sightStrength(ServerPlayer p,String metal){return TemporalConfig.REQUIRE_HOLD.get() && !PowerService.holdingAbility(p,"a:"+metal)?0:strength(p,metal);}
    /** Dodge only avoidable physical attacks, never falls, hazards, explosions or bypass damage. */
    public static double dodgeChance(ServerPlayer p,net.minecraft.world.damagesource.DamageSource source){
        if(!TemporalConfig.DODGE.get() || !PowerConfig.FLARING.get() || PowerData.state(p,"a:atium")!=3)return 0;
        double power=sightStrength(p,"atium");if(power<=0)return 0;
        if(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR) || source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION))return 0;
        var direct=source.getDirectEntity();var attacker=source.getEntity();
        boolean physical=source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) || direct instanceof LivingEntity && direct==attacker;
        if(!physical || attacker==p || interferes(attacker) || interferes(direct))return 0;
        return TemporalMath.dodgeChance(power,PowerConfig.FLARE_STRENGTH.get(),TemporalConfig.DODGE_CHANCE.get(),TemporalConfig.DODGE_CAP.get());
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST)
    public void dodge(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event){
        if(event.getEntity() instanceof ServerPlayer p && event.getAmount()>0){
            double chance=dodgeChance(p,event.getSource());
            if(chance>0 && p.getRandom().nextDouble()<chance)event.setCanceled(true);
        }
    }
    public static boolean interferes(Entity e) {return TemporalConfig.COUNTERS.get() && e instanceof ServerPlayer p && (sightStrength(p,"atium")>0 || sightStrength(p,"electrum")>0);}
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
            if(sightStrength(p,metal)>0 && PowerConfig.CONSUMPTION.get()) {
                int state=PowerData.state(p,"a:"+metal);if(state==3&&!PowerConfig.FLARING.get()){state=2;PowerData.state(p,"a:"+metal,2);}
                double rate=metal.equals("atium")?TemporalConfig.ATIUM_DRAIN.get():TemporalConfig.ELECTRUM_DRAIN.get();
                EnhancementService.burn(p,metal,rate/20*PowerRules.levelMultiplier(state,PowerConfig.LOW_DRAIN.get(),1,PowerConfig.FLARE_DRAIN.get()));
            }
            if(PowerConfig.CONSUMPTION.get() && PowerData.reserve(p,metal)<=0 && PowerData.state(p,"a:"+metal)>0)PowerData.state(p,"a:"+metal,0);
        }
        boolean active=sightStrength(p,"atium")>0 || sightStrength(p,"electrum")>0;
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
    static List<AABB> collisions(ServerLevel level,AABB swept,Entity entity) {
        var first=BlockPos.containing(swept.minX-1e-6,swept.minY-1e-6,swept.minZ-1e-6);var last=BlockPos.containing(swept.maxX+1e-6,swept.maxY+1e-6,swept.maxZ+1e-6);
        if((long)(last.getX()-first.getX()+1)*(last.getY()-first.getY()+1)*(last.getZ()-first.getZ()+1)>216)return null;
        var boxes=new ArrayList<AABB>();
        for(var pos:BlockPos.betweenClosed(first,last)) {
            if(!level.hasChunkAt(pos))return null;
            for(var box:level.getBlockState(pos).getCollisionShape(level,pos,net.minecraft.world.phys.shapes.CollisionContext.of(entity)).toAabbs())boxes.add(box.move(pos));
        }
        return boxes;
    }
    static double clip(AABB box,AABB obstacle,int axis,double delta) {
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
    private record Route(List<Vec3> points,double speed,boolean complete) { }
    /** Snapshot only an existing route; never create, tick, advance or request a new path. */
    private static Route route(Mob mob,Vec3 motion){
        if(!TemporalConfig.NAVIGATION.get() || mob.isNoAi() || mob.hurtTime>0 || !mob.onGround() || mob.isNoGravity() || mob.isPassenger() || mob.isInWaterOrBubble()
            || !(mob.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation))return null;
        var navigation=mob.getNavigation();var path=navigation.getPath();if(path==null || path.isDone())return null;
        double modifier=((dev.cosmerearcanists.mixin.NavigationAccessor)navigation).cosmerearcanists$speedModifier();
        double planned=mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)*modifier;
        if(!Double.isFinite(planned)||planned<=0 || Math.abs(motion.y)>.12)return null;
        // Preserve large impulses and motion opposite to navigation rather than steering them away.
        if(motion.horizontalDistance()>Math.max(.45,planned*1.8))return null;
        var points=new ArrayList<Vec3>();int index=path.getNextNodeIndex(),end=Math.min(path.getNodeCount(),index+TemporalConfig.PATH_NODES.get());
        for(int j=index;j<end;j++){
            var point=path.getEntityPosAtNode(mob,j);if(!Double.isFinite(point.x)||!Double.isFinite(point.y)||!Double.isFinite(point.z)||point.distanceToSqr(mob.position())>128*128 || !mob.level().hasChunkAt(BlockPos.containing(point)))break;
            points.add(point);
        }
        if(points.isEmpty() || points.getFirst().distanceToSqr(mob.position())>64)return null;
        var first=points.stream().map(p->p.subtract(mob.position()).multiply(1,0,1)).filter(v->v.lengthSqr()>.04).findFirst().orElse(Vec3.ZERO);
        if(first.lengthSqr()>0 && motion.horizontalDistance()>.04 && first.normalize().dot(motion.normalize())<-.25)return null;
        double speed=motion.horizontalDistance()>.02?motion.horizontalDistance():planned;
        return new Route(List.copyOf(points),Math.clamp(speed,.01,1.0),index+points.size()==path.getNodeCount());
    }
    public static List<Frame> predict(Entity entity,int ticks){
        var motion=velocity(entity);var route=entity instanceof Mob mob?route(mob,motion):null;
        return trace(entity,motion,ticks,route);
    }
    public static List<Frame> trace(Entity entity,Vec3 initialVelocity,int ticks) {return trace(entity,initialVelocity,ticks,null);}
    private static List<Frame> trace(Entity entity,Vec3 initialVelocity,int ticks,Route route) {
        if(!(entity.level() instanceof ServerLevel level))return List.of();
        var frames=new ArrayList<Frame>();var position=entity.position();var box=entity.getBoundingBox();var v=initialVelocity;double fallen=entity.fallDistance,g=gravity(entity);
        if(!Double.isFinite(v.x) || !Double.isFinite(v.y) || !Double.isFinite(v.z) || v.lengthSqr()>256 || entity.getBbWidth()>8 || entity.getBbHeight()>8)return List.of(new Frame(position,TemporalPacket.UNCERTAIN));
        int waypoint=0;boolean grounded=entity.onGround();
        for(int tick=0;tick<Math.min(60,ticks);tick++) {
            if(route!=null && grounded){
                while(waypoint<route.points.size()){
                    var delta=route.points.get(waypoint).subtract(position);
                    if(delta.horizontalDistance()<=.08 && Math.abs(delta.y)<=.6)waypoint++;else break;
                }
                if(waypoint<route.points.size()){
                    var delta=route.points.get(waypoint).subtract(position);double distance=delta.horizontalDistance(),step=Math.min(route.speed,distance);
                    var horizontal=distance<1e-8?Vec3.ZERO:new Vec3(delta.x/distance*step,0,delta.z/distance*step);
                    v=new Vec3(horizontal.x,delta.y>.5?.42:Math.min(0,v.y),horizontal.z);
                }else v=new Vec3(0,Math.min(0,v.y),0);
            }
            var obstacles=collisions(level,box.expandTowards(v),entity);if(obstacles==null){frames.add(new Frame(position,TemporalPacket.UNCERTAIN));break;}
            var step=move(box,v,obstacles);boolean landed=v.y<0 && step.y>v.y+1e-6;
            position=position.add(step);box=box.move(step);int flags=hazard(level,box,entity);
            if(route!=null && waypoint>=route.points.size() && !route.complete)flags|=TemporalPacket.UNCERTAIN;
            grounded=landed || grounded && Math.abs(v.y)<1e-8 && Math.abs(step.y)<1e-8;
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
            if(TemporalConfig.SHADOWS.get() || warnings && (flags&TemporalPacket.DANGER_MASK)!=0)samples.add(new TemporalPacket.Point(frames.get(index).position,warnings?flags:flags&TemporalPacket.UNCERTAIN));
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
    private static boolean mayAggro(Mob mob,ServerPlayer observer){
        return TemporalConfig.AGGRO.get() && mob instanceof net.minecraft.world.entity.monster.Enemy && !mob.isNoAi() && mob.getTarget()==null && mob.canAttack(observer) && !observer.getAbilities().invulnerable && !observer.isSpectator()
            && (!(mob instanceof NeutralMob neutral) || neutral.isAngryAt(observer)) && !EmotionService.pacified(mob) && DominationService.owner(mob)==null && !EmotionService.trusts(mob,observer);
    }
    private static boolean futureSightLine(ServerPlayer observer,Vec3 from,Vec3 to){
        var delta=to.subtract(from);int steps=(int)Math.ceil(delta.length()*4);if(steps>256)return false;
        for(int i=0;i<=steps;i++)if(!observer.serverLevel().hasChunkAt(BlockPos.containing(from.add(delta.scale(steps==0?0:(double)i/steps)))))return false;
        return observer.serverLevel().clip(new net.minecraft.world.level.ClipContext(from,to,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,observer)).getType()==HitResult.Type.MISS;
    }
    private static void aggro(ServerPlayer observer,Mob mob,List<Frame> own,List<Frame> other){
        double range=Math.min(64,mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE));
        for(int i=0;i<Math.min(own.size(),other.size());i++){var f=own.get(i);var position=other.get(i).position;
            if(position.distanceToSqr(f.position)<=range*range && futureSightLine(observer,position.add(0,mob.getEyeHeight(),0),f.position.add(0,observer.getEyeHeight(),0))){own.set(i,new Frame(f.position,f.warning|TemporalPacket.AGGRO));break;}
        }
    }
    /** Interference deliberately replaces a useful line with divergent possibilities, not a hidden exact line beneath branches. */
    private static List<Frame> branch(Entity e,Vec3 velocity,int ticks,int sign) {
        double speed=Math.max(.12,Math.min(.4,velocity.horizontalDistance()));double heading=velocity.horizontalDistance()>.01?Math.toDegrees(Math.atan2(-velocity.x,velocity.z)):e.getYRot();double yaw=Math.toRadians(heading+sign*70);
        return trace(e,new Vec3(-Math.sin(yaw)*speed,velocity.y,Math.cos(yaw)*speed),ticks);
    }
    public static TemporalPacket forecast(ServerPlayer observer) {
        String dim=observer.level().dimension().location().toString();double atium=sightStrength(observer,"atium"),electrum=sightStrength(observer,"electrum");
        var shadows=new ArrayList<TemporalPacket.Shadow>();int horizon=horizon(Math.max(atium,electrum));
        if(horizon==0)return new TemporalPacket(dim,1,List.of());
        var self=new ArrayList<>(trace(observer,velocity(observer),horizon));boolean warnings=TemporalConfig.WARNINGS.get();
        double range=Math.min(TemporalConfig.MAX_RANGE.get(),TemporalConfig.RANGE.get()*Math.sqrt(Math.max(1,atium)));
        // Electrum scans threats only to forecast consequences for self, never sends their identity or path.
        var candidates=observer.serverLevel().getEntities(observer,observer.getBoundingBox().inflate(range),e->(e instanceof LivingEntity || TemporalConfig.PROJECTILES.get() && e instanceof Projectile && e.getDeltaMovement().lengthSqr()>1e-6 || atium>0 && e instanceof ItemEntity) && (visible(observer,e) || electrum>0 && e instanceof Mob mob && mayAggro(mob,observer)));
        candidates.sort(Comparator.<Entity>comparingInt(e->e instanceof Projectile?0:hostile(e,observer)?1:2).thenComparingDouble(e->e.distanceToSqr(observer)));
        var aggroSources=new HashMap<Mob,List<Frame>>();
        int targets=0;
        for(var entity:candidates) {
            if(targets++>=TemporalConfig.TARGETS.get())break;
            var frames=predict(entity,horizon(atium>0?atium:electrum));if(frames.isEmpty())continue;
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
            if(warnings && electrum>0 && !split && entity instanceof Mob mob && mayAggro(mob,observer)){aggro(observer,mob,self,frames);aggroSources.put(mob,frames);}
            if(atium<=0 || !visible(observer,entity) || !TemporalConfig.SHADOWS.get() && threat==0)continue;
            int lead=Math.min(4,frames.size());var intent=frames.get(lead-1).position.subtract(entity.position()).scale(1.0/lead);
            boolean alternate=TemporalConfig.MULTI_SHADOWS.get() && PowerService.holdingAlternateAbility(observer,"a:atium");
            if(split) {
                if(TemporalConfig.SHADOWS.get())for(int sign:new int[]{-1,1}) {
                    var possibilities=branch(entity,intent,horizon(atium),sign);
                    if(!possibilities.isEmpty())shadows.add(shadow(entity,TemporalPacket.BRANCH,possibilities,false,TemporalPacket.UNCERTAIN));
                }
                if(alternate && TemporalConfig.SHADOWS.get())shadows.add(shadow(entity,TemporalPacket.BRANCH,frames,false,TemporalPacket.UNCERTAIN));
            } else {
                shadows.add(shadow(entity,entity instanceof Projectile?TemporalPacket.PROJECTILE:TemporalPacket.OTHER,frames,warnings,threat));
                if(alternate && TemporalConfig.SHADOWS.get())for(int sign:new int[]{-1,1}){
                    var possibilities=branch(entity,intent,horizon(atium),sign);
                    if(!possibilities.isEmpty())shadows.add(shadow(entity,TemporalPacket.BRANCH,possibilities,false,TemporalPacket.UNCERTAIN));
                }
            }
        }
        if(electrum>0 && !self.isEmpty() && (TemporalConfig.SHADOWS.get() || warnings)) {
            int n=Math.min(self.size(),horizon(electrum));shadows.add(shadow(observer,TemporalPacket.SELF,self.subList(0,n),warnings,0));
            if(TemporalConfig.SHADOWS.get() && TemporalConfig.JUMP.get() && observer.onGround() && PowerService.holdingAbility(observer,"a:electrum")) {
                var v=velocity(observer);var jump=new ArrayList<>(trace(observer,new Vec3(v.x,.42,v.z),horizon(electrum)));for(var entry:aggroSources.entrySet())aggro(observer,entry.getKey(),jump,entry.getValue());
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
