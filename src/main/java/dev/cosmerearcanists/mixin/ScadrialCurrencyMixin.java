package dev.cosmerearcanists.mixin;
import dev.cosmerearcanists.currency.CurrencyTrades;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractVillager.class)
public abstract class ScadrialCurrencyMixin {
    @Inject(method="getOffers",at=@At("RETURN"))
    private void cosmerearcanists$currency(CallbackInfoReturnable<MerchantOffers> ci){if((Object)this instanceof Villager villager)CurrencyTrades.prepare(villager,ci.getReturnValue());}
}
