package dev.cosmerearcanists.mixin;

import dev.cosmerearcanists.filing.Filings;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Ingredient.class)
public abstract class FiledIngredientMixin {
    @Inject(method="test(Lnet/minecraft/world/item/ItemStack;)Z",at=@At("HEAD"),cancellable=true)
    private void cosmerearcanists$excludeFiled(ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        if(stack!=null && Filings.protectedMaterial(stack))cir.setReturnValue(false);
    }
}
