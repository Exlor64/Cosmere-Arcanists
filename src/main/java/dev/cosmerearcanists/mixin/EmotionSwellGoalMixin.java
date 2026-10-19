package dev.cosmerearcanists.mixin;

import dev.cosmerearcanists.power.EmotionService;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Losing anger stops autonomous swelling; an already manually ignited creeper stays ignited. */
@Mixin(SwellGoal.class)
public abstract class EmotionSwellGoalMixin {
    @Shadow @Final private Creeper creeper;
    @Inject(method="canUse",at=@At("HEAD"),cancellable=true)
    private void cosmerearcanists$soothed(CallbackInfoReturnable<Boolean> cir){if(!creeper.isIgnited() && EmotionService.pacified(creeper)){creeper.setSwellDir(-1);cir.setReturnValue(false);}}
}
