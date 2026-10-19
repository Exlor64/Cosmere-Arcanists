package dev.cosmerearcanists.investiture;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import dev.cosmerearcanists.power.SeekingConfig;

/** Server integration API. Providers report actual active effects, never dormant powers or stored reserves.
 * Register during common setup on the server thread; emit one-shot pulses from server-side power execution.
 * No provider may force chunks. Copper concealment can be opted out of for effects with different lore rules.
 */
public final class InvestitureSignals {
    public static final ResourceLocation ALLOMANCY=ResourceLocation.fromNamespaceAndPath("cosmerearcanists","allomancy");
    public record Signal(ResourceLocation art,ResourceLocation effect,UUID source,Vec3 position,double intensity,double difficulty,int period,int burnState,boolean copperMaskable) {
        public Signal {
            Objects.requireNonNull(art);Objects.requireNonNull(effect);Objects.requireNonNull(source);Objects.requireNonNull(position);
            if(art.toString().length()>128 || effect.toString().length()>128 || !Double.isFinite(position.x)||!Double.isFinite(position.y)||!Double.isFinite(position.z)
                || Math.abs(position.x)>3e7 || Math.abs(position.y)>3e7 || Math.abs(position.z)>3e7 || !Double.isFinite(intensity)||intensity<=0||intensity>4096 || !Double.isFinite(difficulty)||difficulty<=0||difficulty>4096 || period<4||period>200 || burnState<0||burnState>3)throw new IllegalArgumentException("Invalid kinetic Investiture signal");
        }
    }
    @FunctionalInterface public interface Provider {void collect(ServerLevel level,Consumer<Signal> output);}
    private record Timed(Signal signal,long expires) { }
    private static final Map<ResourceLocation,Provider> PROVIDERS=new LinkedHashMap<>();
    private static final Map<ServerLevel,LinkedHashMap<String,Timed>> PULSES=new IdentityHashMap<>();
    public static void register(ResourceLocation id,Provider provider){if(PROVIDERS.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(provider))!=null)throw new IllegalArgumentException("Duplicate Investiture provider: "+id);}
    public static void unregister(ResourceLocation id){PROVIDERS.remove(id);}
    /** Returns whether accepted; repeated source/effect calls refresh, rather than duplicate, a pulse. */
    public static boolean emit(ServerLevel level,Signal signal,int lifetimeTicks) {
        if(!level.hasChunkAt(BlockPos.containing(signal.position)))return false;
        var pulses=PULSES.computeIfAbsent(level,l->new LinkedHashMap<>());pulses.values().removeIf(t->t.expires<=level.getGameTime());
        String key=signal.source+"/"+signal.art+"/"+signal.effect;
        if(!pulses.containsKey(key) && pulses.size()>=SeekingConfig.MAX_EMISSIONS.get())return false;
        pulses.put(key,new Timed(signal,level.getGameTime()+Math.clamp(lifetimeTicks,1,100)));return true;
    }
    public static List<Signal> collect(ServerLevel level) {
        var result=new LinkedHashMap<String,Signal>();
        Consumer<Signal> sink=s->{
            if(s==null || !level.hasChunkAt(BlockPos.containing(s.position)))return;
            String key=s.source+"/"+s.art+"/"+s.effect;
            if(result.size()<SeekingConfig.MAX_EMISSIONS.get() || result.containsKey(key))result.put(key,s);
        };
        for(var provider:PROVIDERS.values())provider.collect(level,sink);
        var pulses=PULSES.get(level);
        if(pulses!=null){pulses.values().removeIf(t->t.expires<=level.getGameTime());pulses.values().forEach(t->sink.accept(t.signal));if(pulses.isEmpty())PULSES.remove(level);}
        return List.copyOf(result.values());
    }
    public static void removeSource(UUID source){for(var pulses:PULSES.values())pulses.values().removeIf(t->t.signal.source.equals(source));}
    public static void clearTransient(){PULSES.clear();}
    private InvestitureSignals() { }
}
