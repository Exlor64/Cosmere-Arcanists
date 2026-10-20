package dev.cosmerearcanists.feruchemy;
import java.util.*;
import dev.cosmerearcanists.ModMetals;
import net.minecraft.server.level.ServerPlayer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
/** Referenced only after ModList confirms Curios; the standalone mod has no runtime dependency. */
public final class CuriosMetalminds {
    public static void register(){for(var holder:ModMetals.ITEMS.getEntries())if(holder.get() instanceof MetalmindItem)CuriosApi.registerCurio(holder.get(),new ICurioItem(){});}
    /** Our additive delta replaces only our defaults; unrelated slot modifiers remain intact. */
    public static void configureSlots(ServerPlayer p){
        CuriosApi.getCuriosInventory(p).ifPresent(inventory->{
            for(String slot:List.of("ring","bracelet","necklace"))inventory.getStacksHandler(slot).ifPresent(holder->{
                int baseline=slot.equals("necklace")?1:2;
                int desired=switch(slot){case "ring"->FeruchemyConfig.RING_SLOTS.get();case "bracelet"->FeruchemyConfig.BRACELET_SLOTS.get();default->FeruchemyConfig.NECKLACE_SLOTS.get();};
                double delta=FeruchemyConfig.CURIOS.get()?desired-baseline:0;
                var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(dev.cosmerearcanists.CosmereArcanists.MOD_ID,"default_metalmind_"+slot+"_slots");
                var previous=holder.getModifiers().get(id);if(previous==null&&delta==0||previous!=null&&previous.amount()==delta) return;
                // Curios9.5.1 drops invalid functional pieces, but resizing can discard cosmetic pieces.
                // Temporarily remove cosmetics, then restore or return them after the resize.
                var cosmetics=holder.getCosmeticStacks();var saved=new ArrayList<net.minecraft.world.item.ItemStack>();
                for(int i=0;i<cosmetics.getSlots();i++){saved.add(cosmetics.getStackInSlot(i));cosmetics.setStackInSlot(i,net.minecraft.world.item.ItemStack.EMPTY);}
                inventory.removeSlotModifier(slot,id);
                if(delta!=0)inventory.addTransientSlotModifier(slot,id,delta,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
                holder.update();cosmetics=holder.getCosmeticStacks();
                for(int i=0;i<saved.size();i++){var item=saved.get(i);if(i<cosmetics.getSlots())cosmetics.setStackInSlot(i,item);else if(!item.isEmpty())dev.cosmerearcanists.filing.Filings.give(p,item);}
            });
        });
    }
    public static List<Metalminds.Access> touching(ServerPlayer p){
        var result=new ArrayList<Metalminds.Access>();CuriosApi.getCuriosInventory(p).ifPresent(inventory->{
            for(String slot:List.of("ring","bracelet","necklace"))inventory.getStacksHandler(slot).ifPresent(holder->{
                var stacks=holder.getStacks();var active=holder.getActiveStates();
                for(int i=0;i<stacks.getSlots();i++){
                    var s=stacks.getStackInSlot(i);if(!(s.getItem() instanceof MetalmindItem item)||!item.slot().equals(slot)||s.getCount()!=1||!active.get(i))continue;
                    int index=i;result.add(new Metalminds.Access(s,()->stacks.setStackInSlot(index,s)));
                }
            });
        });return result;
    }
    private CuriosMetalminds(){}
}
