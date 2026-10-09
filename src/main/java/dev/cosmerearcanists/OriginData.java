package dev.cosmerearcanists;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Separate birth powers from later acquired powers. Disabled features retain saved data. */
public record OriginData(String planet, String race, String allomancy, String feruchemy) {
    private static final String KEY = "cosmerearcanists_origin";
    public static boolean assigned(ServerPlayer player) { return player.getPersistentData().contains(KEY); }
    public static OriginData read(ServerPlayer player) {
        var tag = player.getPersistentData().getCompound(KEY);
        return new OriginData(tag.getString("planet"), tag.getString("race"), tag.getString("allomancy"), tag.getString("feruchemy"));
    }
    public void save(ServerPlayer player) {
        var tag = new CompoundTag();
        tag.putInt("schema", 1);
        tag.putString("planet", planet); tag.putString("race", race);
        tag.putString("allomancy", allomancy); tag.putString("feruchemy", feruchemy);
        player.getPersistentData().put(KEY, tag);
    }
    public static void copy(ServerPlayer original, ServerPlayer replacement) {
        if (assigned(original)) replacement.getPersistentData().put(KEY, original.getPersistentData().getCompound(KEY).copy());
    }
    public static void reset(ServerPlayer player) { player.getPersistentData().remove(KEY); }
    public String summary() {
        return "Origin: " + planet + " / " + race + "; Allomancy: " + display(allomancy) + "; Feruchemy: " + display(feruchemy);
    }
    private static String display(String value) { return value.isEmpty() ? "none" : value; }
}
