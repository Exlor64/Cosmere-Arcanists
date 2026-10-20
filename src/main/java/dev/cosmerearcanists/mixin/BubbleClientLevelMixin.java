package dev.cosmerearcanists.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cosmerearcanists.power.BubbleService;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
/** Match predicted dropped-item and projectile substeps to the server; ordinary remote-mob interpolation stays untouched. */
@Mixin(ClientLevel.class)
public abstract class BubbleClientLevelMixin {
    @WrapMethod(method="tickNonPassenger")
    private void cosmerearcanists$predictedEntityTime(Entity entity,Operation<Void> original){if(BubbleService.predictsLocally(entity))BubbleService.tickEntity(entity,()->original.call(entity));else original.call(entity);}
}
