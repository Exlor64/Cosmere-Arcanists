package dev.cosmerearcanists.nicrosil;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Unknown resource IDs survive save/load while their supplying system is absent or disabled. */
public final class NicrosilData extends SavedData {
    public final NicrosilLedger ledger=new NicrosilLedger();
    public static NicrosilData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(NicrosilData::new,NicrosilData::load,null),"cosmere_nicrosil");}
    public static NicrosilData load(CompoundTag root,HolderLookup.Provider lookup){
        var data=new NicrosilData();var minds=root.getList("minds",Tag.TAG_COMPOUND);
        for(int i=0;i<minds.size();i++){var tag=minds.getCompound(i);if(!tag.hasUUID("id"))continue;try{var mind=new NicrosilLedger.Mind(tag.getUUID("id"),tag.hasUUID("owner")?tag.getUUID("owner"):null,tag.getDouble("capacity"));var entries=tag.getList("entries",Tag.TAG_COMPOUND);for(int j=0;j<entries.size();j++){var e=entries.getCompound(j);if(e.hasUUID("id"))mind.add(new NicrosilLedger.Entry(e.getUUID("id"),e.getInt("kind"),e.getString("key"),e.getDouble("amount"),e.getDouble("charge"),e.hasUUID("source_identity")?e.getUUID("source_identity"):e.getInt("source_schema")>=1?null:mind.owner),NicrosilLedger.HARD_ENTRIES);}data.ledger.add(mind,Integer.MAX_VALUE);}catch(IllegalArgumentException ignored){}}
        return data;
    }
    @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider lookup){var minds=new ListTag();for(var mind:ledger.minds()){var tag=new CompoundTag();tag.putUUID("id",mind.id);if(mind.owner!=null)tag.putUUID("owner",mind.owner);tag.putDouble("capacity",mind.capacity);var entries=new ListTag();for(var entry:mind.entries()){var e=new CompoundTag();e.putUUID("id",entry.id());e.putInt("kind",entry.kind());e.putString("key",entry.key());e.putDouble("amount",entry.amount());e.putDouble("charge",entry.charge());e.putInt("source_schema",1);if(entry.sourceIdentity()!=null)e.putUUID("source_identity",entry.sourceIdentity());entries.add(e);}tag.put("entries",entries);minds.add(tag);}root.put("minds",minds);return root;}
}
