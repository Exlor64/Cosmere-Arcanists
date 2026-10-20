package dev.cosmerearcanists.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cosmerearcanists.power.BubbleService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ServerLevel.class)
public abstract class BubbleLevelMixin {
    @WrapMethod(method="tick")
    private void cosmerearcanists$world(java.util.function.BooleanSupplier hasTime,Operation<Void> original){dev.cosmerearcanists.power.BubbleWorldService.levelTick((ServerLevel)(Object)this,()->original.call(hasTime));}
    @WrapMethod(method="tickNonPassenger")
    private void cosmerearcanists$time(Entity entity,Operation<Void> original){BubbleService.tickEntity(entity,()->original.call(entity));}
}
