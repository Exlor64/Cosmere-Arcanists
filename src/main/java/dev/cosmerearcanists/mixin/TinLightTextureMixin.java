package dev.cosmerearcanists.mixin;

import org.spongepowered.asm.mixin.injection.ModifyArg;
import dev.cosmerearcanists.client.TinClient;
import dev.cosmerearcanists.power.TinMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Enhance only the 16x16 lightmap sampled by world shaders; never emit actual block light. */
@Mixin(LightTexture.class)
public abstract class TinLightTextureMixin {
    @ModifyArg(method="updateLightTexture",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/NativeImage;setPixelRGBA(III)V"),index=2)
    private int cosmerearcanists$tinLight(int color){
        var mc=Minecraft.getInstance();int enhanced=TinMath.liftLightmap(color,TinClient.nightVision(mc.getTimer().getGameTimeDeltaPartialTick(false)));double sight=mc.player==null?1:dev.cosmerearcanists.feruchemy.TinFeruchemy.factor(mc.player,true);return TinMath.dimLightmap(enhanced,sight);
    }
}
