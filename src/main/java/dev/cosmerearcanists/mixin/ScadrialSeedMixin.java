package dev.cosmerearcanists.mixin;
import dev.cosmerearcanists.ScadrialWorld;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Noise/biome/structure state construction reads ServerLevel's dimension-specific seed. */
@Mixin(ServerLevel.class)
public abstract class ScadrialSeedMixin {
    @Inject(method="getSeed",at=@At("RETURN"),cancellable=true)
    private void cosmerearcanists$scadrialSeed(CallbackInfoReturnable<Long> result){var level=(ServerLevel)(Object)this;if(level.dimension().location().toString().equals(ScadrialWorld.ID))result.setReturnValue(ScadrialWorld.seed(result.getReturnValue()));}
}
