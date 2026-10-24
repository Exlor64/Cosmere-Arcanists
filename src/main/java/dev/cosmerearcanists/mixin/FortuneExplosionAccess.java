package dev.cosmerearcanists.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read the actual source, power and center before a TNT explosion mutates the world. */
@Mixin(Explosion.class)
public interface FortuneExplosionAccess {
    @Accessor("source") Entity cosmerearcanists$source();
    @Accessor("radius") float cosmerearcanists$radius();
    @Accessor("x") double cosmerearcanists$x();
    @Accessor("y") double cosmerearcanists$y();
    @Accessor("z") double cosmerearcanists$z();
}
