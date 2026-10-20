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
    public static boolean bound(ItemStack s){return !s.isEmpty()&&data(s).contains(KEY);}
    public static Filings.Source source(ItemStack s){
        if(s.isEmpty()||Filings.filed(s))return null;
        if(s.getItem() instanceof MetalmindItem band)return new Filings.Source(band.metal(),band.units());
        return FeruchemyConfig.DENOMINATIONS.get()?Filings.source(s):null;
    }
    public static UUID owner(ItemStack s){var t=data(s).getCompound(KEY);return t.hasUUID("owner")?t.getUUID("owner"):null;}
    public static double charge(ItemStack s){double value=data(s).getCompound(KEY).getDouble("charge");return Double.isFinite(value)?Math.max(0,value):0;}
    public static double capacity(ItemStack s){var t=data(s).getCompound(KEY);double value=t.getDouble("capacity");var source=source(s);return bound(s)?Double.isFinite(value)?Math.max(0,value):0:source==null?0:source.nuggetUnits()*FeruchemyConfig.CAPACITY.get();}
    public static boolean usable(ItemStack s,UUID user,String metal){
        var source=source(s);if(s.getCount()!=1||source==null||!source.metal().equals(metal))return false;
        if(!bound(s))return true;var root=data(s).getCompound(KEY);
        return root.getString("metal").equals(metal)&&owner(s)!=null&&(!FeruchemyConfig.OWNERSHIP.get()||user.equals(owner(s)));
    }
    /** Positive stores, negative taps. Returns signed amount actually transferred. Never splits a stack. */
    public static double transfer(ItemStack s,UUID user,String metal,double amount){
        if(!Double.isFinite(amount)||amount==0||!usable(s,user,metal))return 0;
        double before=Math.min(charge(s),capacity(s)),after=Math.clamp(before+amount,0,capacity(s));
        if(after==before)return 0;
        var root=data(s);var tag=root.getCompound(KEY);if(!bound(s)){tag.putUUID("owner",user);tag.putString("metal",metal);tag.putDouble("capacity",capacity(s));}
        tag.putDouble("charge",after);root.put(KEY,tag);s.set(DataComponents.MAX_STACK_SIZE,1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(root));return after-before;
    }
    public record Access(ItemStack stack,Runnable changed){}
    public static java.util.List<Access> touching(ServerPlayer p){
        var result=new java.util.ArrayList<Access>();
        if(FeruchemyConfig.CURIOS.get()&&net.neoforged.fml.ModList.get().isLoaded("curios"))result.addAll(CuriosMetalminds.touching(p));
        for(var hand:InteractionHand.values())result.add(new Access(p.getItemInHand(hand),()->p.getInventory().setChanged()));
        return result;
    }
    public static double transferAccessible(ServerPlayer p,String metal,double amount){
        double remaining=amount;
        var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<ItemStack,Boolean>());
        for(var access:touching(p)){
            if(!seen.add(access.stack()))continue;
            double moved=transfer(access.stack(),p.getUUID(),metal,remaining);if(moved!=0){access.changed().run();remaining-=moved;}
            if(Math.abs(remaining)<1e-10)break;
        }
        return amount-remaining;
    }
    public static double accessibleCharge(ServerPlayer p,String metal){double total=0;for(var access:touching(p)){var s=access.stack();if(usable(s,p.getUUID(),metal))total+=Math.min(charge(s),capacity(s));}return total;}
    private Metalminds(){}
}
