package dev.cosmerearcanists.feruchemy;
import java.util.*;
import dev.cosmerearcanists.filing.Filings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
/** Item-owned charge. A physical singleton is required at every read/write boundary. */
public final class Metalminds {
    private static final String KEY="cosmerearcanists_metalmind";
    private static CompoundTag data(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static boolean bound(ItemStack s){return !s.isEmpty()&&data(s).contains(KEY)&&charge(s)>0;}
    public static Filings.Source source(ItemStack s){
        if(s.isEmpty()||Filings.filed(s))return null;
        if(s.getItem() instanceof MetalmindItem band)return new Filings.Source(band.metal(),band.units());
        return FeruchemyConfig.DENOMINATIONS.get()?Filings.source(s):null;
    }
    public static UUID owner(ItemStack s){if(!bound(s))return null;var t=data(s).getCompound(KEY);return t.hasUUID("owner")?t.getUUID("owner"):null;}
    public static double charge(ItemStack s){double value=data(s).getCompound(KEY).getDouble("charge");return Double.isFinite(value)?Math.max(0,value):0;}
    public static double capacity(ItemStack s){var t=data(s).getCompound(KEY);double value=t.getDouble("capacity");var source=source(s);return t.contains("capacity")?Double.isFinite(value)?Math.max(0,value):0:source==null?0:source.nuggetUnits()*FeruchemyConfig.CAPACITY.get();}
    public static boolean unkeyed(ItemStack s){var tag=data(s).getCompound(KEY);return bound(s)&&tag.getBoolean("unkeyed")&&!tag.hasUUID("owner");}
    public static String storedMetal(ItemStack s){return bound(s)?data(s).getCompound(KEY).getString("metal"):"";}
    private static String physical(String metal){return metal.equals("tin:sight")||metal.equals("tin:hearing")||metal.equals("tin:touch")?"tin":metal;}
    public static boolean usable(ItemStack s,UUID user,String metal){
        var source=source(s);if(s.getCount()!=1||source==null||!source.metal().equals(physical(metal)))return false;
        if(!bound(s))return true;var root=data(s).getCompound(KEY);
        return root.getString("metal").equals(metal)&&(unkeyed(s)||owner(s)!=null&&(!FeruchemyConfig.OWNERSHIP.get()||Objects.equals(user,owner(s))));
    }
    /** Player-aware authorization honors active implanted donor Identities without unkeying/rebinding a mind. */
    private static UUID bindingUser(ItemStack s,ServerPlayer p){var owner=owner(s);return owner!=null&&dev.cosmerearcanists.hemalurgy.IdentityAccess.accepts(p,owner)?owner:dev.cosmerearcanists.hemalurgy.IdentityAccess.nativeIdentity(p);}
    public static boolean usableBy(ItemStack s,ServerPlayer p,String metal){return !dev.cosmerearcanists.hemalurgy.AluminumSuppression.active(p)&&usable(s,bindingUser(s,p),metal);}
    public static boolean canStoreFor(ItemStack s,ServerPlayer p,String metal,boolean neutral){return usableBy(s,p,metal)&&(!bound(s)||unkeyed(s)==neutral);}
    public static double transferFor(ItemStack s,ServerPlayer p,String metal,double amount){return transferWithPlayerBinding(s,p,metal,amount,false);}
    public static double transferWithPlayerBinding(ItemStack s,ServerPlayer p,String metal,double amount,boolean neutral){return usableBy(s,p,metal)?transferWithBinding(s,bindingUser(s,p),metal,amount,neutral):0;}
    public static boolean unkeyedForStorage(ServerPlayer p,String metal){return !metal.equals("aluminum")&&AluminumFeruchemy.unkeyedStorage(p);}
    public static boolean canStore(ItemStack s,UUID user,String metal,boolean unkeyed){return usable(s,user,metal)&&(!bound(s)||unkeyed(s)==unkeyed);}
    public static boolean writable(ServerPlayer p,ItemStack s,String metal){return canStoreFor(s,p,metal,unkeyedForStorage(p,metal));}
    public static double transferForStorage(ServerPlayer p,ItemStack s,String metal,double amount){return transferWithPlayerBinding(s,p,metal,amount,unkeyedForStorage(p,metal));}
    /** Positive stores, negative taps. Returns signed amount actually transferred. Never splits a stack. */
    public static double transfer(ItemStack s,UUID user,String metal,double amount){return transferWithBinding(s,user,metal,amount,false);}
    /** Explicit binding is for authoritative storage/refund integrations, never a client-selected mode. */
    public static double transferWithBinding(ItemStack s,UUID user,String metal,double amount,boolean neutral){
        if(dev.cosmerearcanists.nicrosil.NicrosilService.specialized(s))return 0; // Permanent powers are consumed only by explicit nicrosil transfers or wiping.
        if(!Double.isFinite(amount)||amount==0||!usable(s,user,metal)||amount>0&&!canStore(s,user,metal,neutral))return 0;
        double before=Math.min(charge(s),capacity(s)),after=Math.clamp(before+amount,0,capacity(s));
        if(after==before){if(after==0)dev.cosmerearcanists.copper.CopperService.eraseItem(s);return 0;}
        var root=data(s);var tag=root.getCompound(KEY);if(!bound(s)){if(neutral){tag.remove("owner");tag.putBoolean("unkeyed",true);}else{if(user==null)return 0;tag.putUUID("owner",user);tag.remove("unkeyed");}tag.putString("metal",metal);tag.putDouble("capacity",capacity(s));}
        tag.putDouble("charge",after);if(after==0){tag.remove("owner");tag.remove("metal");tag.remove("unkeyed");}root.put(KEY,tag);s.set(DataComponents.MAX_STACK_SIZE,1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(root));if(after==0)dev.cosmerearcanists.copper.CopperService.eraseItem(s);return after-before;
    }
    /** Remove obsolete bindings from previously saved empty pieces without changing capacity or stack size. */
    public static boolean unkeyEmpty(ItemStack s){if(s.getCount()!=1||charge(s)>0)return false;boolean erased=dev.cosmerearcanists.copper.CopperService.eraseItem(s);var root=data(s);var tag=root.getCompound(KEY);if(!tag.contains("owner")&&!tag.contains("metal")&&!tag.contains("unkeyed"))return erased;tag.remove("owner");tag.remove("metal");tag.remove("unkeyed");tag.putDouble("charge",0);root.put(KEY,tag);s.set(DataComponents.MAX_STACK_SIZE,1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(root));return true;}
    /** Destructive wipe integration: erase notebook/server images before removing ordinary charge and ownership. */
    public static boolean wipe(ItemStack s){if(s.getCount()!=1)return false;if(!dev.cosmerearcanists.nicrosil.NicrosilService.eraseItem(s))return false;boolean notebook=!dev.cosmerearcanists.copper.CopperService.identity(s).equals(dev.cosmerearcanists.copper.CopperNetwork.ZERO);boolean erased=dev.cosmerearcanists.copper.CopperService.eraseItem(s);if(notebook&&!erased)return false;var root=data(s);if(!root.contains(KEY))return erased;root.remove("cosmerearcanists_duralumin_commitment");var old=root.getCompound(KEY);var empty=new CompoundTag();if(old.contains("capacity"))empty.putDouble("capacity",old.getDouble("capacity"));empty.putDouble("charge",0);root.put(KEY,empty);s.set(DataComponents.CUSTOM_DATA,CustomData.of(root));return true;}
    public record Access(ItemStack stack,Runnable changed){}
    public static java.util.List<Access> touching(ServerPlayer p){
        var result=new java.util.ArrayList<Access>();
        if(FeruchemyConfig.CURIOS.get()&&net.neoforged.fml.ModList.get().isLoaded("curios"))result.addAll(CuriosMetalminds.touching(p));
        for(var hand:InteractionHand.values())result.add(new Access(p.getItemInHand(hand),()->p.getInventory().setChanged()));
        if(FeruchemyConfig.INVENTORY.get())for(var stack:p.getInventory().items)result.add(new Access(stack,()->p.getInventory().setChanged()));
        for(var access:result)if(unkeyEmpty(access.stack()))access.changed().run();
        return result;
    }
    /** Store local-time charge, returning the equivalent base-tick payment for effect/capacity accounting.
     * Only periodic storage uses this: tapping, refunds and direct transfers retain their exact amounts. */
    public static double storageRate(ServerPlayer p){return dev.cosmerearcanists.power.BubbleConfig.FERUCHEMY_STORAGE.get()?Math.max(1,dev.cosmerearcanists.power.BubbleService.rate(p)):1;}
    public static double accessibleSpace(ServerPlayer p,String metal){double total=0;var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<ItemStack,Boolean>());for(var access:touching(p)){var s=access.stack();if(seen.add(s)&&writable(p,s,metal))total+=Math.max(0,capacity(s)-charge(s));}return total;}
    public static double storePerTick(ServerPlayer p,String metal,double amount){
        double rate=storageRate(p);
        return transferAccessible(p,metal,amount*rate)/rate;
    }
    public static double transferAccessible(ServerPlayer p,String metal,double amount){
        if(amount>0&&StorageActivity.blocked(p))return 0;
        double remaining=amount;
        var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<ItemStack,Boolean>());
        for(var access:touching(p)){
            if(!seen.add(access.stack()))continue;
            double moved=remaining>0?transferForStorage(p,access.stack(),metal,remaining):transferFor(access.stack(),p,metal,remaining);if(moved!=0){access.changed().run();remaining-=moved;}
            if(Math.abs(remaining)<1e-10)break;
        }
        return amount-remaining;
    }
    public static double accessibleCharge(ServerPlayer p,String metal){double total=0;var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<ItemStack,Boolean>());for(var access:touching(p)){var s=access.stack();if(seen.add(s)&&usableBy(s,p,metal))total+=Math.min(charge(s),capacity(s));}return total;}
    private Metalminds(){}
}
