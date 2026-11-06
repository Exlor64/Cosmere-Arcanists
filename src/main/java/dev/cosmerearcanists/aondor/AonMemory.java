package dev.cosmerearcanists.aondor;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import dev.cosmerearcanists.copper.CopperService;
import static dev.cosmerearcanists.aondor.AonProgram.Type;

/** Bounded typed snapshots: entity UUIDs, never entity NBT or chunk tickets. */
public final class AonMemory {
    private record Entry(long expires,CompoundTag value){}
    private static final Map<MinecraftServer,Map<UUID,Map<Integer,Entry>>> TEMPORARY=new WeakHashMap<>();
    public static int key(double value){if(!Double.isFinite(value)||value<0||value>1_000_000||value!=Math.rint(value))throw new IllegalArgumentException("Memory key must be a whole number from 0 to 1000000");return (int)value;}
    static void cleanup(MinecraftServer server){var players=TEMPORARY.get(server);if(players==null)return;long now=server.overworld().getGameTime();players.values().forEach(entries->entries.values().removeIf(e->e.expires()<=now));players.values().removeIf(Map::isEmpty);}
    static void clear(MinecraftServer server){TEMPORARY.remove(server);}
    private static Map<Integer,Entry> temporary(ServerPlayer p){
        var players=TEMPORARY.computeIfAbsent(p.server,s->new HashMap<>());long now=p.server.overworld().getGameTime();
        players.values().forEach(entries->entries.values().removeIf(e->e.expires()<=now));players.values().removeIf(Map::isEmpty);
        if(!players.containsKey(p.getUUID())&&players.size()>=1024)throw new IllegalArgumentException("Temporary Aon memory is busy");
        return players.computeIfAbsent(p.getUUID(),id->new HashMap<>());
    }
    static String remember(ServerPlayer p,double index,AonEvaluator.Value value){
        int key=key(index);var snapshot=encode(p,value);
        if(CopperService.memoryMode(p)<0){CopperService.storeVariable(p,key,snapshot);return "Remember: key "+key+" stored in coppermind.";}
        var entries=temporary(p);if(!entries.containsKey(key)&&entries.size()>=AonDorConfig.MEMORY_ENTRIES.get())throw new IllegalArgumentException("Temporary Aon memory is full");
        entries.put(key,new Entry(p.server.overworld().getGameTime()+20L*AonDorConfig.MEMORY_SECONDS.get(),snapshot));return "Remember: key "+key+" stored for "+AonDorConfig.MEMORY_SECONDS.get()+" seconds.";
    }
    static AonEvaluator.Value recall(ServerPlayer p,double index){
        int key=key(index);CompoundTag snapshot;
        if(CopperService.memoryMode(p)>0)snapshot=CopperService.recallVariable(p,key);
        else{var entry=temporary(p).get(key);if(entry==null)throw new IllegalArgumentException("Memory key "+key+" is missing or expired");snapshot=entry.value();}
        return decode(p,snapshot);
    }
    @SuppressWarnings("unchecked") static CompoundTag encode(ServerPlayer p,AonEvaluator.Value value){
        var tag=new CompoundTag();tag.putString("type",value.type().name());Object data=value.data();
        switch(value.type()){
            case NUMBER->{double n=(Double)data;if(!Double.isFinite(n))throw new IllegalArgumentException("Cannot remember a nonfinite number");tag.putDouble("number",n);}
            case DIRECTION,VECTOR,POSITION->{var v=(Vec3)data;vector(tag,v);if(value.type()==Type.POSITION)dimension(p,tag);}
            case BLOCK->{tag.putLong("block",((BlockPos)data).asLong());dimension(p,tag);}
            case ENTITY->{entity(tag,(LivingEntity)data);dimension(p,tag);}
            case ENTITIES->{var list=(List<LivingEntity>)data;if(list.size()>AonDorConfig.MAX_TARGETS.get())throw new IllegalArgumentException("Too many remembered entities");var refs=new ListTag();for(var entity:list){var ref=new CompoundTag();entity(ref,entity);refs.add(ref);}tag.put("entities",refs);dimension(p,tag);}
            case REGION->{var region=(AonEvaluator.Sphere)data;vector(tag,region.center());tag.putDouble("radius",region.radius());dimension(p,tag);}
            default->throw new IllegalArgumentException("Remember requires a concrete value");
        }
        return tag;
    }
    private static void dimension(ServerPlayer p,CompoundTag tag){tag.putString("dimension",p.level().dimension().location().toString());}
    private static void entity(CompoundTag tag,LivingEntity entity){tag.putUUID("entity",entity.getUUID());}
    private static void vector(CompoundTag tag,Vec3 v){if(!Double.isFinite(v.x)||!Double.isFinite(v.y)||!Double.isFinite(v.z))throw new IllegalArgumentException("Cannot remember nonfinite coordinates");tag.putDouble("x",v.x);tag.putDouble("y",v.y);tag.putDouble("z",v.z);}
    private static LivingEntity resolve(ServerPlayer p,CompoundTag tag){
        if(!tag.hasUUID("entity"))throw new IllegalArgumentException("Invalid remembered entity reference");
        var entity=p.serverLevel().getEntity(tag.getUUID("entity"));
        if(entity==null&&p.getUUID().equals(tag.getUUID("entity")))entity=p;
        if(!(entity instanceof LivingEntity living)||!living.isAlive()||living.isSpectator())throw new IllegalArgumentException("Remembered entity is dead, unloaded or unavailable");return living;
    }
    static AonEvaluator.Value decode(ServerPlayer p,CompoundTag tag){
        if(tag.contains("dimension")&&!tag.getString("dimension").equals(p.level().dimension().location().toString()))throw new IllegalArgumentException("Remembered value belongs to another dimension");
        final Type type;try{type=Type.valueOf(tag.getString("type"));}catch(IllegalArgumentException e){throw new IllegalArgumentException("Invalid memory value type");}
        Object data=switch(type){
            case NUMBER->{double n=tag.getDouble("number");if(!Double.isFinite(n))throw new IllegalArgumentException("Invalid remembered number");yield n;}
            case DIRECTION,VECTOR,POSITION->{var v=new Vec3(tag.getDouble("x"),tag.getDouble("y"),tag.getDouble("z"));new AonMath.Vector(v.x,v.y,v.z);yield v;}
            case BLOCK->BlockPos.of(tag.getLong("block"));
            case ENTITY->resolve(p,tag);
            case ENTITIES->{var refs=tag.getList("entities",Tag.TAG_COMPOUND);if(refs.size()>AonDorConfig.MAX_TARGETS.get())throw new IllegalArgumentException("Stored list exceeds the current target limit");var list=new ArrayList<LivingEntity>();for(int i=0;i<refs.size();i++)list.add(resolve(p,refs.getCompound(i)));yield List.copyOf(list);}
            case REGION->{var v=new Vec3(tag.getDouble("x"),tag.getDouble("y"),tag.getDouble("z"));new AonMath.Vector(v.x,v.y,v.z);double radius=tag.getDouble("radius");if(!Double.isFinite(radius)||radius<=0)throw new IllegalArgumentException("Invalid remembered region");yield new AonEvaluator.Sphere(v,radius);}
            default->throw new IllegalArgumentException("Invalid remembered value");
        };
        return new AonEvaluator.Value(type,data);
    }
    private AonMemory(){}
}
