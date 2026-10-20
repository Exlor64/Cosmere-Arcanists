package dev.cosmerearcanists.mixin;
import java.util.*;
import net.minecraft.world.ticks.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(LevelChunkTicks.class)
public interface BubbleChunkTicksAccess<T> {
    @Accessor("tickQueue") Queue<ScheduledTick<T>> cosmerearcanists$queue();
    @Accessor("ticksPerPosition") Set<ScheduledTick<?>> cosmerearcanists$positions();
}
