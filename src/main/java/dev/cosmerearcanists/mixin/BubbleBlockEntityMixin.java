package dev.cosmerearcanists.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cosmerearcanists.power.BubbleService;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
@Mixin(targets="net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BubbleBlockEntityMixin {
    @Shadow @Final private BlockEntity blockEntity;
    @WrapMethod(method="tick")
    private void cosmerearcanists$time(Operation<Void> original){BubbleService.blockTick(blockEntity,()->original.call());}
}
