package dev.cosmerearcanists.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Shorten the existing instance; native ticking retains amplifier, hidden effects and expiration hooks. */
@Mixin(MobEffectInstance.class)
public interface BronzeEffectAccess {
    @Accessor("duration") void cosmerearcanists$duration(int ticks);
    @Accessor("hiddenEffect") MobEffectInstance cosmerearcanists$hiddenEffect();
}
