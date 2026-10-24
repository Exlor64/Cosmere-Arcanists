package dev.cosmerearcanists.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.cosmerearcanists.feruchemy.ChromiumFeruchemy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve Luck of the Sea and other native luck contributions; adjust only the paid owner's fishing roll. */
@Mixin(FishingHook.class)
public abstract class FortuneFishingMixin {
    @WrapOperation(method="retrieve",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/storage/loot/LootParams$Builder;withLuck(F)Lnet/minecraft/world/level/storage/loot/LootParams$Builder;"))
    private LootParams.Builder cosmerearcanists$fortune(LootParams.Builder builder,float luck,Operation<LootParams.Builder> original){var owner=((FishingHook)(Object)this).getPlayerOwner();return original.call(builder,owner instanceof ServerPlayer p?luck+(float)ChromiumFeruchemy.fishingLuck(p):luck);}
}
