package dev.cosmerearcanists.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cosmerearcanists.power.BubbleWorldService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BubbleRandomBlockMixin {
    @WrapMethod(method="randomTick")
    private void cosmerearcanists$random(ServerLevel level,BlockPos pos,RandomSource random,Operation<Void> original){BubbleWorldService.randomBlock(level,pos,random,()->original.call(level,pos,random));}
}
