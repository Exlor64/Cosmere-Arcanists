package dev.cosmerearcanists.mixin;
import dev.cosmerearcanists.feruchemy.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class FeruchemyMotionMixin {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;setTicksFrozen(I)V"))
    private void cosmerearcanists$storedWarmthStopsThaw(LivingEntity entity,int ticks,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original){
        if(entity instanceof net.minecraft.server.level.ServerPlayer p&&ticks<p.getTicksFrozen()&&p.canFreeze()&&BrassFeruchemy.storingCold(p))return;
        original.call(entity,ticks);
    }

    @Inject(method="canFreeze",at=@At("HEAD"),cancellable=true)
    private void cosmerearcanists$warmthPreventsFreezing(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci){if((Object)this instanceof net.minecraft.server.level.ServerPlayer p&&BrassFeruchemy.warm(p))ci.setReturnValue(false);}

    @ModifyVariable(method="knockback",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private double cosmerearcanists$massKnockback(double force){return force*IronFeruchemy.knockbackMultiplier((LivingEntity)(Object)this)*ElectrumFeruchemy.knockbackMultiplier((LivingEntity)(Object)this);}
    @Inject(method="travel",at=@At("TAIL"))
    private void cosmerearcanists$lightweightDrag(Vec3 input,CallbackInfo ci){var e=(LivingEntity)(Object)this;double mass=IronFeruchemy.massMultiplier(e);
        if(!FeruchemyConfig.DRAG.get()||mass>=1||e.isNoGravity()||e.isFallFlying()||e.isInWater()||e.isInLava())return;
        var velocity=e.getDeltaMovement();double terminal=Math.max(.12,FeruchemyConfig.TERMINAL.get()*mass);if(velocity.y < -terminal)e.setDeltaMovement(velocity.x,-terminal,velocity.z);
    }
}
