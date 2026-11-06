package dev.cosmerearcanists.aondor;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import static dev.cosmerearcanists.aondor.AonProgram.Type;

/** Every expression is resolved and type-checked before a world-changing effect. */
public final class AonEvaluator {
    record Sphere(Vec3 center,double radius){}
    record Value(Type type,Object data){}
    static Object value(ServerPlayer player,AonProgram.Node node){return evaluate(player,node).data();}
    private static Value require(Value value,Type... expected){if(Arrays.stream(expected).noneMatch(t->t==value.type()))throw new IllegalArgumentException("Stored/expression value is "+value.type()+"; expected "+Arrays.toString(expected));return value;}
    private static Object input(ServerPlayer p,AonProgram.Node n,Type... types){return require(evaluate(p,n),types).data();}
    @SuppressWarnings("unchecked") static Value evaluate(ServerPlayer p,AonProgram.Node node){
        var args=node.arguments();String key=node.id().substring(17);
        if(key.equals("recall"))return AonMemory.recall(p,args.isEmpty()?0:(Double)input(p,args.get(0),Type.NUMBER));
        if(AonMath.operation(node.id())){
            var resolved=args.stream().map(n->evaluate(p,n)).toList();
            var runtimeNodes=resolved.stream().map(v->new AonProgram.Node("runtime",0,v.type(),List.of())).toList();
            var type=AonMath.resultType(key,runtimeNodes);var values=new Object[resolved.size()];
            for(int i=0;i<values.length;i++){Object v=resolved.get(i).data();values[i]=v instanceof Vec3 vector?new AonMath.Vector(vector.x,vector.y,vector.z):v;}
            Object result=AonMath.apply(key,values);return new Value(type,result instanceof AonMath.Vector v?new Vec3(v.x(),v.y(),v.z()):result);
        }
        Object result=switch(key){
            case "caster"->p;
            case "gaze_entity"->{var target=AonDorEffects.target(p);if(target==null)throw new IllegalArgumentException("Gaze Entity found no target");yield target;}
            case "gaze_block"->{var pos=AonDorEffects.gazeBlock(p);if(pos==null)throw new IllegalArgumentException("Gaze Block found no block");yield pos;}
            case "position_of"->{Object target=input(p,args.get(0),Type.ENTITY,Type.BLOCK);yield target instanceof LivingEntity entity?entity.position():((BlockPos)target).getCenter();}
            case "look_direction"->{var entity=args.isEmpty()?p:(LivingEntity)input(p,args.get(0),Type.ENTITY);yield entity.getLookAngle().normalize();}
            case "number"->node.number();
            case "sphere"->{var center=(Vec3)input(p,args.get(0),Type.POSITION);double radius=(Double)input(p,args.get(1),Type.NUMBER);if(radius<=0||radius>AonDorConfig.RANGE.get())throw new IllegalArgumentException("Sphere radius must be positive and at most "+AonDorConfig.RANGE.get());checkPosition(p,center);yield new Sphere(center,radius);}
            case "entities_in"->{var region=(Sphere)input(p,args.get(0),Type.REGION);yield entities(p,region);}
            case "exclude"->{var list=(List<LivingEntity>)input(p,args.get(0),Type.ENTITIES);Object removed=input(p,args.get(1),Type.ENTITY,Type.ENTITIES);var excluded=removed instanceof List<?> entries?entries:List.of(removed);yield list.stream().filter(e->!excluded.contains(e)).toList();}
            case "players"->((List<LivingEntity>)input(p,args.get(0),Type.ENTITIES)).stream().filter(e->e instanceof net.minecraft.world.entity.player.Player).toList();
            case "mobs"->((List<LivingEntity>)input(p,args.get(0),Type.ENTITIES)).stream().filter(e->e instanceof net.minecraft.world.entity.Mob).toList();
            case "nearest"->{var list=(List<LivingEntity>)input(p,args.get(0),Type.ENTITIES);var origin=args.size()>1?(Vec3)input(p,args.get(1),Type.POSITION):p.position();checkPosition(p,origin);yield list.stream().min(Comparator.comparingDouble((LivingEntity e)->e.position().distanceToSqr(origin)).thenComparingInt(LivingEntity::getId)).orElseThrow(()->new IllegalArgumentException("Nearest found no targets"));}
            default->throw new IllegalArgumentException("Unsupported targeting operation "+key);
        };
        return new Value(node.type(),result);
    }
    private static List<LivingEntity> entities(ServerPlayer p,Sphere region){
        double radius=region.radius();if(!Double.isFinite(radius)||radius<=0||radius>AonDorConfig.RANGE.get())throw new IllegalArgumentException("Stored region exceeds the current radius limit");checkPosition(p,region.center());
        var box=new AABB(region.center().subtract(radius,radius,radius),region.center().add(radius,radius,radius));
        for(int x=(int)Math.floor(box.minX/16);x<=(int)Math.floor(box.maxX/16);x++)for(int z=(int)Math.floor(box.minZ/16);z<=(int)Math.floor(box.maxZ/16);z++)if(!p.serverLevel().hasChunk(x,z))throw new IllegalArgumentException("Sphere reaches unloaded terrain");
        var list=p.serverLevel().getEntitiesOfClass(LivingEntity.class,box,e->e.isAlive()&&!e.isSpectator()&&e.position().distanceToSqr(region.center())<=radius*radius);
        list.sort(Comparator.comparingDouble((LivingEntity e)->e.position().distanceToSqr(region.center())).thenComparingInt(LivingEntity::getId));
        if(list.size()>AonDorConfig.MAX_TARGETS.get())throw new IllegalArgumentException("Too many targets; use a smaller sphere");return List.copyOf(list);
    }
    static void checkPosition(ServerPlayer p,Vec3 pos){
        if(!Double.isFinite(pos.x)||!Double.isFinite(pos.y)||!Double.isFinite(pos.z)||pos.distanceToSqr(p.getEyePosition())>AonDorConfig.RANGE.get()*AonDorConfig.RANGE.get()||!p.serverLevel().hasChunkAt(BlockPos.containing(pos))||!p.serverLevel().getWorldBorder().isWithinBounds(BlockPos.containing(pos)))throw new IllegalArgumentException("Target position is outside loaded casting range");
    }
    @SuppressWarnings("unchecked") public static String execute(ServerPlayer p,AonProgram program,double strength){
        var root=program.root();var args=root.arguments();
        if(root.id().equals("cosmerearcanists:remember")){
            double key=args.isEmpty()?0:(Double)input(p,args.get(0),Type.NUMBER);var remembered=args.size()>1?evaluate(p,args.get(1)):new Value(Type.ENTITY,p);return AonMemory.remember(p,key,remembered);
        }
        if(args.isEmpty())return AonDorEffects.cast(p,root.id(),strength);
        var resolved=evaluate(p,args.get(0));
        if(root.id().equals("cosmerearcanists:light")){var position=(Vec3)require(resolved,Type.POSITION).data();checkPosition(p,position);return AonDorEffects.lightAt(p,position,strength);}
        require(resolved,root.id().equals("cosmerearcanists:force")?new Type[]{Type.ENTITY,Type.ENTITIES}:new Type[]{Type.ENTITY,Type.ENTITIES,Type.BLOCK});
        Object target=resolved.data();var entities=target instanceof List<?>?(List<LivingEntity>)target:target instanceof LivingEntity entity?List.of(entity):List.<LivingEntity>of();
        var direction=args.size()>1?(Vec3)input(p,args.get(1),Type.DIRECTION,Type.VECTOR):p.getLookAngle();
        if(root.id().equals("cosmerearcanists:force")&&args.size()>1&&(!Double.isFinite(direction.length())||direction.length()>AonDorConfig.MAX_FORCE_VECTOR.get()))throw new IllegalArgumentException("Force vector magnitude exceeds "+AonDorConfig.MAX_FORCE_VECTOR.get());
        if(entities.size()>AonDorConfig.MAX_TARGETS.get())throw new IllegalArgumentException("Stored list exceeds the current target limit");
        for(var entity:entities){checkPosition(p,entity.position());if(!entity.isAlive()||entity.isSpectator()||entity.level()!=p.level())throw new IllegalArgumentException("A selected entity is no longer available");}
        if(root.id().equals("cosmerearcanists:force")){int applied=0;for(var entity:entities)if(AonDorEffects.push(p,entity,strength,direction,true))applied++;return "Force: pushed "+applied+" target(s).";}
        if(target instanceof BlockPos pos){checkPosition(p,pos.getCenter());return AonDorEffects.warmBlock(p,pos)?"Heat: block warmed.":"Heat: this block cannot be warmed.";}
        int warmed=0;for(var entity:entities)if(AonDorEffects.warm(entity,strength))warmed++;return "Heat: warmed "+warmed+" freezing target(s).";
    }
    private AonEvaluator(){}
}
