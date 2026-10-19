package dev.cosmerearcanists.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cosmerearcanists.power.TinService;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/** Associate position-based entity sounds with their actual source, preserving cancellation and silence. */
@Mixin(Entity.class)
public abstract class EntitySoundSourceMixin {
    @WrapMethod(method="playSound(Lnet/minecraft/sounds/SoundEvent;FF)V")
    private void cosmerearcanists$soundSource(SoundEvent sound,float volume,float pitch,Operation<Void> original) {
        var entity=(Entity)(Object)this;
        if(entity.level().isClientSide)original.call(sound,volume,pitch);
        else TinService.withSoundSource(entity,()->original.call(sound,volume,pitch));
    }
}
