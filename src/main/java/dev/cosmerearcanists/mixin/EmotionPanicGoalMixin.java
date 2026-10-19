package dev.cosmerearcanists.mixin;

import dev.cosmerearcanists.power.EmotionService;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PanicGoal.class)
public abstract class EmotionPanicGoalMixin {
    @Shadow @org.spongepowered.asm.mixin.Final protected PathfinderMob mob;
    @Inject(method={"canUse","canContinueToUse"},at=@At("HEAD"),cancellable=true)
    private void cosmerearcanists$calm(CallbackInfoReturnable<Boolean> cir){if(EmotionService.calmPanic(mob)){mob.getNavigation().stop();cir.setReturnValue(false);}}
}
