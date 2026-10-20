package dev.cosmerearcanists.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.cosmerearcanists.feruchemy.TinFeruchemy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(AbstractSoundInstance.class)
public abstract class TinSoundMixin {
    @ModifyReturnValue(method="getVolume",at=@At("RETURN"))
    private float cosmerearcanists$hearing(float volume){var p=Minecraft.getInstance().player;var sound=(SoundInstance)(Object)this;var category=sound.getSource();return p==null||sound.isRelative()||category==SoundSource.MUSIC||category==SoundSource.RECORDS||category==SoundSource.MASTER?volume:(float)(volume*TinFeruchemy.factor(p,false));}
}
