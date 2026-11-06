package dev.cosmerearcanists.aondor;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

/** Entity outlines and permanent light placement; no entity light-source simulation or chunk tickets. */
public final class AonLightEffects {
    private static final String UNTIL="cosmerearcanists_aon_glow_until";
    private static final Map<Entity,Long> TRACKED=new WeakHashMap<>();
    public static String permanent(ServerPlayer p,Vec3 point,double strength){return planPermanent(p,point,strength).get().message();}
    static Supplier<AonCastPlan.Outcome> planPermanent(ServerPlayer p,Vec3 point,double strength){
        AonEvaluator.checkPosition(p,point);var pos=BlockPos.containing(point);var level=p.serverLevel();
        if(!p.mayBuild()||!level.mayInteract(p,pos)||!p.mayUseItemAt(pos,Direction.UP,p.getMainHandItem()))return AonCastPlan.none("Light: building is not allowed here.");
        var before=level.getBlockState(pos);if(!before.isAir()&&!(before.is(AonLightBlock.LIGHT.get())&&!before.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT)))return AonCastPlan.none("Light: no clear space at those coordinates.");
        int brightness=(int)Math.max(1,Math.round(AonDorConfig.LIGHT_LEVEL.get()*Math.min(1,strength)));AonCost.add("permanent_light",brightness*20.0);
        return ()->{var snapshot=BlockSnapshot.create(level.dimension(),level,pos);if(!AonLightBlock.placePermanent(level,pos,brightness))return new AonCastPlan.Outcome("Light: no clear space at those coordinates.",false);
            if(EventHooks.onBlockPlace(p,snapshot,Direction.UP)){snapshot.restore();return new AonCastPlan.Outcome("Light: placement was denied.",false);}return new AonCastPlan.Outcome("Light: permanent light created.",true);};
    }
    static Supplier<AonCastPlan.Outcome> planTemporary(ServerPlayer p,Vec3 point,double strength){
        var level=p.serverLevel();if(!p.mayBuild())return AonCastPlan.none("Light: building is not allowed here.");var origin=BlockPos.containing(point);
        for(var pos:List.of(origin,origin.above(),origin.below())){if(!level.hasChunkAt(pos)||!level.isInWorldBounds(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.mayInteract(p,pos)||!level.getBlockState(pos).isAir())continue;
            int brightness=(int)Math.max(1,Math.round(AonDorConfig.LIGHT_LEVEL.get()*Math.min(1,strength))),ticks=(int)Math.max(1,Math.round(AonDorConfig.LIGHT_SECONDS.get()*20*strength));AonCost.add("temporary_light",brightness*ticks/400.0);
            return ()->{var snapshot=BlockSnapshot.create(level.dimension(),level,pos);if(!AonLightBlock.place(level,pos,brightness,ticks))return new AonCastPlan.Outcome("Light: no clear space nearby.",false);if(EventHooks.onBlockPlace(p,snapshot,Direction.UP)){snapshot.restore();return new AonCastPlan.Outcome("Light: placement was denied.",false);}return new AonCastPlan.Outcome("Light: temporary light created.",true);};
        }return AonCastPlan.none("Light: no clear space nearby.");
    }
    public static String glow(ServerPlayer p,List<Entity> targets,double strength){return planGlow(p,targets,strength).get().message();}
    static Supplier<AonCastPlan.Outcome> planGlow(ServerPlayer p,List<Entity> targets,double strength){
        if(targets.size()>AonDorConfig.MAX_TARGETS.get())throw new IllegalArgumentException("Too many Light targets");var unique=new LinkedHashSet<>(targets);
        for(var target:unique){AonEvaluator.checkPosition(p,target.position());if(!target.isAlive()||target.isSpectator()||target.level()!=p.level())throw new IllegalArgumentException("A Light target is no longer available");}
        int ticks=(int)Math.max(1,Math.round(AonDorConfig.GLOW_SECONDS.get()*20*strength));AonCost.add("glowing",unique.size()*ticks/200.0);
        return ()->{for(var target:unique){AonEvaluator.checkPosition(p,target.position());if(!target.isAlive()||target.isSpectator()||target.level()!=p.level())throw new IllegalArgumentException("Light target changed during payment");}for(var target:unique)applyGlow(target,ticks);return new AonCastPlan.Outcome("Light: glowing applied to "+unique.size()+" target(s).",!unique.isEmpty());};
    }
    static void applyGlow(Entity entity,int ticks){
        if(entity instanceof LivingEntity living){living.addEffect(new MobEffectInstance(MobEffects.GLOWING,ticks,0,false,false,true));return;}
        if(!(entity.level() instanceof ServerLevel level))return;var data=entity.getPersistentData();
        // A pre-existing glow belongs to another source and must never get an expiry from us.
        if(!data.contains(UNTIL)&&entity.isCurrentlyGlowing())return;
        long until=Math.max(data.getLong(UNTIL),level.getServer().overworld().getGameTime()+ticks);data.putLong(UNTIL,until);entity.setGlowingTag(true);TRACKED.put(entity,until);
    }
    static void joined(Entity entity){
        if(!(entity.level() instanceof ServerLevel level)||!entity.getPersistentData().contains(UNTIL))return;
        long until=entity.getPersistentData().getLong(UNTIL);if(until<=level.getServer().overworld().getGameTime()){entity.setGlowingTag(false);entity.getPersistentData().remove(UNTIL);}else{entity.setGlowingTag(true);TRACKED.put(entity,until);}
    }
    static void tick(MinecraftServer server){
        long now=server.overworld().getGameTime();var iterator=TRACKED.entrySet().iterator();
        while(iterator.hasNext()){var entry=iterator.next();var entity=entry.getKey();if(entity.isRemoved()){iterator.remove();continue;}if(entity.level() instanceof ServerLevel level&&level.getServer()==server&&now>=entry.getValue()){entity.setGlowingTag(false);entity.getPersistentData().remove(UNTIL);iterator.remove();}}
    }
    static void stopped(){TRACKED.clear();}
    private AonLightEffects(){}
}
