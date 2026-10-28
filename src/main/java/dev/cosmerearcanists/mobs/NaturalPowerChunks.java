package dev.cosmerearcanists.mobs;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
/** Overworld game time is common to all dimensions; sleep changes day time, not this clock. */
public final class NaturalPowerChunks extends SavedData {
    private static final java.util.Map<MinecraftServer,NaturalPowerChunks> CACHE=new java.util.WeakHashMap<>();
    private java.lang.ref.WeakReference<MinecraftServer> serverRef=new java.lang.ref.WeakReference<>(null);
    public final ChunkPowerLedger ledger=new ChunkPowerLedger();
    public static NaturalPowerChunks install(net.minecraft.server.level.ServerLevel level){
        synchronized(CACHE){return CACHE.computeIfAbsent(level.getServer(),server->{var data=level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(NaturalPowerChunks::new,NaturalPowerChunks::load,null),"cosmere_natural_power_chunks");data.serverRef=new java.lang.ref.WeakReference<>(server);return data;});}
    }
    public static NaturalPowerChunks get(MinecraftServer server){
        synchronized(CACHE){var data=CACHE.get(server);return data!=null?data:install(server.overworld());}
    }
    private void changed(){
        setDirty();var server=serverRef.get();
        // World-generation births can occur off-thread. Queue a dirty mark after any in-progress main-thread save.
        if(server!=null&&!server.isSameThread())server.execute(this::setDirty);
    }
    public synchronized boolean available(ChunkPowerLedger.Key key,long now){int before=ledger.size();boolean result=ledger.available(key,now);if(before!=ledger.size())changed();return result;}
    public synchronized boolean claim(ChunkPowerLedger.Key key,long now,long ticks){int before=ledger.size();boolean result=ledger.claim(key,now,ticks);if(result&&ticks>0||before!=ledger.size())changed();return result;}
    public static NaturalPowerChunks load(CompoundTag root,HolderLookup.Provider lookup){
        var data=new NaturalPowerChunks();var entries=root.getList("chunks",Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(entries.size(),ChunkPowerLedger.HARD_LIMIT);i++){
            var tag=entries.getCompound(i);String dimension=tag.getString("dimension");
            if(dimension.length()>128||ResourceLocation.tryParse(dimension)==null||!tag.contains("chunk",Tag.TAG_LONG)||!tag.contains("until",Tag.TAG_LONG))continue;
            data.ledger.restore(new ChunkPowerLedger.Key(dimension,tag.getLong("chunk")),tag.getLong("until"));
        }return data;
    }
    @Override public synchronized CompoundTag save(CompoundTag root,HolderLookup.Provider lookup){
        var entries=new ListTag();for(var entry:ledger.entries().entrySet()){var tag=new CompoundTag();tag.putString("dimension",entry.getKey().dimension());tag.putLong("chunk",entry.getKey().chunk());tag.putLong("until",entry.getValue());entries.add(tag);}root.put("chunks",entries);return root;
    }
}
