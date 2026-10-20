package dev.cosmerearcanists.feruchemy;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
public final class MetalmindEvents {
    @SubscribeEvent public void tooltip(ItemTooltipEvent event){var s=event.getItemStack();var source=Metalminds.source(s);if(source==null)return;
        if(Metalminds.bound(s)||s.getItem() instanceof MetalmindItem){event.getToolTip().add(Component.literal(String.format(Locale.ROOT,"%s metalmind: %.2f / %.2f attribute-seconds",source.metal(),Metalminds.charge(s),Metalminds.capacity(s))));if(Metalminds.storedMetal(s).startsWith("tin:"))event.getToolTip().add(Component.literal("Stored sense: "+Metalminds.storedMetal(s).substring(4)));var owner=Metalminds.owner(s);if(owner!=null)event.getToolTip().add(Component.literal("Bound to "+owner));event.getToolTip().add(Component.literal("Hold one item in either hand to store or tap."));}
        if(Metalminds.bound(s))event.getToolTip().add(Component.literal("Bound metalmind: cannot be crafted, filed, processed, or placed."));
    }
    @SubscribeEvent public void place(PlayerInteractEvent.RightClickBlock event){if(event.getItemStack().getItem() instanceof BlockItem&&Metalminds.bound(event.getItemStack()))event.setUseItem(TriState.FALSE);}
}
