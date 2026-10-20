package dev.cosmerearcanists.currency;

import dev.cosmerearcanists.ModMetals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;

/** Convert only a Scadrian villager's emerald legs; retain full offer save data. */
public final class CurrencyTrades {
    public static final String SCADRIAN="cosmerearcanists.scadrial";
    public static boolean scadrian(Villager v){return v.getTags().contains(SCADRIAN)||CurrencyConfig.ALL_VILLAGERS.get()||CurrencyConfig.DIMENSIONS.get().contains(v.level().dimension().location().toString());}
    private static CompoundTag cost(Item item,int count){var t=new CompoundTag();t.putString("id",BuiltInRegistries.ITEM.getKey(item).toString());t.putInt("count",count);return t;}
    private static int count(CompoundTag t){return t.contains("count")?Math.max(1,t.getInt("count")):1;}
    private static boolean emerald(CompoundTag t){return t.getString("id").equals("minecraft:emerald");}
    private static CompoundTag oneCost(int value){if(value<=64)return cost(ModMetals.CLIP.get(),value);int boxes=(value+CurrencyConfig.CLIPS_PER_BOXING.get()-1)/CurrencyConfig.CLIPS_PER_BOXING.get();return boxes<=64?cost(ModMetals.BOXING.get(),boxes):null;}
    public static MerchantOffer convert(Villager v,MerchantOffer original){
        if(!original.getBaseCostA().is(Items.EMERALD)&&!original.getCostB().is(Items.EMERALD)&&!original.getResult().is(Items.EMERALD))return original;
        var ops=v.registryAccess().createSerializationContext(NbtOps.INSTANCE);var tag=(CompoundTag)MerchantOffer.CODEC.encodeStart(ops,original).getOrThrow();tag=tag.copy();CompoundTag a=tag.getCompound("buy"),b=tag.getCompound("buyB"),sell=tag.getCompound("sell");
        if(emerald(a)){
            int value=count(a)*CurrencyConfig.CLIPS_PER_EMERALD.get();int ratio=CurrencyConfig.CLIPS_PER_BOXING.get();CompoundTag newA,newB=null;
            if(b.isEmpty()&&value>64&&value<=128&&value<ratio){newA=cost(ModMetals.CLIP.get(),64);newB=cost(ModMetals.CLIP.get(),value-64);}
            else if(b.isEmpty()&&value>=ratio&&value%ratio>0&&value%ratio<=64&&value/ratio<=64){newA=cost(ModMetals.BOXING.get(),value/ratio);newB=cost(ModMetals.CLIP.get(),value%ratio);}
            else newA=oneCost(value);
            if(newA==null)return original;tag.put("buy",newA);if(newB!=null)tag.put("buyB",newB);
        }
        if(emerald(b)){var replacement=oneCost(count(b)*CurrencyConfig.CLIPS_PER_EMERALD.get());if(replacement==null)return original;tag.put("buyB",replacement);}
        if(emerald(sell)){
            int value=count(sell)*CurrencyConfig.CLIPS_PER_EMERALD.get(),boxes=value/CurrencyConfig.CLIPS_PER_BOXING.get();if(boxes>64)return original;
            tag.put("sell",boxes>0?cost(ModMetals.BOXING.get(),boxes):cost(ModMetals.CLIP.get(),Math.min(64,value)));
        }
        return MerchantOffer.CODEC.parse(ops,tag).getOrThrow();
    }
    public static boolean exchange(MerchantOffer offer){return offer.getBaseCostA().is(ModMetals.CLIP.get())&&(offer.getCostB().isEmpty()||offer.getCostB().is(ModMetals.CLIP.get()))&&offer.getResult().is(ModMetals.BOXING.get());}
    public static void resetExchangePrices(MerchantOffers offers){for(var offer:offers)if(exchange(offer))offer.resetSpecialPriceDiff();}
    public static void prepare(Villager v,MerchantOffers offers){
        if(v.level().isClientSide||!CurrencyConfig.TRADES.get()||!scadrian(v))return;
        v.addTag(SCADRIAN);for(int i=0;i<offers.size();i++)offers.set(i,convert(v,offers.get(i)));
        if(CurrencyConfig.EXCHANGE.get()&&offers.stream().noneMatch(CurrencyTrades::exchange)){
            int clips=CurrencyConfig.CLIPS_PER_BOXING.get();var offer=new MerchantOffer(new ItemCost(ModMetals.CLIP.get(),Math.min(64,clips)),new ItemStack(ModMetals.BOXING.get()),1000000,0,0f);
            var ops=v.registryAccess().createSerializationContext(NbtOps.INSTANCE);var tag=((CompoundTag)MerchantOffer.CODEC.encodeStart(ops,offer).getOrThrow()).copy();if(clips>64)tag.put("buyB",cost(ModMetals.CLIP.get(),clips-64));tag.putBoolean("rewardExp",false);offers.add(MerchantOffer.CODEC.parse(ops,tag).getOrThrow());
        }resetExchangePrices(offers);
    }
    private CurrencyTrades(){}
}
