package dev.cosmerearcanists.power;

import java.util.*;
import dev.cosmerearcanists.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class PowerData {
    public static final String KEY = "cosmerearcanists_powers";
    public static CompoundTag data(ServerPlayer p) {
        if (!p.getPersistentData().contains(KEY)) p.getPersistentData().put(KEY, new CompoundTag());
        var tag = p.getPersistentData().getCompound(KEY);
        if (tag.getInt("burn_levels_schema") < 1) {
            var states = tag.getCompound("states");
            for (String power : states.getAllKeys()) if (power.startsWith("a:")) {
                int old = states.getInt(power);
                if (old == 1 || old == 2) states.putInt(power, old + 1);
            }
            tag.put("states", states); tag.putInt("burn_levels_schema", 1);
        }
        if(tag.getInt("reserve_units_schema")<1){
            var reserves=tag.getCompound("reserves");
            for(String metal:reserves.getAllKeys())reserves.putDouble(metal,Math.max(0,Math.min(PowerConfig.RESERVE_CAP.get(),PowerConfig.reserveCost(reserves.getDouble(metal)))));
            tag.put("reserves",reserves);tag.putInt("reserve_units_schema",1);
        }
        return tag;
    }
    private static boolean enabled(String power) {
        if((power.equals("a:bendalloy")||power.equals("a:cadmium"))&&!BubbleService.configured(power.substring(2)))return false;
        if(power.equals("a:gold")&&!GoldService.configured())return false;
        if ((power.equals("a:chromium") || power.equals("a:nicrosil")) && !ContactService.configured(power.substring(2)))return false;
        if ((power.equals("a:aluminum") || power.equals("a:duralumin")) && !EnhancementService.configured(power.substring(2)))return false;
        if (Emotion.power(power) && !EmotionService.configured(power)) return false;
        if (power.equals("a:bronze") && (!SeekingConfig.ENABLED.get() || !SeekingConfig.BRONZE.get())) return false;
        if (power.equals("a:copper") && (!SeekingConfig.ENABLED.get() || !SeekingConfig.COPPER.get())) return false;
        if (power.equals("a:atium")) return ServerConfig.ALLOMANCY.enabled().get() && TemporalConfig.ENABLED.get() && TemporalConfig.ATIUM.get();
        if (power.equals("a:electrum") && (!TemporalConfig.ENABLED.get() || !TemporalConfig.ELECTRUM.get())) return false;
        if (power.length() < 3) return false;
        var art = power.startsWith("a:") ? ServerConfig.ALLOMANCY : ServerConfig.FERUCHEMY;
        try { return art.enabled().get() && art.metals().get(Metal.valueOf(power.substring(2).toUpperCase(Locale.ROOT))).get(); }
        catch (IllegalArgumentException ex) { return false; }
    }
    public static List<String> available(ServerPlayer p) {
        PowerAttributes.migrate(p);
        if (!PowerConfig.ENABLED.get()) return List.of();
        return PowerAttributes.keys().stream().filter(key -> PowerAttributes.strength(p, key) > 0)
            .filter(PowerData::enabled).sorted().toList();
    }

    public static String secondary(ServerPlayer p) {
        String key=data(p).getString("secondary");
        return PowerConfig.SECONDARY_ENABLED.get() && available(p).contains(key) ? key : "";
    }
    public static String selected(ServerPlayer p) {
        String secondary=secondary(p);
        var list=available(p).stream().filter(key->!key.equals(secondary)).toList();
        var key=data(p).getString("selected");
        return list.contains(key) ? key : list.isEmpty() ? "" : list.getFirst();
    }
    public static boolean assignSlot(ServerPlayer p,String power,int slot) {
        if(!available(p).contains(power) || slot<0 || slot>1 || slot==1 && !PowerConfig.SECONDARY_ENABLED.get())return false;
        String primary=selected(p), secondary=secondary(p);
        var tag=data(p);
        if(slot==1) {
            tag.putString("secondary",power);
            if(primary.equals(power))tag.putString("selected",secondary);
        } else {
            tag.putString("selected",power);
            if(secondary.equals(power))tag.putString("secondary",primary);
        }
        return true;
    }
    public static int storeLevels(ServerPlayer p,String power){return power.equals("f:tin")?dev.cosmerearcanists.feruchemy.TinFeruchemy.storeLevels(p):power.equals("f:gold")?dev.cosmerearcanists.feruchemy.GoldFeruchemy.storeLevels(p):power.equals("f:pewter")?dev.cosmerearcanists.feruchemy.PewterFeruchemy.storeLevels(p):power.equals("f:steel")?dev.cosmerearcanists.feruchemy.SteelFeruchemy.storeLevels(p):power.equals("f:iron")?dev.cosmerearcanists.feruchemy.IronFeruchemy.storeLevels(p):PowerConfig.FERUCHEMY_LEVELS.get();}
    public static int tapLevels(ServerPlayer p,String power){return power.equals("f:tin")?dev.cosmerearcanists.feruchemy.TinFeruchemy.tapLevels(p):power.equals("f:gold")?dev.cosmerearcanists.feruchemy.GoldFeruchemy.tapLevels(p):power.equals("f:pewter")?dev.cosmerearcanists.feruchemy.PewterFeruchemy.tapLevels(p):power.equals("f:steel")?dev.cosmerearcanists.feruchemy.SteelFeruchemy.tapLevels(p):power.equals("f:iron")?dev.cosmerearcanists.feruchemy.IronFeruchemy.tapLevels(p):PowerConfig.FERUCHEMY_LEVELS.get();}
    public static int state(ServerPlayer p,String power){
        var d=data(p);var states=d.getCompound("states");int raw=states.getInt(power),value=PowerRules.clamp(power,raw,storeLevels(p,power),tapLevels(p,power));
        if(value!=raw){states.putInt(power,value);d.put("states",states);}return value;
    }
    public static void state(ServerPlayer p,String power,int state){
        var d=data(p);var states=d.getCompound("states");states.putInt(power,PowerRules.clamp(power,state,storeLevels(p,power),tapLevels(p,power)));d.put("states",states);
    }
    public static double reserve(ServerPlayer p, String metal) { return Math.max(0, Math.min(PowerConfig.RESERVE_CAP.get(), data(p).getCompound("reserves").getDouble(metal))); }
    public static void reserve(ServerPlayer p, String metal, double amount) {
        var d = data(p); var reserves = d.getCompound("reserves");
        reserves.putDouble(metal, Math.max(0, Math.min(PowerConfig.RESERVE_CAP.get(), amount))); d.put("reserves", reserves);
    }
    public static void grant(ServerPlayer p, String power, boolean grant) {
        PowerAttributes.set(p, power, grant ? Math.max(1, PowerAttributes.strength(p, power)) : 0);
    }
    public static void clonePlayer(ServerPlayer original, ServerPlayer replacement) {
        PowerAttributes.copy(original, replacement);
        replacement.getPersistentData().put(KEY, original.getPersistentData().getCompound(KEY).copy());
        // Preserve reserves/selection but require deliberate reactivation after respawn.
        data(replacement).put("states", new CompoundTag());
    }
    private PowerData() { }
}
