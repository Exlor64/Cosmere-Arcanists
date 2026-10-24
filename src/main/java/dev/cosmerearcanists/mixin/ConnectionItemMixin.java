package dev.cosmerearcanists.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cosmerearcanists.connection.ConnectionService;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** Record sharing only after a real vanilla pickup reduces the dropped stack. */
@Mixin(ItemEntity.class)
public abstract class ConnectionItemMixin {
    @WrapMethod(method="playerTouch")
    private void cosmerearcanists$sharedItem(Player player,Operation<Void> original){var item=(ItemEntity)(Object)this;int before=item.getItem().getCount();original.call(player);ConnectionService.itemShared(item,player,before-item.getItem().getCount());}
}
