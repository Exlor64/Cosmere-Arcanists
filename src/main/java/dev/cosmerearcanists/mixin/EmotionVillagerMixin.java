package dev.cosmerearcanists.mixin;

import dev.cosmerearcanists.power.EmotionService;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Session-only special prices are reset by vanilla when trading ends, without changing gossip or base costs. */
@Mixin(Villager.class)
public abstract class EmotionVillagerMixin {
    @Inject(method="updateSpecialPrices",at=@At("TAIL"))
    private void cosmerearcanists$trustPrices(Player player,CallbackInfo ci){EmotionService.discount((Villager)(Object)this,player);}
}
