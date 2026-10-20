package dev.cosmerearcanists.mixin;
import dev.cosmerearcanists.client.FeruchemicalTinClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
@Mixin(LivingEntityRenderer.class)
public abstract class TinInvisibleRendererMixin {
    @Unique private LivingEntity cosmerearcanists$sightEntity;
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("HEAD"))
    private void cosmerearcanists$track(LivingEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){cosmerearcanists$sightEntity=entity;}
    @ModifyExpressionValue(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean cosmerearcanists$localVisibility(boolean invisible){return invisible&&(cosmerearcanists$sightEntity==null||FeruchemicalTinClient.invisibleOpacity(cosmerearcanists$sightEntity)<=0);}
    @ModifyArg(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),index=4)
    private int cosmerearcanists$clarity(int color){double alpha=cosmerearcanists$sightEntity==null?0:FeruchemicalTinClient.invisibleOpacity(cosmerearcanists$sightEntity);return alpha>0?(color&0xffffff)|((int)Math.round(alpha*255)<<24):color;}
}
