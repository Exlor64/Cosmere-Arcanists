package dev.cosmerearcanists.compounding;

import dev.cosmerearcanists.power.*;
import java.util.*;
import java.util.function.BiFunction;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

/** Base strength is partitioned by Identity; equipment/spike providers report their own contributions separately. */
public final class PowerSources {
    private static final String KEY="power_source_identities";
    private static final List<BiFunction<ServerPlayer,String,List<IdentityMath.Portion>>> EXTERNAL=new ArrayList<>();
    public static synchronized void registerExternal(BiFunction<ServerPlayer,String,List<IdentityMath.Portion>> provider){EXTERNAL.add(Objects.requireNonNull(provider));}
    private static List<IdentityMath.Portion> read(ServerPlayer p,String key,double base){var out=new ArrayList<IdentityMath.Portion>();var list=PowerData.data(p).getCompound(KEY).getList(key,Tag.TAG_COMPOUND);double left=base;for(int i=0;i<Math.min(64,list.size())&&left>0;i++){var t=list.getCompound(i);double amount=t.getDouble("amount");if(Double.isFinite(amount)&&amount>0){amount=Math.min(left,amount);out.add(new IdentityMath.Portion(t.hasUUID("identity")?t.getUUID("identity"):null,amount));left-=amount;}}if(left>0)out.add(new IdentityMath.Portion(dev.cosmerearcanists.hemalurgy.IdentityAccess.nativeIdentity(p),left));return out;}
    private static void write(ServerPlayer p,String key,List<IdentityMath.Portion> values){var root=PowerData.data(p);var sources=root.getCompound(KEY);var list=new ListTag();for(var part:values){var t=new CompoundTag();if(part.identity()!=null)t.putUUID("identity",part.identity());t.putDouble("amount",part.strength());list.add(t);}if(list.isEmpty())sources.remove(key);else sources.put(key,list);root.put(KEY,sources);}
    private static void append(List<IdentityMath.Portion> list,UUID identity,double amount){if(amount<=0)return;for(int i=0;i<list.size();i++)if(Objects.equals(list.get(i).identity(),identity)){list.set(i,new IdentityMath.Portion(identity,list.get(i).strength()+amount));return;}list.add(new IdentityMath.Portion(identity,amount));}
    public static List<IdentityMath.Portion> personal(ServerPlayer p,String key){var values=read(p,key,PowerAttributes.baseStrength(p,key));write(p,key,values);return List.copyOf(values);}
    public static void baseChanged(ServerPlayer p,String key,double before,double after){var values=read(p,key,Math.max(0,before));double remove=Math.max(0,before-after);for(int i=0;i<values.size()&&remove>0;){var part=values.get(i);double took=Math.min(remove,part.strength());remove-=took;if(took==part.strength())values.remove(i);else{values.set(i,new IdentityMath.Portion(part.identity(),part.strength()-took));i++;}}if(after>before)append(values,dev.cosmerearcanists.hemalurgy.IdentityAccess.nativeIdentity(p),after-before);write(p,key,values);}
    public static boolean canGrant(ServerPlayer p,String key,UUID identity){var parts=personal(p,key);return parts.size()<64||parts.stream().anyMatch(v->Objects.equals(v.identity(),identity));}
    public static void grant(ServerPlayer p,String key,double amount,UUID identity){var parts=new ArrayList<>(personal(p,key));if(!canGrant(p,key,identity)||!dev.cosmerearcanists.nicrosil.NicrosilLedger.canReceive(PowerAttributes.baseStrength(p,key),amount))throw new IllegalArgumentException("Cannot add power source");double before=PowerAttributes.baseStrength(p,key);PowerAttributes.set(p,key,before+amount);append(parts,identity,amount);write(p,key,parts);}
    public static List<IdentityMath.Portion> components(ServerPlayer p,String key){var result=new ArrayList<>(personal(p,key));for(var provider:EXTERNAL){var list=provider.apply(p,key);if(list!=null)for(var part:list){if(result.size()>=128)break;if(part!=null)result.add(part);}}return List.copyOf(result);}
    public static IdentityMath.Pair pair(ServerPlayer p,String metal){return IdentityMath.best(components(p,"a:"+metal),components(p,"f:"+metal),dev.cosmerearcanists.hemalurgy.IdentityAccess.identities(p));}
    public static void reset(ServerPlayer p){PowerData.data(p).remove(KEY);}
    private PowerSources(){}
}
