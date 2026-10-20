package dev.cosmerearcanists.mixin;
import dev.cosmerearcanists.client.BubbleClient;
import dev.cosmerearcanists.power.BubbleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Entity.class)
public abstract class BubblePlayerMoveMixin {
    @ModifyVariable(method="move",at=@At("HEAD"),argsOnly=true)
    private Vec3 cosmerearcanists$localTime(Vec3 motion,MoverType type,Vec3 originalMotion){if((Object)this!=Minecraft.getInstance().player||type!=MoverType.SELF)return motion;double rate=BubbleClient.rate();if(Math.abs(rate-1)<1e-5)return motion;var scaled=motion.scale(rate);double max=BubbleConfig.MAX_PLAYER_STEP.get();return rate>1&&scaled.lengthSqr()>max*max?scaled.normalize().scale(max):scaled;}
}
