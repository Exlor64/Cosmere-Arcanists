package dev.cosmerearcanists.hemalurgy;
import dev.cosmerearcanists.power.PowerData;
import net.minecraft.server.level.ServerPlayer;
/** Sticky actual dimension visits; cloned with persistent PowerData, independent of Identity/origin. */
public final class ScadrialVisits {
    private static final String KEY="visited_scadrial";
    public static boolean observe(ServerPlayer p){if(HemalurgyConfig.SCADRIAL_DIMENSIONS.get().contains(p.level().dimension().location().toString()))PowerData.data(p).putBoolean(KEY,true);return visited(p);}
    public static boolean visited(ServerPlayer p){return PowerData.data(p).getBoolean(KEY);}
    private ScadrialVisits(){}
}
