package dev.cosmerearcanists.mixin;

import dev.cosmerearcanists.filing.Filings;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Simple shapeless matching uses item IDs instead of Ingredient.test; guard that optimized path too. */
@Mixin(ShapelessRecipe.class)
public abstract class FiledShapelessMixin {
    @Inject(method="matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",at=@At("HEAD"),cancellable=true)
    private void cosmerearcanists$excludeFiled(CraftingInput input,Level level,CallbackInfoReturnable<Boolean> cir) {
        if(Filings.hasFiled(input))cir.setReturnValue(false);
    }
}
