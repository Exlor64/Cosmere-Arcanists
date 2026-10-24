package dev.cosmerearcanists.connection;

import dev.cosmerearcanists.ModMetals;
import dev.cosmerearcanists.currency.CurrencyTrades;
import dev.cosmerearcanists.feruchemy.*;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.MerchantOffer;

/** Independent, reversible session adjustments. Refreshes only our delta, retaining vanilla and emotional prices. */
public final class ConnectionTrades {
    private record Session(ServerPlayer player,Map<MerchantOffer,Integer> adjustments){}
    private static final Map<Villager,Session> SESSIONS=new WeakHashMap<>();
    private static boolean currency(ItemStack stack){return stack.is(Items.EMERALD)||stack.is(ModMetals.CLIP.get())||stack.is(ModMetals.BOXING.get());}
    public static boolean exchange(MerchantOffer offer){return CurrencyTrades.exchange(offer)||currency(offer.getBaseCostA())&&currency(offer.getResult())&&(offer.getCostB().isEmpty()||currency(offer.getCostB()));}
    public static double fraction(ServerPlayer p){if(!ConnectionConfig.TRADES.get())return 0;double levels=DuraluminFeruchemy.fundedLevels(p);return levels>0?-Math.min(FeruchemicalEnhancement.fractionCap(p,"duralumin",ConnectionConfig.DISCOUNT_CAP.get()),levels*ConnectionConfig.DISCOUNT_STEP.get()):DuraluminFeruchemy.storedFraction(p)*ConnectionConfig.STORE_PRICE.get();}
    public static void beforePricing(Villager v){var old=SESSIONS.remove(v);if(old!=null)old.adjustments().forEach((offer,amount)->offer.addToSpecialPriceDiff(-amount));}
    public static void end(Villager v){SESSIONS.remove(v);}
    public static void begin(Villager v,Player p){if(!(p instanceof ServerPlayer server))return;beforePricing(v);var session=new Session(server,new IdentityHashMap<>());SESSIONS.put(v,session);apply(v,session);}
    private static boolean apply(Villager v,Session s){boolean changed=false;double fraction=fraction(s.player());for(var offer:v.getOffers()){int before=s.adjustments().getOrDefault(offer,0);int next=exchange(offer)?0:ConnectionMath.priceAdjustment(offer.getBaseCostA().getCount(),fraction);if(next!=before){offer.addToSpecialPriceDiff(next-before);changed=true;}s.adjustments().put(offer,next);}return changed;}
    public static void tick(){for(var entry:List.copyOf(SESSIONS.entrySet())){var v=entry.getKey();var s=entry.getValue();if(v.isRemoved()||!v.isAlive()||v.getTradingPlayer()!=s.player()||!s.player().isAlive()){beforePricing(v);continue;}if(apply(v,s))s.player().sendMerchantOffers(s.player().containerMenu.containerId,v.getOffers(),v.getVillagerData().getLevel(),v.getVillagerXp(),true,v.canRestock());}}
    public static void clear(){for(var v:List.copyOf(SESSIONS.keySet()))beforePricing(v);SESSIONS.clear();}
    private ConnectionTrades(){}
}
