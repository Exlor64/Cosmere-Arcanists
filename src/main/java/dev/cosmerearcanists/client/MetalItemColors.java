package dev.cosmerearcanists.client;
import dev.cosmerearcanists.ModMetals;
import dev.cosmerearcanists.filing.Filings;
import dev.cosmerearcanists.power.MetalVialItem;
import dev.cosmerearcanists.visual.MetalPalette;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
/** Client-only tint registration; original 16px source artwork is never recolored or resampled on disk. */
public final class MetalItemColors {
    public static int color(ItemStack stack,int index){
        if(index!=0)return -1;
        if(stack.getItem() instanceof dev.cosmerearcanists.filing.MetalFilingItem)return MetalPalette.color(Filings.metal(stack));
        if(stack.getItem() instanceof MetalVialItem)return MetalPalette.mixture(Filings.contents(stack));
        String path=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();int split=path.lastIndexOf('_');return split<0?-1:MetalPalette.color(path.substring(0,split));
    }
    public static void register(RegisterColorHandlersEvent.Item event){
        for(var entry:ModMetals.ITEMS.getEntries()){var item=entry.get();String path=BuiltInRegistries.ITEM.getKey(item).getPath();if(path.endsWith("_spike")||path.endsWith("_ingot")||item instanceof dev.cosmerearcanists.filing.MetalFilingItem||item instanceof MetalVialItem)event.register(MetalItemColors::color,item);}
        event.register(MetalItemColors::color,Items.IRON_INGOT,Items.COPPER_INGOT,Items.GOLD_INGOT);
    }
    private MetalItemColors(){}
}
