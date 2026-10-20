package dev.cosmerearcanists.mixin;
import dev.cosmerearcanists.feruchemy.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class FeruchemyMotionMixin {
    @ModifyVariable(method="knockback",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private double cosmerearcanists$massKnockback(double force){return force*IronFeruchemy.knockbackMultiplier((LivingEntity)(Object)this);}
    @Inject(method="travel",at=@At("TAIL"))
    private void cosmerearcanists$lightweightDrag(Vec3 input,CallbackInfo ci){var e=(LivingEntity)(Object)this;double mass=IronFeruchemy.massMultiplier(e);
        if(!FeruchemyConfig.DRAG.get()||mass>=1||e.isNoGravity()||e.isFallFlying()||e.isInWater()||e.isInLava())return;
        var velocity=e.getDeltaMovement();double terminal=Math.max(.12,FeruchemyConfig.TERMINAL.get()*mass);if(velocity.y < -terminal)e.setDeltaMovement(velocity.x,-terminal,velocity.z);
    }
}
