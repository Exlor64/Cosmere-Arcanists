package dev.cosmerearcanists.power;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One activation control for every implemented power, including future instant and held abilities. */
public final class AbilityDispatcher<P, T> {
    public enum Phase { START, TICK, STOP }
    @FunctionalInterface
    public interface Handler<P, T> {
        void activate(P player, String power, Phase phase, List<T> targets);
    }
    private final Map<String, Handler<P, T>> handlers = new HashMap<>();
    public void register(String power, Handler<P, T> handler) {
        if (handlers.putIfAbsent(power, handler) != null) throw new IllegalStateException("Duplicate ability: " + power);
    }
    public void activate(P player, String power, Phase phase, List<T> targets) {
        var handler = handlers.get(power);
        if (handler != null) handler.activate(player, power, phase, targets);
    }
}
