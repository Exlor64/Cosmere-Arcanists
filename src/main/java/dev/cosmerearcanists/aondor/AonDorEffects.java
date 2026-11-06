package dev.cosmerearcanists.aondor;

import dev.cosmerearcanists.power.PowerService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;

/** The three initial standalone effects. Every target is resolved from the server's current gaze. */
public final class AonDorEffects {
    public static String cast(ServerPlayer player,String id,double strength){
        return switch(id){
            case "cosmerearcanists:light"->light(player,strength);
            case "cosmerearcanists:force"->{var target=target(player);yield target!=null&&push(player,target,strength)?"Force: target pushed.":"Force: look at a valid entity within range.";}
            case "cosmerearcanists:heat"->heat(player,strength);
            default->"This Aon has no standalone effect configured.";
        };
    }
    private static BlockHitResult blockHit(ServerPlayer player){return player.serverLevel().clip(new ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(AonDorConfig.RANGE.get())),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));}
    private static boolean loadedRay(ServerPlayer player){
        var start=player.getEyePosition();var end=start.add(player.getLookAngle().scale(AonDorConfig.RANGE.get()));
        for(int i=0;i<=32;i++)if(!player.serverLevel().hasChunkAt(BlockPos.containing(start.lerp(end,i/32.0))))return false;return true;
    }
    public static LivingEntity target(ServerPlayer player){
        if(!loadedRay(player))return null;var start=player.getEyePosition();var hit=blockHit(player);var end=hit.getLocation();
        LivingEntity nearest=null;double distance=start.distanceToSqr(end);
        for(var entity:player.serverLevel().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().expandTowards(player.getLookAngle().scale(AonDorConfig.RANGE.get())).inflate(1),e->e!=player&&e.isAlive()&&!e.isSpectator())){
            var intersection=entity.getBoundingBox().inflate(.1).clip(start,end);if(intersection.isEmpty())continue;double next=start.distanceToSqr(intersection.get());if(next<distance){nearest=entity;distance=next;}
        }return nearest;
    }
    public static boolean push(ServerPlayer player,LivingEntity target,double strength){return push(player,target,strength,player.getLookAngle(),false);}
    public static boolean push(ServerPlayer player,LivingEntity target,double strength,Vec3 heading,boolean explicit){
        if((target==player&&!explicit)||!target.isAlive()||target.isSpectator()||target.level()!=player.level()||target!=player&&(player.isAlliedTo(target)||target instanceof ServerPlayer other&&!player.canHarmPlayer(other)))return false;
        var direction=heading.normalize().scale(AonDorConfig.FORCE.get()*strength);
        if(target instanceof ServerPlayer other)PowerService.externalImpulse(other,direction);
        else {target.setDeltaMovement(target.getDeltaMovement().add(direction));target.hasImpulse=true;target.hurtMarked=true;}
        player.serverLevel().sendParticles(ParticleTypes.END_ROD,target.getX(),target.getY()+target.getBbHeight()*.5,target.getZ(),8,.15,.15,.15,.01);return true;
    }
    private static String light(ServerPlayer player,double strength){
        return lightAt(player,player.getEyePosition().add(player.getLookAngle().scale(.8)),strength);
    }
    public static String lightAt(ServerPlayer player,Vec3 point,double strength){
        if(!player.getAbilities().mayBuild)return "Light: building is not allowed here.";
        var origin=BlockPos.containing(point);
        for(var pos:java.util.List.of(origin,origin.above(),origin.below()))if(player.serverLevel().mayInteract(player,pos)&&AonLightBlock.place(player.serverLevel(),pos,(int)Math.round(AonDorConfig.LIGHT_LEVEL.get()*Math.min(1,strength)),(int)Math.round(AonDorConfig.LIGHT_SECONDS.get()*20*strength))){
            player.serverLevel().sendParticles(ParticleTypes.END_ROD,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,16,.12,.12,.12,.01);return "Light: temporary light created.";
        }return "Light: no clear space nearby.";
    }
    public static boolean warm(LivingEntity target,double strength){int before=target.getTicksFrozen();if(before<=0)return false;target.setTicksFrozen(Math.max(0,before-(int)Math.round(AonDorConfig.THAW.get()*strength)));return true;}
    private static String heat(ServerPlayer player,double strength){
        var target=target(player);if(target!=null){boolean warmed=warm(target,strength);player.serverLevel().sendParticles(ParticleTypes.FLAME,target.getX(),target.getY()+target.getBbHeight()*.5,target.getZ(),10,.15,.15,.15,.005);return warmed?"Heat: target warmed; freezing reduced.":"Heat: target is already warm.";}
        if(!loadedRay(player))return "Heat: target area is not loaded.";var hit=player.serverLevel().clip(new ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(AonDorConfig.RANGE.get())),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));if(hit.getType()==HitResult.Type.MISS)return "Heat: look at a cold block, campfire or freezing entity.";
        return warmBlock(player,hit.getBlockPos())?"Heat: block warmed.":"Heat: this block has no basic warming interaction.";
    }
    public static BlockPos gazeBlock(ServerPlayer player){
        if(!loadedRay(player))return null;var hit=player.serverLevel().clip(new ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(AonDorConfig.RANGE.get())),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));return hit.getType()==HitResult.Type.MISS?null:hit.getBlockPos();
    }
    public static boolean warmBlock(ServerPlayer player,BlockPos pos){
        var level=player.serverLevel();if(!level.hasChunkAt(pos)||!player.getAbilities().mayBuild||!level.mayInteract(player,pos))return false;
        var state=level.getBlockState(pos);boolean changed=false;
        if(state.is(Blocks.SNOW)){int layers=state.getValue(SnowLayerBlock.LAYERS);changed=level.setBlock(pos,layers>1?state.setValue(SnowLayerBlock.LAYERS,layers-1):Blocks.AIR.defaultBlockState(),3);}
        else if(state.is(Blocks.ICE)||state.is(Blocks.FROSTED_ICE))changed=level.setBlock(pos,level.dimensionType().ultraWarm()?Blocks.AIR.defaultBlockState():Blocks.WATER.defaultBlockState(),3);
        else if(state.getBlock() instanceof CampfireBlock&&!state.getValue(BlockStateProperties.LIT)&&!state.getValue(BlockStateProperties.WATERLOGGED))changed=level.setBlock(pos,state.setValue(BlockStateProperties.LIT,true),3);
        if(changed)level.sendParticles(ParticleTypes.FLAME,pos.getX()+.5,pos.getY()+.7,pos.getZ()+.5,12,.2,.2,.2,.005);
        return changed;
    }
    private AonDorEffects(){}
}
