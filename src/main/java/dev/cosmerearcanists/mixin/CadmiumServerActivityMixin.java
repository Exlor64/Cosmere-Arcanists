package dev.cosmerearcanists.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.cosmerearcanists.feruchemy.CadmiumFeruchemy;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Native movement-stat exhaustion is implemented on ServerPlayer, not Player. */
@Mixin(ServerPlayer.class)
public abstract class CadmiumServerActivityMixin {
    @WrapOperation(method="checkMovementStatistics",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
    private void cosmerearcanists$oxygenMovement(ServerPlayer player,float amount,Operation<Void> original){original.call(player,CadmiumFeruchemy.activityExhaustion(player,amount));}
}
