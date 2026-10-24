package dev.cosmerearcanists.nicrosil;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Future Stormlight/other resource integrations. No resource or power is fabricated by this API. */
public final class NicrosilResources {
    /** All callbacks run synchronously on the server and must not throw. A failed transfer MUST leave the provider unchanged.
     * Successful transfers move exactly the specified amount, with no asynchronous work or reentry.
     * The label and positive chargePerUnit conversion are stable for the provider registration. */
    public interface Provider {
        String label();
        double chargePerUnit();
        double available(ServerPlayer player);
        boolean canReceive(ServerPlayer player,double amount);
        boolean extract(ServerPlayer player,double amount);
        boolean insert(ServerPlayer player,double amount);
    }
    private static final Map<String,Provider> PROVIDERS=new LinkedHashMap<>();
    public static synchronized void register(ResourceLocation id,Provider provider){Objects.requireNonNull(provider);if(id.toString().length()>128)throw new IllegalArgumentException("Resource ID too long");if(PROVIDERS.containsKey(id.toString()))throw new IllegalArgumentException("Duplicate nicrosil resource "+id);if(!Double.isFinite(provider.chargePerUnit())||provider.chargePerUnit()<=0||provider.label().isBlank()||provider.label().length()>80)throw new IllegalArgumentException("Invalid nicrosil resource");PROVIDERS.put(id.toString(),provider);}
    public static synchronized Provider get(String key){return PROVIDERS.get(key);}
    public static synchronized Map<String,Provider> providers(){return Map.copyOf(PROVIDERS);}
    private NicrosilResources(){}
}
