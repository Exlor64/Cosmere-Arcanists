package dev.cosmerearcanists.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cosmerearcanists.feruchemy.SteelFluidRunning;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Entity.class)
public abstract class SteelFluidMovementMixin {
    @WrapMethod(method="move")
    private void cosmerearcanists$fluidRunningMovement(MoverType type,Vec3 motion,Operation<Void> original){
        if((Object)this instanceof Player p&&type==MoverType.SELF)SteelFluidRunning.move(p,motion,()->original.call(type,motion));
        else original.call(type,motion);
    }
}
