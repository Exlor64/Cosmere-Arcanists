package dev.cosmerearcanists.currency;
import dev.cosmerearcanists.ModMetals;
import net.minecraft.world.item.ItemStack;
public final class Coins {
    public static boolean coin(ItemStack s){return s.is(ModMetals.BOXING.get())||s.is(ModMetals.CLIP.get());}
    public static double units(ItemStack s){return (s.is(ModMetals.BOXING.get())?CurrencyConfig.BOXING_UNITS.get():CurrencyConfig.CLIP_UNITS.get())*s.getCount();}
    private Coins(){}
}
