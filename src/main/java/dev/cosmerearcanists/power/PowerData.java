package dev.cosmerearcanists.power;

import java.util.*;
import dev.cosmerearcanists.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class PowerData {
    public static final String KEY = "cosmerearcanists_powers";
    public static CompoundTag data(ServerPlayer p) {
        if (!p.getPersistentData().contains(KEY)) p.getPersistentData().put(KEY, new CompoundTag());
        return p.getPersistentData().getCompound(KEY);
    }
    private static boolean enabled(String power) {
        if (power.length() < 3) return false;
        var art = power.startsWith("a:") ? ServerConfig.ALLOMANCY : ServerConfig.FERUCHEMY;
        try { return art.enabled().get() && art.metals().get(Metal.valueOf(power.substring(2).toUpperCase(Locale.ROOT))).get(); }
        catch (IllegalArgumentException ex) { return false; }
    }
    public static List<String> available(ServerPlayer p) {
        if (!PowerConfig.ENABLED.get()) return List.of();
        var result = new TreeSet<String>();
        if (OriginData.assigned(p)) {
            var origin = OriginData.read(p);
            if (!origin.allomancy().isEmpty()) result.add("a:" + origin.allomancy());
            if (!origin.feruchemy().isEmpty()) result.add("f:" + origin.feruchemy());
        }
        var grants = data(p).getCompound("grants");
        for (var key : grants.getAllKeys()) if (grants.getBoolean(key)) result.add(key);
        return result.stream().filter(PowerData::enabled).toList();
    }
    public static String selected(ServerPlayer p) {
        var list = available(p); var key = data(p).getString("selected");
        return list.contains(key) ? key : list.isEmpty() ? "" : list.getFirst();
    }
    public static int state(ServerPlayer p, String power) { return PowerRules.clamp(power, data(p).getCompound("states").getInt(power), PowerConfig.FERUCHEMY_LEVELS.get()); }
    public static void state(ServerPlayer p, String power, int state) {
        var d = data(p); var states = d.getCompound("states");
        states.putInt(power, PowerRules.clamp(power, state, PowerConfig.FERUCHEMY_LEVELS.get())); d.put("states", states);
    }
    public static double reserve(ServerPlayer p, String metal) { return Math.max(0, Math.min(PowerConfig.RESERVE_CAP.get(), data(p).getCompound("reserves").getDouble(metal))); }
    public static void reserve(ServerPlayer p, String metal, double amount) {
        var d = data(p); var reserves = d.getCompound("reserves");
        reserves.putDouble(metal, Math.max(0, Math.min(PowerConfig.RESERVE_CAP.get(), amount))); d.put("reserves", reserves);
    }
    public static void grant(ServerPlayer p, String power, boolean grant) {
        var d = data(p); var grants = d.getCompound("grants"); grants.putBoolean(power, grant); d.put("grants", grants);
    }
    public static void clonePlayer(ServerPlayer original, ServerPlayer replacement) {
        if (!original.getPersistentData().contains(KEY)) return;
        replacement.getPersistentData().put(KEY, original.getPersistentData().getCompound(KEY).copy());
        // Preserve reserves/selection but require deliberate reactivation after respawn.
        data(replacement).put("states", new CompoundTag());
    }
    private PowerData() { }
}
