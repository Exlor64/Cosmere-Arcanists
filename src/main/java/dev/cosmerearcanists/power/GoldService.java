package dev.cosmerearcanists.power;

import java.util.*;
import dev.cosmerearcanists.*;
import net.minecraft.nbt.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Bounded personal history, not a restoration system. Recorded items are registry ids only. */
public final class GoldService {
    static final String KEY="gold_history";
    public static final List<EquipmentSlot> SLOTS=List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD);
    private static final Map<UUID,Integer> SELECTED=new HashMap<>();
    private static final Set<UUID> SENT=new HashSet<>();
    public static boolean configured(){return GoldConfig.ENABLED.get()&&PowerConfig.ENABLED.get()&&ServerConfig.ALLOMANCY.enabled().get()&&ServerConfig.ALLOMANCY.metals().get(Metal.GOLD).get();}
    public static double strength(ServerPlayer p){if(!configured()||!p.isAlive()||p.isSpectator()||PowerConfig.CONSUMPTION.get()&&PowerData.reserve(p,"gold")<=0)return 0;int state=PowerData.state(p,"a:gold");if(state==3&&!PowerConfig.FLARING.get())state=2;return PowerAttributes.effectiveStrength(p,"a:gold")*PowerRules.levelMultiplier(state,PowerConfig.LOW_STRENGTH.get(),PowerConfig.BURN_STRENGTH.get(),PowerConfig.FLARE_STRENGTH.get());}
    private static long clock(ServerPlayer p){return p.server.overworld().getGameTime();}
    private static CompoundTag root(ServerPlayer p){var data=PowerData.data(p);if(!data.contains(KEY,Tag.TAG_COMPOUND))data.put(KEY,new CompoundTag());return data.getCompound(KEY);}
    public static List<CompoundTag> history(ServerPlayer p){var list=root(p).getList("versions",Tag.TAG_COMPOUND);var result=new ArrayList<CompoundTag>();boolean anchor=GoldConfig.FIRST.get()&&list.size()>GoldConfig.LIMIT.get();if(anchor)result.add(list.getCompound(0).copy());for(int i=Math.max(anchor?1:0,list.size()-GoldConfig.LIMIT.get()+(anchor?1:0));i<list.size();i++)result.add(list.getCompound(i).copy());return List.copyOf(result);}
    private static CompoundTag appearance(ServerPlayer p){var t=new CompoundTag();var origin=OriginData.read(p);t.putString("planet",origin.planet());t.putString("race",origin.race());t.putInt("level",p.experienceLevel);t.putDouble("integrity",SpiritualIntegrity.get(p));var powers=new CompoundTag();for(String key:PowerAttributes.keys()){double value=PowerAttributes.strength(p,key);if(value>0)powers.putDouble(key,value);}t.put("powers",powers);var gear=new ListTag();for(var slot:SLOTS)gear.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(p.getItemBySlot(slot).getItem()).toString()));t.put("equipment",gear);return t;}
    /** Initial, changed or periodic snapshots; called even before the player obtains gold. */
    public static boolean record(ServerPlayer p){
        if(!configured()||!GoldConfig.RECORD.get()||!p.isAlive()||p.isSpectator())return false;
        var root=root(p);var list=root.getList("versions",Tag.TAG_COMPOUND);long now=clock(p);var current=appearance(p);var before=root.getCompound("lastAppearance");long age=now-root.getLong("lastTick");
        if(!list.isEmpty()&&current.equals(before)&&!GoldConfig.UNCHANGED.get())return false;
        if(!list.isEmpty()&&age>=0&&age<GoldConfig.INTERVAL.get()&&(current.equals(before)||age<GoldConfig.COOLDOWN.get()))return false;
        long sequence=Math.max(0,root.getLong("sequence"))+1;var snapshot=current.copy();snapshot.putLong("sequence",sequence);snapshot.putLong("tick",now);snapshot.putString("reason",list.isEmpty()?"First recorded self":current.equals(before)?"Later self":"Character changed");list.add(snapshot);
        while(list.size()>GoldConfig.LIMIT.get())list.remove(GoldConfig.FIRST.get()&&list.size()>1?1:0);root.put("versions",list);root.put("lastAppearance",current);root.putLong("lastTick",now);root.putLong("sequence",sequence);return true;
    }
    public static int accessible(ServerPlayer p){double power=strength(p);return power<=0?0:Math.min(GoldConfig.LIMIT.get(),Math.max(1,(int)Math.floor(GoldConfig.BASE_ACCESS.get()*Math.sqrt(power))));}
    public static CompoundTag selected(ServerPlayer p){var history=history(p);if(history.isEmpty()||accessible(p)==0)return new CompoundTag();int end=history.size()-1;if(end>0&&appearance(p).equals(root(p).getCompound("lastAppearance")))end--;int count=Math.min(end+1,accessible(p));return history.get(end-Math.floorMod(SELECTED.getOrDefault(p.getUUID(),0),count)).copy();}
    public static void activate(ServerPlayer p,String power,AbilityDispatcher.Phase phase){if(phase!=AbilityDispatcher.Phase.START||strength(p)<=0)return;if(history(p).isEmpty())record(p);if(history(p).isEmpty()){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Gold: no recorded history"),true);return;}if(PowerService.holdingAlternateAbility(p,"a:gold"))SELECTED.merge(p.getUUID(),1,(a,b)->a==Integer.MAX_VALUE?0:a+b);send(p);}
    public static List<String> details(ServerPlayer p,CompoundTag s){
        if(s.isEmpty())return List.of("Gold: no recorded history");var lines=new ArrayList<String>();long seconds=Math.max(0,clock(p)-s.getLong("tick"))/20;lines.add("Gold: version "+s.getLong("sequence")+" - "+(seconds<60?seconds+"s":seconds<3600?seconds/60+"m":seconds/3600+"h")+" ago");
        if(GoldConfig.DETAILS.get()){lines.add(s.getString("reason")+" | level "+s.getInt("level"));lines.add("Origin: "+display(s.getString("planet"))+" / "+display(s.getString("race")));lines.add("Spiritual integrity: "+String.format(Locale.ROOT,"%.0f%%",100*s.getDouble("integrity")));
            var powers=s.getCompound("powers");var values=new ArrayList<String>();for(String key:new TreeSet<>(powers.getAllKeys()))if(PowerAttributes.keys().contains(key))values.add(key+" "+String.format(Locale.ROOT,"%.2f",powers.getDouble(key)));if(values.isEmpty())lines.add("Powers: none recorded");else for(int i=0;i<values.size()&&lines.size()<9;i+=4){String text=String.join(", ",values.subList(i,Math.min(i+4,values.size())));lines.add(text.length()>160?text.substring(0,157)+"...":text);}}
        lines.add("Alt + Activate: older version");return List.copyOf(lines);
    }
    private static String display(String s){if(s.isEmpty())return "unassigned";return s.length()>48?s.substring(0,48):s;}
    /** Put the vision beside the observer, clipped before nearby solid blocks; no historic world is loaded. */
    public static Vec3 position(ServerPlayer p){var direction=new Vec3(-p.getLookAngle().z,0,p.getLookAngle().x).normalize();if(direction.lengthSqr()<.01)direction=new Vec3(1,0,0);var from=p.getEyePosition();var to=from.add(direction.scale(GoldConfig.DISTANCE.get()));for(int i=0;i<=16;i++)if(!p.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(from.lerp(to,i/16.0))))return p.position();var hit=p.serverLevel().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));var point=hit.getType()==HitResult.Type.MISS?to:hit.getLocation().subtract(direction.scale(.4));return point.add(0,-p.getEyeHeight(),0);}
    private static boolean viewing(ServerPlayer p){return strength(p)>0&&!ScoutService.active(p)&&(!GoldConfig.REQUIRE_HOLD.get()||PowerService.holdingAbility(p,"a:gold"));}
    public static void send(ServerPlayer p){if(!viewing(p)){clearVision(p);return;}var snapshot=selected(p);if(snapshot.isEmpty()){clearVision(p);return;}var gear=new ArrayList<String>();var stored=snapshot.getList("equipment",Tag.TAG_STRING);for(int i=0;i<6;i++)gear.add(GoldConfig.EQUIPMENT.get()&&i<stored.size()?stored.getString(i):"minecraft:air");float clarity=(float)Math.clamp(.16+.18*Math.sqrt(strength(p)),.16,.65);PacketDistributor.sendToPlayer(p,new GoldPacket(p.level().dimension().location().toString(),20,position(p),p.getYRot()-90,clarity,gear,details(p,snapshot)));SENT.add(p.getUUID());}
    public static void tick(ServerPlayer p){if(p.tickCount%40==0)record(p);double strength=strength(p);if(strength>0&&PowerConfig.CONSUMPTION.get()){int state=PowerData.state(p,"a:gold");if(state==3&&!PowerConfig.FLARING.get())state=2;EnhancementService.burn(p,"gold",GoldConfig.DRAIN.get()/20*PowerRules.levelMultiplier(state,PowerConfig.LOW_DRAIN.get(),1,PowerConfig.FLARE_DRAIN.get()));if(PowerData.reserve(p,"gold")<=0)PowerData.state(p,"a:gold",0);}if(!viewing(p))clearVision(p);else if(p.tickCount%GoldConfig.UPDATE.get()==0)send(p);}
    public static void clearVision(ServerPlayer p){if(SENT.remove(p.getUUID()))PacketDistributor.sendToPlayer(p,new GoldPacket(p.level().dimension().location().toString(),1,p.position(),0,0,Collections.nCopies(6,"minecraft:air"),List.of()));}
    private static void clear(ServerPlayer p){SELECTED.remove(p.getUUID());clearVision(p);}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void clone(PlayerEvent.Clone e){if(e.getOriginal() instanceof ServerPlayer old)clear(old);}
    @SubscribeEvent public void stopped(ServerStoppedEvent e){SELECTED.clear();SENT.clear();}
    public GoldService(){}
}
