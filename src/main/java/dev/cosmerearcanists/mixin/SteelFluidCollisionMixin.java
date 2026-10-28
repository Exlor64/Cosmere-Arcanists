package dev.cosmerearcanists.mixin;
import dev.cosmerearcanists.feruchemy.SteelFluidRunning;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LiquidBlock.class)
public abstract class SteelFluidCollisionMixin {
    @Inject(method="getCollisionShape",at=@At("HEAD"),cancellable=true)
    private void cosmerearcanists$runOnFluid(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context,CallbackInfoReturnable<VoxelShape> ci){
        var shape=SteelFluidRunning.surface(level,pos,state.getFluidState(),context);if(!shape.isEmpty())ci.setReturnValue(shape);
    }
}
