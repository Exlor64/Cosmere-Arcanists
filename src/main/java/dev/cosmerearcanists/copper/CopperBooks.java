package dev.cosmerearcanists.copper;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
/** Metadata is world SavedData; item identity points here, image bytes never enter item NBT or metadata packets. */
public final class CopperBooks extends SavedData {
    public final Map<UUID,LinkedHashMap<UUID,CopperPage>> books=new LinkedHashMap<>();
    public final Set<UUID> erasedImages=new LinkedHashSet<>();
    public static CopperBooks get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(CopperBooks::new,CopperBooks::load,null),"cosmere_copperminds");}
    public static CopperBooks load(CompoundTag root,HolderLookup.Provider lookup){var data=new CopperBooks();var erased=root.getList("erased_images",Tag.TAG_COMPOUND);for(int i=0;i<erased.size();i++)if(erased.getCompound(i).hasUUID("id"))data.erasedImages.add(erased.getCompound(i).getUUID("id"));var books=root.getList("books",Tag.TAG_COMPOUND);for(int i=0;i<books.size();i++){var b=books.getCompound(i);if(!b.hasUUID("id"))continue;var pages=new LinkedHashMap<UUID,CopperPage>();var list=b.getList("pages",Tag.TAG_COMPOUND);for(int j=0;j<Math.min(64,list.size());j++)try{var p=CopperPage.load(list.getCompound(j));pages.put(p.id(),p);}catch(IllegalArgumentException ignored){}data.books.put(b.getUUID("id"),pages);}return data;}
    @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider lookup){var all=new ListTag();books.forEach((id,pages)->{var b=new CompoundTag();b.putUUID("id",id);var list=new ListTag();pages.values().forEach(p->list.add(p.save()));b.put("pages",list);all.add(b);});root.put("books",all);var erased=new ListTag();erasedImages.forEach(id->{var t=new CompoundTag();t.putUUID("id",id);erased.add(t);});root.put("erased_images",erased);return root;}
}
