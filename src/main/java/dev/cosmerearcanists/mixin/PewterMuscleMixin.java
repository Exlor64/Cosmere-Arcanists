package dev.cosmerearcanists.mixin;
import dev.cosmerearcanists.feruchemy.*;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Broaden the skin and outer clothing without changing camera, height or collision bounds. */
@Mixin(PlayerModel.class)
public abstract class PewterMuscleMixin {
    @Unique private float cosmerearcanists$previousWidth=1;
    @Unique private void cosmerearcanists$width(float multiplier){var model=(PlayerModel<?>)(Object)this;for(ModelPart part:new ModelPart[]{model.body,model.leftArm,model.rightArm,model.leftLeg,model.rightLeg,model.jacket,model.leftSleeve,model.rightSleeve,model.leftPants,model.rightPants}){part.xScale*=multiplier;part.zScale*=multiplier;}}
    @Inject(method="setupAnim",at=@At("HEAD"))
    private void cosmerearcanists$reset(LivingEntity entity,float limb,float amount,float age,float yaw,float pitch,CallbackInfo ci){cosmerearcanists$width(1/cosmerearcanists$previousWidth);cosmerearcanists$previousWidth=1;}
    @Inject(method="setupAnim",at=@At("TAIL"))
    private void cosmerearcanists$muscles(LivingEntity entity,float limb,float amount,float age,float yaw,float pitch,CallbackInfo ci){if(entity instanceof Player p&&FeruchemyConfig.PEWTER_MUSCLES.get()){float width=(float)Math.max(.7,Math.min(1.75,Math.pow(PewterFeruchemy.factor(p),.3)));cosmerearcanists$width(width);cosmerearcanists$previousWidth=width;}}
}
