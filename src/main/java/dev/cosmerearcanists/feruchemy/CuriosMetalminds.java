package dev.cosmerearcanists.feruchemy;
import java.util.*;
import dev.cosmerearcanists.ModMetals;
import net.minecraft.server.level.ServerPlayer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
/** Referenced only after ModList confirms Curios; the standalone mod has no runtime dependency. */
public final class CuriosMetalminds {
    public static void register(){for(var holder:ModMetals.ITEMS.getEntries())if(holder.get() instanceof MetalmindItem)CuriosApi.registerCurio(holder.get(),new ICurioItem(){});}
    public static List<Metalminds.Access> touching(ServerPlayer p){
        var result=new ArrayList<Metalminds.Access>();CuriosApi.getCuriosInventory(p).ifPresent(inventory->{
            for(String slot:List.of("ring","bracelet","necklace"))inventory.getStacksHandler(slot).ifPresent(holder->{
                var stacks=holder.getStacks();int limit=slot.equals("necklace")?1:2;var active=holder.getActiveStates();
                for(int i=0;i<Math.min(limit,stacks.getSlots());i++){
                    var s=stacks.getStackInSlot(i);if(!(s.getItem() instanceof MetalmindItem item)||!item.slot().equals(slot)||s.getCount()!=1||!active.get(i))continue;
                    int index=i;result.add(new Metalminds.Access(s,()->stacks.setStackInSlot(index,s)));
                }
            });
        });return result;
    }
    private CuriosMetalminds(){}
}
