package dev.cosmerearcanists.mixin;
import dev.cosmerearcanists.power.BubbleService;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Entity.class)
public abstract class BubbleMoveMixin {
    @ModifyVariable(method="move",at=@At("HEAD"),argsOnly=true)
    private Vec3 cosmerearcanists$fraction(Vec3 motion,MoverType type,Vec3 originalMotion){return type==MoverType.SELF?motion.scale(BubbleService.movementScale()):motion;}
}
