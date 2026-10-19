package dev.cosmerearcanists.mixin;

import dev.cosmerearcanists.power.PewterService;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Intercept exhaustion before vanilla can spend saturation/food, including threshold-crossing ticks. */
@Mixin(Player.class)
public abstract class PlayerExhaustionMixin {
    @ModifyVariable(method="causeFoodExhaustion",at=@At("HEAD"),argsOnly=true)
    private float cosmerearcanists$pewterEndurance(float amount) {
        return PewterService.exhaustion((Player)(Object)this,amount);
    }
}
