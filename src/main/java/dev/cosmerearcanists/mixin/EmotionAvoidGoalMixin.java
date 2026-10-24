package dev.cosmerearcanists.mixin;

import dev.cosmerearcanists.power.EmotionService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvoidEntityGoal.class)
public abstract class EmotionAvoidGoalMixin {
    @Shadow @org.spongepowered.asm.mixin.Final protected PathfinderMob mob;
    @Shadow protected LivingEntity toAvoid;
    @Shadow @org.spongepowered.asm.mixin.Final protected float maxDist;
    @Inject(method="canUse",at=@At("RETURN"),cancellable=true)
    private void cosmerearcanists$trust(CallbackInfoReturnable<Boolean> cir){if(cir.getReturnValue() && (EmotionService.trusts(mob,toAvoid)||dev.cosmerearcanists.connection.ConnectionService.ignoreAvoidance(mob,toAvoid,maxDist)))cir.setReturnValue(false);}
    @Inject(method="canContinueToUse",at=@At("HEAD"),cancellable=true)
    private void cosmerearcanists$continue(CallbackInfoReturnable<Boolean> cir){if((EmotionService.trusts(mob,toAvoid)||dev.cosmerearcanists.connection.ConnectionService.ignoreAvoidance(mob,toAvoid,maxDist))){mob.getNavigation().stop();cir.setReturnValue(false);}}
}
