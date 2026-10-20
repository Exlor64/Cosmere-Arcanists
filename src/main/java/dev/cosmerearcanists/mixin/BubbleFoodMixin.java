package dev.cosmerearcanists.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cosmerearcanists.power.BubbleService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(FoodData.class)
public abstract class BubbleFoodMixin {
    @WrapMethod(method="tick")
    private void cosmerearcanists$food(Player p,Operation<Void> original){BubbleService.playerTimer(p,"food",()->original.call(p));}
}
