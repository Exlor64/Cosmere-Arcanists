package dev.cosmerearcanists.mixin;

import dev.cosmerearcanists.client.PowerClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Choose a single-item drop before vanilla predicts inventory removal and selects its server action packet. */
@Mixin(LocalPlayer.class)
public abstract class AlternateDropMixin {
    @ModifyVariable(method="drop(Z)Z",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private boolean cosmerearcanists$singleAlternateThrow(boolean entireStack){return entireStack&&!PowerClient.ALTERNATE.isDown();}
}
