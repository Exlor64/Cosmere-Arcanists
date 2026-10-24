package dev.cosmerearcanists.hemalurgy;
import dev.cosmerearcanists.compounding.PowerSources;
import dev.cosmerearcanists.connection.ConnectionBonds;
import dev.cosmerearcanists.power.PowerAttributes;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
public final class DuraluminTheft {
    public static final String POWER="trait:connection";
    /** Read at final settlement so the lethal hit's own newly formed bond is included. */
    public static StolenConnection capture(ServerPlayer p){var ledger=ConnectionBonds.get(p.server).ledger;var bonds=new HashMap<UUID,Double>();for(var pair:ledger.connections(p.getUUID()))bonds.put(pair.other(p.getUUID()),ledger.strength(pair.first(),pair.second()));return new StolenConnection(IdentityAccess.nativeIdentity(p),IdentityAccess.nativeWorld(p),bonds);}
    public static void remove(ServerPlayer p,StolenConnection stored){if(!capture(p).equals(stored))throw new IllegalStateException("Connections changed during theft");for(String key:PowerAttributes.keys())PowerSources.personal(p,key); // Preserve existing power provenance before issuing a new native Identity.
        var data=ConnectionBonds.get(p.server);data.ledger.remove(p.getUUID());data.setDirty();IdentityAccess.detachNative(p);dev.cosmerearcanists.connection.ConnectionService.clearSensing(p);}
    private DuraluminTheft(){}
}
