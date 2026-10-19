package dev.cosmerearcanists.mixin;

import java.util.Optional;
import dev.cosmerearcanists.filing.Filings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Also guard recipe-manager/cached-machine paths against repairing or recycling filed material. */
@Mixin(RecipeManager.class)
public abstract class FiledRecipeManagerMixin {
    @Inject(method="getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",at=@At("HEAD"),cancellable=true)
    private void cosmerearcanists$excludeFiled(RecipeType<?> type,RecipeInput input,Level level,CallbackInfoReturnable<Optional<?>> cir) {
        if(Filings.hasFiled(input))cir.setReturnValue(Optional.empty());
    }
    @Inject(method="getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;",at=@At("HEAD"),cancellable=true)
    private void cosmerearcanists$excludeFiledCached(RecipeType<?> type,RecipeInput input,Level level,ResourceLocation lastRecipe,CallbackInfoReturnable<Optional<?>> cir) {
        if(Filings.hasFiled(input))cir.setReturnValue(Optional.empty());
    }
}
