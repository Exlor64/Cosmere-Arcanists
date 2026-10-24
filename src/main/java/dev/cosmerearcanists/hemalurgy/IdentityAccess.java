package dev.cosmerearcanists.hemalurgy;
import dev.cosmerearcanists.OriginData;
import dev.cosmerearcanists.power.PowerData;
import java.util.*;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
/** Authoritative implantation-provider boundary. Inventory/held spikes are deliberately NOT providers. */
public final class IdentityAccess {
    private static final String NATIVE="native_identity",HOME_LOST="home_connection_stolen";
    private static final List<Function<ServerPlayer,List<StolenConnection>>> PROVIDERS=new ArrayList<>();
    public static synchronized void registerImplanted(Function<ServerPlayer,List<StolenConnection>> provider){PROVIDERS.add(Objects.requireNonNull(provider));}
    public static UUID nativeIdentity(ServerPlayer p){var tag=PowerData.data(p);return tag.hasUUID(NATIVE)?tag.getUUID(NATIVE):p.getUUID();}
    public static String nativeWorld(ServerPlayer p){return PowerData.data(p).getBoolean(HOME_LOST)||!OriginData.assigned(p)?"":OriginData.read(p).planet();}
    public static List<StolenConnection> implanted(ServerPlayer p){if(!p.isAlive()||p.isRemoved()||p.isSpectator()||AluminumSuppression.active(p)||!HemalurgyService.enabled("duralumin"))return List.of();var out=new ArrayList<StolenConnection>();for(var provider:PROVIDERS){var values=provider.apply(p);if(values!=null)for(var value:values)if(value!=null&&out.size()<64)out.add(value);}return List.copyOf(out);}
    public static Set<UUID> identities(ServerPlayer p){var result=new HashSet<UUID>();result.add(nativeIdentity(p));for(var value:implanted(p))result.add(value.identity());return Set.copyOf(result);}
    public static boolean accepts(ServerPlayer p,UUID owner){return owner!=null&&identities(p).contains(owner);}
    public static Set<String> worlds(ServerPlayer p){var worlds=new HashSet<String>();String nativeWorld=nativeWorld(p);if(!nativeWorld.isEmpty())worlds.add(nativeWorld);for(var value:implanted(p))if(!value.homeWorld().isEmpty())worlds.add(value.homeWorld());return Set.copyOf(worlds);}
    public static Map<UUID,Double> bonds(ServerPlayer p){var ledger=dev.cosmerearcanists.connection.ConnectionBonds.get(p.server).ledger;var result=new HashMap<UUID,Double>();for(var pair:ledger.connections(p.getUUID()))result.put(pair.other(p.getUUID()),ledger.strength(pair.first(),pair.second()));for(var value:implanted(p))value.players().forEach((id,strength)->{if(!id.equals(p.getUUID()))result.merge(id,strength,Math::max);});return Map.copyOf(result);}
    public static void detachNative(ServerPlayer p){PowerData.data(p).putUUID(NATIVE,UUID.randomUUID());PowerData.data(p).putBoolean(HOME_LOST,true);}
    private IdentityAccess(){}
}
