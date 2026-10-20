package dev.cosmerearcanists.mixin;
import java.util.function.BiConsumer;
import it.unimi.dsi.fastutil.longs.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cosmerearcanists.power.BubbleWorldService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.*;
import org.spongepowered.asm.mixin.*;
@Mixin(LevelTicks.class)
public abstract class BubbleScheduledMixin<T> {
    @Shadow @Final private Long2ObjectMap<LevelChunkTicks<T>> allContainers;
    @Shadow @Final private Long2LongMap nextTickForContainer;
    @WrapMethod(method="schedule")
    private void cosmerearcanists$schedule(ScheduledTick<T> tick,Operation<Void> original){original.call(tick);BubbleWorldService.scheduled(tick);}
    @WrapMethod(method="tick")
    private void cosmerearcanists$localTime(long time,int maximum,BiConsumer<BlockPos,T> consumer,Operation<Void> original){BubbleWorldService.scheduledFrame(allContainers,nextTickForContainer,time,maximum,(limit)->original.call(time,limit,consumer));}
}
