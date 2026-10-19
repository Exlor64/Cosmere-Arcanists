package dev.cosmerearcanists.filing;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class FilingEvents {
    @SubscribeEvent public void tooltip(ItemTooltipEvent event) {
        if(Filings.filed(event.getItemStack())) {
            event.getToolTip().add(Component.literal("Filings remaining: "+Filings.remaining(event.getItemStack())+" / "+Filings.capacity(event.getItemStack())));
            event.getToolTip().add(Component.literal("Partially filed: cannot be crafted, processed, or placed."));
        }
    }
    @SubscribeEvent public void useBlock(PlayerInteractEvent.RightClickBlock event) {
        // Placing and rebreaking a filed storage block must never regenerate a full block of metal.
        if(Filings.filed(event.getItemStack())){event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);}
    }
}
