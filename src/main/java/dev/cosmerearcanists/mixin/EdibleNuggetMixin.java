package dev.cosmerearcanists.mixin;
import dev.cosmerearcanists.compounding.CompoundingService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Covers vanilla iron/gold and ordinary mod nuggets without replacing their registry items. */
@Mixin(Item.class)
public abstract class EdibleNuggetMixin {
    @Inject(method="use",at=@At("HEAD"),cancellable=true)
    private void cosmerearcanists$eat(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir){var stack=player.getItemInHand(hand);if(!CompoundingService.nugget(stack))return;if(player instanceof ServerPlayer p&&!CompoundingService.canEat(p,stack)){cir.setReturnValue(InteractionResultHolder.fail(stack));return;}player.startUsingItem(hand);cir.setReturnValue(InteractionResultHolder.consume(stack));}
    @Inject(method="getUseDuration",at=@At("HEAD"),cancellable=true)
    private void cosmerearcanists$duration(ItemStack stack,LivingEntity user,CallbackInfoReturnable<Integer> cir){if(CompoundingService.nugget(stack))cir.setReturnValue(32);}
    @Inject(method="getUseAnimation",at=@At("HEAD"),cancellable=true)
    private void cosmerearcanists$animation(ItemStack stack,CallbackInfoReturnable<UseAnim> cir){if(CompoundingService.nugget(stack))cir.setReturnValue(UseAnim.EAT);}
    @Inject(method="finishUsingItem",at=@At("HEAD"),cancellable=true)
    private void cosmerearcanists$finish(ItemStack stack,Level level,LivingEntity user,CallbackInfoReturnable<ItemStack> cir){if(!CompoundingService.nugget(stack))return;if(user instanceof ServerPlayer p)CompoundingService.eat(p,stack);cir.setReturnValue(stack);}
}
