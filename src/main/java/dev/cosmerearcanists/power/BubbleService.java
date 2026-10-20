package dev.cosmerearcanists.power;

import java.util.*;
import dev.cosmerearcanists.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BubbleService {
    private record Key(UUID owner,String metal){}
    public record Bubble(ServerPlayer owner,String metal,String dimension,Vec3 center,double radius,double rate){}
    private static boolean dirty;
    private static final Map<Key,Bubble> BUBBLES=new LinkedHashMap<>();
    private static final Map<Entity,BubbleMath.Clock> CLOCKS=new WeakHashMap<>();
    private static final Map<BlockEntity,BubbleMath.Clock> BLOCK_CLOCKS=new WeakHashMap<>();
    private static final Map<UUID,Double> PLAYER_RATES=new HashMap<>();
    private static final Map<UUID,Vec3> PLAYER_POSITIONS=new HashMap<>();
    private static final ThreadLocal<Double> MOVE_SCALE=ThreadLocal.withInitial(()->1.0),PROJECTILE_SCALE=ThreadLocal.withInitial(()->1.0);
    public static java.util.function.ToDoubleFunction<Player> clientRate=p->1;
    private static final Map<Player,Map<String,BubbleMath.Clock>> PLAYER_CLOCKS=new WeakHashMap<>();
    private static final ResourceLocation ATTRIBUTE=ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID,"bubble_time");
    public static boolean configured(String metal){return BubbleConfig.ENABLED.get()&&PowerConfig.ENABLED.get()&&ServerConfig.ALLOMANCY.enabled().get()&&(metal.equals("bendalloy")?BubbleConfig.BENDALLOY.get():metal.equals("cadmium")&&BubbleConfig.CADMIUM.get())&&ServerConfig.ALLOMANCY.metals().get(Metal.valueOf(metal.toUpperCase(Locale.ROOT))).get();}
    public static double strength(ServerPlayer p,String metal){if(!configured(metal)||!p.isAlive()||p.isSpectator()||PowerConfig.CONSUMPTION.get()&&PowerData.reserve(p,metal)<=0)return 0;int state=PowerData.state(p,"a:"+metal);if(state==3&&!PowerConfig.FLARING.get())state=2;return PowerAttributes.effectiveStrength(p,"a:"+metal)*PowerRules.levelMultiplier(state,PowerConfig.LOW_STRENGTH.get(),PowerConfig.BURN_STRENGTH.get(),PowerConfig.FLARE_STRENGTH.get());}
    public static List<Bubble> bubbles(ServerLevel level){if(BUBBLES.entrySet().removeIf(e->!valid(e.getValue())))dirty=true;return BUBBLES.values().stream().filter(b->b.dimension.equals(level.dimension().location().toString())).toList();}
    private static boolean valid(Bubble b){return b.owner.isAlive()&&!b.owner.isRemoved()&&!b.owner.isSpectator()&&b.dimension.equals(b.owner.level().dimension().location().toString())&&strength(b.owner,b.metal)>0&&PowerService.holdingAbility(b.owner,"a:"+b.metal)&&center(b.owner).distanceToSqr(b.center)<=b.radius*b.radius;}
    private static Vec3 center(Entity e){return e.position().add(0,e.getBbHeight()*.5,0);}
    public static boolean create(ServerPlayer p,String metal){if(strength(p,metal)<=0||!PowerService.holdingAbility(p,"a:"+metal)||ScoutService.active(p))return false;var key=new Key(p.getUUID(),metal);if(BUBBLES.containsKey(key)){if(valid(BUBBLES.get(key)))return true;BUBBLES.remove(key);}if(bubbles(p.serverLevel()).size()>=BubbleConfig.MAX_BUBBLES.get())return false;double s=strength(p,metal);BUBBLES.put(key,new Bubble(p,metal,p.level().dimension().location().toString(),center(p),BubbleMath.radius(s,BubbleConfig.BASE_RADIUS.get(),BubbleConfig.MAX_RADIUS.get()),BubbleMath.rate(metal,s,BubbleConfig.MIN_RATE.get(),Math.min(BubbleConfig.MAX_RATE.get(),BubbleConfig.MAX_STEPS.get()))));sync(p.server);return true;}
    public static void activate(ServerPlayer p,String key,AbilityDispatcher.Phase phase){String metal=key.substring(2);if(phase==AbilityDispatcher.Phase.STOP){remove(p,metal);return;}if(phase==AbilityDispatcher.Phase.START)create(p,metal);}
    public static void remove(ServerPlayer p,String metal){if(BUBBLES.remove(new Key(p.getUUID(),metal))!=null)sync(p.server);}
    private static boolean immune(Bubble b,Entity e){return e.getRootVehicle()==b.owner.getRootVehicle();}
    private static boolean allowed(Bubble b,Entity e){if(immune(b,e)||e.isSpectator())return false;if(e instanceof Player p)return BubbleConfig.PLAYERS.get()&&!p.getAbilities().invulnerable&&(!BubbleConfig.PVP.get()||b.owner.server.isPvpAllowed()&&b.owner.canHarmPlayer(p));return BubbleConfig.ENTITIES.get();}
    private static double contribution(Bubble b,Vec3 position){double distance=position.distanceTo(b.center);if(distance>=b.radius)return 1;double weight=BubbleConfig.EDGE_WIDTH.get()<=0?1:Math.clamp((b.radius-distance)/BubbleConfig.EDGE_WIDTH.get(),0,1);weight=weight*weight*(3-2*weight);return 1+(b.rate-1)*weight;}
    public static double rate(Entity e){if(!(e.level() instanceof ServerLevel level)||!BubbleConfig.ENABLED.get()||!e.isAlive())return 1;double rate=1;for(var b:bubbles(level))if(allowed(b,e))rate*=contribution(b,center(e));return Math.clamp(rate,BubbleConfig.MIN_RATE.get(),Math.min(BubbleConfig.MAX_RATE.get(),BubbleConfig.MAX_STEPS.get()));}
    public static double blockRate(BlockEntity e){if(!BubbleConfig.BLOCK_ENTITIES.get()||!(e.getLevel() instanceof ServerLevel level))return 1;double rate=1;for(var b:bubbles(level))rate*=contribution(b,Vec3.atCenterOf(e.getBlockPos()));return Math.clamp(rate,BubbleConfig.MIN_RATE.get(),Math.min(BubbleConfig.MAX_RATE.get(),BubbleConfig.MAX_STEPS.get()));}
    public static void blockTick(BlockEntity e,Runnable original){double rate=blockRate(e);if(Math.abs(rate-1)<1e-5){BLOCK_CLOCKS.remove(e);original.run();return;}int ticks=BLOCK_CLOCKS.computeIfAbsent(e,k->new BubbleMath.Clock()).advance(rate,BubbleConfig.MAX_STEPS.get());for(int i=0;i<ticks&&!e.isRemoved();i++)original.run();}
    public static double movementScale(){return MOVE_SCALE.get();}
    public static Vec3 intrinsicVelocity(Entity e){return e.getDeltaMovement().scale(1/PROJECTILE_SCALE.get());}
    /** Full logic follows a fractional clock; skipped logic frames still advance collision-aware motion. */
    public static void tickEntity(Entity e,Runnable original){
        if(e instanceof Player||e.isRemoved()){original.run();return;}double rate=rate(e);if(Math.abs(rate-1)<1e-5){CLOCKS.remove(e);Vec3 before=center(e);original.run();crossed(e,before,center(e));return;}
        var clock=CLOCKS.computeIfAbsent(e,k->new BubbleMath.Clock());int ticks=clock.advance(rate,BubbleConfig.MAX_STEPS.get());double step=Math.min(1,rate);
        if(ticks==0){e.setOldPosAndRot();Vec3 before=center(e);if(e instanceof Projectile projectile)driftProjectile(projectile,step);else e.move(MoverType.SELF,e.getDeltaMovement().scale(step));crossed(e,before,center(e));return;}
        for(int i=0;i<ticks&&!e.isRemoved();i++){Vec3 before=center(e);double previous=MOVE_SCALE.get(),projectilePrevious=PROJECTILE_SCALE.get();Vec3 velocity=e.getDeltaMovement();boolean projectile=e instanceof Projectile&&step<1;double gravity=e.getGravity();
            try{MOVE_SCALE.set(projectile?1:step);if(projectile){PROJECTILE_SCALE.set(step);e.setDeltaMovement(velocity.scale(step));}original.run();}
            finally{if(projectile&&!e.isRemoved()){Vec3 result=e.getDeltaMovement();if(result.lengthSqr()>1e-12)e.setDeltaMovement(result.scale(1/step).add(0,gravity*(1/step-1),0));}MOVE_SCALE.set(previous);PROJECTILE_SCALE.set(projectilePrevious);}crossed(e,before,center(e));if(rate>1&&rate(e)==1)break;}
    }
    private static void driftProjectile(Projectile e,double step){Vec3 velocity=e.getDeltaMovement(),before=e.position();e.setDeltaMovement(velocity.scale(step));double previous=PROJECTILE_SCALE.get();PROJECTILE_SCALE.set(step);try{
        var access=(dev.cosmerearcanists.mixin.BubbleProjectileAccess)e;var hit=ProjectileUtil.getHitResultOnMoveVector(e,access::cosmerearcanists$canHit);
        if(hit.getType()!=HitResult.Type.MISS){if(!net.neoforged.neoforge.event.EventHooks.onProjectileImpact(e,hit))access.cosmerearcanists$onHit(hit);}else e.setPos(before.add(velocity.scale(step)));
    }finally{if(!e.isRemoved()){var changed=e.getDeltaMovement();e.setDeltaMovement(changed.equals(velocity.scale(step))?velocity:changed.scale(1/step));}PROJECTILE_SCALE.set(previous);}}
    public static Vec3 deflect(Vec3 velocity,net.minecraft.util.RandomSource random){double speed=velocity.length();if(speed<1e-5)return velocity;double max=Math.toRadians(BubbleConfig.DEFLECTION_DEGREES.get()),angle=Math.acos(1-random.nextDouble()*(1-Math.cos(max))),azimuth=random.nextDouble()*Math.PI*2;var direction=velocity.scale(1/speed);var side=direction.cross(Math.abs(direction.y)<.95?new Vec3(0,1,0):new Vec3(1,0,0)).normalize();var up=side.cross(direction).normalize();return direction.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)*Math.cos(azimuth))).add(up.scale(Math.sin(angle)*Math.sin(azimuth))).scale(speed);}
    public static void crossed(Entity e,Vec3 from,Vec3 to){if(!BubbleConfig.DEFLECTION.get()||!(e.level() instanceof ServerLevel level)||e.isRemoved())return;for(var b:bubbles(level)){if(!allowed(b,e))continue;var a=from.subtract(b.center);var c=to.subtract(b.center);int crossings=BubbleMath.crossings(a.x,a.y,a.z,c.x,c.y,c.z,b.radius);for(int i=0;i<crossings;i++){Vec3 velocity=e.getDeltaMovement();if(e instanceof ServerPlayer p){Vec3 travel=to.subtract(from);var impulse=deflect(travel,level.getRandom()).subtract(travel);p.setDeltaMovement(velocity.add(impulse));PacketDistributor.sendToPlayer(p,new PowerNetwork.Impulse(b.dimension,impulse.x,impulse.y,impulse.z,PowerConfig.MAX_SPEED.get(),false));}else{e.setDeltaMovement(deflect(velocity,level.getRandom()));e.hasImpulse=true;}}}}
    private static void attribute(ServerPlayer p,Holder<Attribute> key,double rate){var a=p.getAttribute(key);if(a==null)return;var modifier=a.getModifier(ATTRIBUTE);if(Math.abs(rate-1)<1e-4){if(modifier!=null)a.removeModifier(ATTRIBUTE);return;}double amount=Math.round((rate-1)*10000)/10000.0;if(modifier==null||modifier.amount()!=amount){a.removeModifier(ATTRIBUTE);a.addTransientModifier(new AttributeModifier(ATTRIBUTE,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}}
    public static double playerRate(Player p){return p.level().isClientSide?clientRate.applyAsDouble(p):PLAYER_RATES.getOrDefault(p.getUUID(),1.0);}
    public static int timerTicks(Player p,String timer){double rate=BubbleConfig.PLAYER_VITALS.get()?playerRate(p):1;var clocks=PLAYER_CLOCKS.computeIfAbsent(p,k->new HashMap<>());if(Math.abs(rate-1)<1e-4){clocks.remove(timer);return 1;}return clocks.computeIfAbsent(timer,k->new BubbleMath.Clock()).advance(rate,BubbleConfig.MAX_STEPS.get());}
    public static void playerTimer(Player p,String timer,Runnable original){int ticks=timerTicks(p,timer);for(int i=0;i<ticks;i++)original.run();}
    @SubscribeEvent public void itemUse(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Tick event){if(event.getEntity() instanceof Player p){int ticks=timerTicks(p,"itemUse");event.setDuration(Math.max(1,event.getDuration()+1-ticks));}}
    public static void tickPlayer(ServerPlayer p){for(String metal:List.of("bendalloy","cadmium"))if(strength(p,metal)>0&&PowerConfig.CONSUMPTION.get()){int state=PowerData.state(p,"a:"+metal);if(state==3&&!PowerConfig.FLARING.get())state=2;EnhancementService.consume(p,metal,BubbleConfig.DRAIN.get()/20*PowerRules.levelMultiplier(state,PowerConfig.LOW_DRAIN.get(),1,PowerConfig.FLARE_DRAIN.get()));if(PowerData.reserve(p,metal)<=0)PowerData.state(p,"a:"+metal,0);}}
    @SubscribeEvent public void tick(ServerTickEvent.Post event){var server=event.getServer();boolean removed=BUBBLES.entrySet().removeIf(e->!valid(e.getValue()));BUBBLES.replaceAll((k,b)->{double s=strength(b.owner,b.metal);return new Bubble(b.owner,b.metal,b.dimension,b.center,BubbleMath.blend(b.radius,BubbleMath.radius(s,BubbleConfig.BASE_RADIUS.get(),BubbleConfig.MAX_RADIUS.get()),BubbleConfig.TRANSITION.get()),BubbleMath.rate(b.metal,s,BubbleConfig.MIN_RATE.get(),Math.min(BubbleConfig.MAX_RATE.get(),BubbleConfig.MAX_STEPS.get())));});for(var p:server.getPlayerList().getPlayers()){updatePlayer(p);}if(removed||dirty||server.getTickCount()%BubbleConfig.UPDATE.get()==0){sync(server);dirty=false;}}
    public static void updatePlayer(ServerPlayer p){double target=rate(p),previous=PLAYER_RATES.getOrDefault(p.getUUID(),1.0),rate=BubbleMath.blend(previous,target,BubbleConfig.TRANSITION.get());if(Math.abs(rate-1)<1e-4)PLAYER_RATES.remove(p.getUUID());else PLAYER_RATES.put(p.getUUID(),rate);attribute(p,Attributes.ATTACK_SPEED,rate);attribute(p,Attributes.GRAVITY,rate);attribute(p,Attributes.BLOCK_BREAK_SPEED,rate);var before=PLAYER_POSITIONS.put(p.getUUID(),center(p));if(before!=null)crossed(p,before,center(p));}
    public static void sync(net.minecraft.server.MinecraftServer server){for(var p:server.getPlayerList().getPlayers()){var list=bubbles(p.serverLevel()).stream().filter(b->b.center.distanceToSqr(p.position())<Math.pow(b.radius+96,2)).limit(128).map(b->new BubblePacket.Sphere(b.owner.getUUID(),b.metal,b.center,b.radius,b.rate)).toList();PacketDistributor.sendToPlayer(p,new BubblePacket(p.level().dimension().location().toString(),playerRate(p),list));}}
    public static void clearPlayer(ServerPlayer p){BUBBLES.keySet().removeIf(k->k.owner.equals(p.getUUID()));PLAYER_RATES.remove(p.getUUID());PLAYER_POSITIONS.remove(p.getUUID());PLAYER_CLOCKS.remove(p);for(var key:List.of(Attributes.ATTACK_SPEED,Attributes.GRAVITY,Attributes.BLOCK_BREAK_SPEED))attribute(p,key,1);sync(p.server);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)clearPlayer(p);}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)clearPlayer(p);}
    @SubscribeEvent public void clone(PlayerEvent.Clone e){if(e.getOriginal() instanceof ServerPlayer p)clearPlayer(p);}
    @SubscribeEvent public void stopped(ServerStoppedEvent e){BUBBLES.clear();CLOCKS.clear();BLOCK_CLOCKS.clear();PLAYER_RATES.clear();PLAYER_POSITIONS.clear();PLAYER_CLOCKS.clear();}
}
