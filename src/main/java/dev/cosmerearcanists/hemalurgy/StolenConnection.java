package dev.cosmerearcanists.hemalurgy;
import java.util.*;
import net.minecraft.nbt.*;
/** A bounded snapshot of the donor's own Identity and organic Connections, never inherited spike contents. */
public record StolenConnection(UUID identity,String homeWorld,Map<UUID,Double> players){
    public StolenConnection{Objects.requireNonNull(identity);Objects.requireNonNull(homeWorld);players=Map.copyOf(players);if(homeWorld.length()>128||!homeWorld.isEmpty()&&!homeWorld.matches("[a-z0-9_.:/-]+")||players.size()>256)throw new IllegalArgumentException("Invalid Connections");for(var value:players.values())if(!Double.isFinite(value)||value<=0||value>100)throw new IllegalArgumentException("Invalid bond");}
    public CompoundTag save(){var t=new CompoundTag();t.putUUID("identity",identity);t.putString("home_world",homeWorld);var list=new ListTag();players.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{var b=new CompoundTag();b.putUUID("player",e.getKey());b.putDouble("strength",e.getValue());list.add(b);});t.put("players",list);return t;}
    public static StolenConnection load(CompoundTag t){if(!t.hasUUID("identity"))throw new IllegalArgumentException("Missing Identity");var list=t.getList("players",Tag.TAG_COMPOUND);if(list.size()>256)throw new IllegalArgumentException("Too many bonds");var players=new HashMap<UUID,Double>();for(int i=0;i<list.size();i++){var b=list.getCompound(i);if(!b.hasUUID("player")||players.put(b.getUUID("player"),b.getDouble("strength"))!=null)throw new IllegalArgumentException("Invalid bond target");}return new StolenConnection(t.getUUID("identity"),t.getString("home_world"),players);}
}
