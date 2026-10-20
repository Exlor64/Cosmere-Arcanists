package dev.cosmerearcanists.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cosmerearcanists.power.BubbleWorldService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(FluidState.class)
public abstract class BubbleRandomFluidMixin {
    @WrapMethod(method="randomTick")
    private void cosmerearcanists$random(Level level,BlockPos pos,RandomSource random,Operation<Void> original){if(level instanceof ServerLevel server)BubbleWorldService.randomFluid(server,pos,random,()->original.call(level,pos,random));else original.call(level,pos,random);}
}
