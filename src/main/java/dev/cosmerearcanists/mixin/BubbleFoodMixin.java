package dev.cosmerearcanists.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cosmerearcanists.power.BubbleService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(FoodData.class)
public abstract class BubbleFoodMixin {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="tick",at=@org.spongepowered.asm.mixin.injection.At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void cosmerearcanists$oxygenNaturalHealing(Player p,float amount,Operation<Void> original){original.call(p,dev.cosmerearcanists.feruchemy.CadmiumFeruchemy.naturalHealing(p,amount));}
    @WrapMethod(method="tick")
    private void cosmerearcanists$food(Player p,Operation<Void> original){BubbleService.playerTimer(p,"food",()->original.call(p));}
}
