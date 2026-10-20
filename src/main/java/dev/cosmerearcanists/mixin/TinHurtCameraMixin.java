package dev.cosmerearcanists.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.cosmerearcanists.feruchemy.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(GameRenderer.class)
public abstract class TinHurtCameraMixin {
    @ModifyExpressionValue(method="bobHurt",at=@At(value="INVOKE",target="Lnet/minecraft/util/Mth;sin(F)F"))
    private float cosmerearcanists$touchPain(float shake){var p=Minecraft.getInstance().player;return p==null||!FeruchemyConfig.TIN_TOUCH_SHAKE.get()?shake:(float)(shake*Math.min(1,TinFeruchemy.touch(p)));}
}
