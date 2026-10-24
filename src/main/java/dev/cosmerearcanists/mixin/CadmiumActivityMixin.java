package dev.cosmerearcanists.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.cosmerearcanists.feruchemy.CadmiumFeruchemy;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Limit endurance to native locomotion call sites; healing, damage, mining and arbitrary exhaustion remain unchanged. */
@Mixin(Player.class)
public abstract class CadmiumActivityMixin {
    @WrapOperation(method={"checkMovementStatistics","jumpFromGround"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void cosmerearcanists$oxygenEndurance(Player player,float amount,Operation<Void> original){original.call(player,CadmiumFeruchemy.activityExhaustion(player,amount));}
}
