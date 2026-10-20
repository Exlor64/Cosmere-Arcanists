package dev.cosmerearcanists.mixin;

import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read requested speed without ticking navigation or mutating its route. */
@Mixin(PathNavigation.class)
public interface NavigationAccessor {
    @Accessor("speedModifier") double cosmerearcanists$speedModifier();
}
