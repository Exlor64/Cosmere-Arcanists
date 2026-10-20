package dev.cosmerearcanists.mixin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Projectile.class)
public interface BubbleProjectileAccess {
    @Invoker("canHitEntity") boolean cosmerearcanists$canHit(Entity entity);
    @Invoker("onHit") void cosmerearcanists$onHit(HitResult hit);
}
