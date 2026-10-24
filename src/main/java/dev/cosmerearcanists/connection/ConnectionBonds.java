package dev.cosmerearcanists.connection;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World-wide relationship history survives death, logout and dimension changes. */
public final class ConnectionBonds extends SavedData {
    public final ConnectionLedger ledger=new ConnectionLedger();
    public static ConnectionBonds get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ConnectionBonds::new,ConnectionBonds::load,null),"cosmere_connections");}
    public static ConnectionBonds load(CompoundTag root,HolderLookup.Provider lookup){var data=new ConnectionBonds();var list=root.getList("bonds",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(262144,list.size());i++){var t=list.getCompound(i);if(!t.hasUUID("first")||!t.hasUUID("second"))continue;try{data.ledger.restore(new ConnectionLedger.Pair(t.getUUID("first"),t.getUUID("second")),new ConnectionLedger.Bond(t.getDouble("strength"),t.getLong("nearby_tick"),t.getLong("interaction_tick")));}catch(IllegalArgumentException ignored){}}return data;}
    @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider lookup){var list=new ListTag();ledger.entries().forEach((pair,bond)->{var t=new CompoundTag();t.putUUID("first",pair.first());t.putUUID("second",pair.second());t.putDouble("strength",bond.strength());t.putLong("nearby_tick",bond.proximityTick());t.putLong("interaction_tick",bond.interactionTick());list.add(t);});root.put("bonds",list);return root;}
}
