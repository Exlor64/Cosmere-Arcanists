package dev.cosmerearcanists.aondor;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;

/** Typed evaluation precedes mutation; selection failures never turn into default targeting. */
public final class AonEvaluator {
    private record Sphere(Vec3 center,double radius){}
    private static Object value(ServerPlayer player,AonProgram.Node node){
        var args=node.arguments();String key=node.id().substring(17);
        return switch(key){
            case "caster"->player;
            case "gaze_entity"->{var target=AonDorEffects.target(player);if(target==null)throw new IllegalArgumentException("Gaze Entity found no target");yield target;}
            case "gaze_block"->{var pos=AonDorEffects.gazeBlock(player);if(pos==null)throw new IllegalArgumentException("Gaze Block found no block");yield pos;}
            case "position_of"->{Object target=value(player,args.get(0));yield target instanceof LivingEntity entity?entity.position():((BlockPos)target).getCenter();}
            case "look_direction"->{var entity=args.isEmpty()?player:(LivingEntity)value(player,args.get(0));yield entity.getLookAngle().normalize();}
            case "number"->node.number();
            case "sphere"->{var center=(Vec3)value(player,args.get(0));double radius=(Double)value(player,args.get(1));if(radius<=0||radius>AonDorConfig.RANGE.get())throw new IllegalArgumentException("Sphere radius must be positive and at most "+AonDorConfig.RANGE.get());checkPosition(player,center);yield new Sphere(center,radius);}
            case "entities_in"->{var region=(Sphere)value(player,args.get(0));var box=new AABB(region.center().subtract(region.radius(),region.radius(),region.radius()),region.center().add(region.radius(),region.radius(),region.radius()));
                for(int x=(int)Math.floor(box.minX/16);x<=(int)Math.floor(box.maxX/16);x++)for(int z=(int)Math.floor(box.minZ/16);z<=(int)Math.floor(box.maxZ/16);z++)if(!player.serverLevel().hasChunk(x,z))throw new IllegalArgumentException("Sphere reaches unloaded terrain");
                var list=player.serverLevel().getEntitiesOfClass(LivingEntity.class,box,e->e.isAlive()&&!e.isSpectator()&&e.position().distanceToSqr(region.center())<=region.radius()*region.radius());
                list.sort(Comparator.comparingDouble((LivingEntity e)->e.position().distanceToSqr(region.center())).thenComparingInt(LivingEntity::getId));
                if(list.size()>AonDorConfig.MAX_TARGETS.get())throw new IllegalArgumentException("Too many targets; use a smaller sphere");yield List.copyOf(list);}
            default->throw new IllegalArgumentException("Unsupported targeting operation "+key);
        };
    }
    private static void checkPosition(ServerPlayer player,Vec3 pos){
        if(pos.distanceToSqr(player.getEyePosition())>AonDorConfig.RANGE.get()*AonDorConfig.RANGE.get()||!player.serverLevel().hasChunkAt(BlockPos.containing(pos))||!player.serverLevel().getWorldBorder().isWithinBounds(BlockPos.containing(pos)))throw new IllegalArgumentException("Target position is outside loaded casting range");
    }
    @SuppressWarnings("unchecked") public static String execute(ServerPlayer player,AonProgram program,double strength){
        var root=program.root();var args=root.arguments();if(args.isEmpty())return AonDorEffects.cast(player,root.id(),strength);
        Object target=value(player,args.get(0));
        if(root.id().equals("cosmerearcanists:light")){var position=(Vec3)target;checkPosition(player,position);return AonDorEffects.lightAt(player,position,strength);}
        var entities=target instanceof List<?>?(List<LivingEntity>)target:target instanceof LivingEntity entity?List.of(entity):List.<LivingEntity>of();
        // Resolve and validate all positional/directional inputs before changing the world.
        var direction=args.size()>1?(Vec3)value(player,args.get(1)):player.getLookAngle();
        for(var entity:entities){checkPosition(player,entity.position());if(!entity.isAlive()||entity.level()!=player.level())throw new IllegalArgumentException("A selected entity is no longer available");}
        if(root.id().equals("cosmerearcanists:force")){
            int applied=0;for(var entity:entities)if(AonDorEffects.push(player,entity,strength,direction,true))applied++;return "Force: pushed "+applied+" target(s).";
        }
        if(target instanceof BlockPos pos){checkPosition(player,pos.getCenter());return AonDorEffects.warmBlock(player,pos)?"Heat: block warmed.":"Heat: this block cannot be warmed.";}
        int warmed=0;for(var entity:entities)if(AonDorEffects.warm(entity,strength))warmed++;return "Heat: warmed "+warmed+" freezing target(s).";
    }
    private AonEvaluator(){}
}
