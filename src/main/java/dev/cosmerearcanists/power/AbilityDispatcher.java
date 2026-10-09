package dev.cosmerearcanists.power;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/** One activation control for every implemented power, including future instant and held abilities. */
public final class AbilityDispatcher {
    public enum Phase { START, TICK, STOP }
    @FunctionalInterface
    public interface Handler {
        void activate(ServerPlayer player, String power, Phase phase, List<MetalTarget> targets);
    }
    private static final Map<String, Handler> HANDLERS = new HashMap<>();
    public static void register(String power, Handler handler) {
        if (HANDLERS.putIfAbsent(power, handler) != null) throw new IllegalStateException("Duplicate ability: " + power);
    }
    public static void activate(ServerPlayer player, String power, Phase phase, List<MetalTarget> targets) {
        var handler = HANDLERS.get(power);
        if (handler != null) handler.activate(player, power, phase, targets);
    }
    private AbilityDispatcher() { }
}
