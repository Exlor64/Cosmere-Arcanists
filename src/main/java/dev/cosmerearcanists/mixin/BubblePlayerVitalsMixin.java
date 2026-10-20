package dev.cosmerearcanists.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cosmerearcanists.power.BubbleService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(LivingEntity.class)
public abstract class BubblePlayerVitalsMixin {
    @WrapMethod(method="baseTick")
    private void cosmerearcanists$vitals(Operation<Void> original){if((Object)this instanceof Player p)BubbleService.playerTimer(p,"vitals",()->original.call());else original.call();}
}
