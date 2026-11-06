package dev.cosmerearcanists.connection;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import dev.cosmerearcanists.feruchemy.DuraluminFeruchemy;
import dev.cosmerearcanists.hemalurgy.IdentityAccess;
import dev.cosmerearcanists.power.PowerAttributes;

/** Source-agnostic world Connection sampling. Providers own their stacking, funding and expiry rules. */
public final class WorldConnectionAccess {
    public record State(double baseline,double effective){public State{if(!Double.isFinite(baseline)||baseline<0||!Double.isFinite(effective)||effective<0)throw new IllegalArgumentException("Invalid world Connection");}}
    @FunctionalInterface public interface Provider {State sample(ServerPlayer player,String world,State current);}
    private static final Map<ResourceLocation,Provider> PROVIDERS=new LinkedHashMap<>();
    public static AutoCloseable register(ResourceLocation id,Provider provider){if(PROVIDERS.size()>=64||PROVIDERS.putIfAbsent(id,Objects.requireNonNull(provider))!=null)throw new IllegalArgumentException("Duplicate/full Connection provider registry");return ()->PROVIDERS.remove(id,provider);}
    public static State sample(ServerPlayer player,String world){
        // No numeric planetary ledger exists yet: normalize an existing native/art connection to one.
        double baseline=IdentityAccess.worlds(player).contains(world)||world.equals("sel")&&player.getAttributeValue(PowerAttributes.AONDOR)>0?1:0;
        var state=new State(baseline,baseline*DuraluminFeruchemy.connectionMultiplier(player));
        for(var provider:List.copyOf(PROVIDERS.values()))state=Objects.requireNonNull(provider.sample(player,world,state),"Null Connection provider result");
        return state;
    }
    private WorldConnectionAccess(){}
}
