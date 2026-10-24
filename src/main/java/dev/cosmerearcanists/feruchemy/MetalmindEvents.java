package dev.cosmerearcanists.feruchemy;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
public final class MetalmindEvents {
    @SubscribeEvent public void tooltip(ItemTooltipEvent event){var s=event.getItemStack();if(dev.cosmerearcanists.compounding.CompoundingService.nugget(s)){event.getToolTip().add(Component.literal("Hold right-click to eat · costs 1 heart (configurable)."));if(Metalminds.bound(s))event.getToolTip().add(Component.literal(String.format(Locale.ROOT,"Compatible compounding: refill reserves, +%.1f%% for %.2fs",100*dev.cosmerearcanists.compounding.CompoundingMath.boost(Metalminds.charge(s),Metalminds.capacity(s),dev.cosmerearcanists.compounding.CompoundingConfig.BOOST.get()),dev.cosmerearcanists.compounding.CompoundingConfig.SECONDS.get()*dev.cosmerearcanists.compounding.CompoundingMath.fullness(Metalminds.charge(s),Metalminds.capacity(s)))));}
        var source=Metalminds.source(s);if(source==null)return;
        if(Metalminds.bound(s)||s.getItem() instanceof MetalmindItem){event.getToolTip().add(Component.literal(!dev.cosmerearcanists.copper.CopperService.identity(s).equals(dev.cosmerearcanists.copper.CopperNetwork.ZERO)?"Coppermind notebook · store to write, tap to read":String.format(Locale.ROOT,"%s metalmind: %.2f / %.2f %s",source.metal(),Metalminds.charge(s),Metalminds.capacity(s),source.metal().equals("nicrosil")?"Investiture capacity":source.metal().equals("chromium")?"Fortune-seconds":source.metal().equals("duralumin")?"Connection-seconds":source.metal().equals("bendalloy")?"nutrition points":source.metal().equals("cadmium")?"breath-seconds":"attribute-seconds")));if(Metalminds.storedMetal(s).startsWith("tin:"))event.getToolTip().add(Component.literal("Stored sense: "+Metalminds.storedMetal(s).substring(4)));if(Metalminds.unkeyed(s))event.getToolTip().add(Component.literal(source.metal().equals("nicrosil")?"Unkeyed nicrosilmind · anyone can tap":"Unkeyed · requires matching Feruchemy"));var owner=Metalminds.owner(s);if(owner!=null)event.getToolTip().add(Component.literal("Bound to "+owner));event.getToolTip().add(Component.literal("Hold one item in either hand to store or tap."));}
        if(source.metal().equals("nicrosil")){event.getToolTip().add(Component.literal("Use nicrosil activation to open its menu; right-click non-nuggets also opens it."));event.getToolTip().add(Component.literal("Storing gradually moves power strength; tapping permanently adds it."));}
        if(Metalminds.bound(s))event.getToolTip().add(Component.literal("Bound metalmind: cannot be crafted, filed, processed, or placed."));
    }
    @SubscribeEvent public void place(PlayerInteractEvent.RightClickBlock event){if(event.getItemStack().getItem() instanceof BlockItem&&Metalminds.bound(event.getItemStack()))event.setUseItem(TriState.FALSE);}
}
