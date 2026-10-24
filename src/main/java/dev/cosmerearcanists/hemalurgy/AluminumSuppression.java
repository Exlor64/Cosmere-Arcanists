package dev.cosmerearcanists.hemalurgy;
import dev.cosmerearcanists.power.*;
import net.minecraft.server.level.ServerPlayer;
/** Access lock; learned bases, Identity provenance and reserves remain intact. */
public final class AluminumSuppression {
    private static final String UNTIL="hemalurgic_aluminum_until";
    public static long remaining(ServerPlayer p){return Math.max(0,PowerData.data(p).getLong(UNTIL)-p.server.overworld().getGameTime());}
    public static boolean active(ServerPlayer p){return remaining(p)>0;}
    public static void apply(ServerPlayer p){
        long until=p.server.overworld().getGameTime()+20L*HemalurgyConfig.ALUMINUM_SECONDS.get();
        PowerData.data(p).putLong(UNTIL,Math.max(until,PowerData.data(p).getLong(UNTIL)));
        for(String key:PowerAttributes.keys())PowerService.powerTransferred(p,key);
        dev.cosmerearcanists.nicrosil.NicrosilService.clear(p);
        dev.cosmerearcanists.nicrosil.TemporaryPowers.stop(p);
        dev.cosmerearcanists.compounding.CompoundingService.clear(p);
        EnhancementService.clear(p);
    }
    private AluminumSuppression(){}
}
