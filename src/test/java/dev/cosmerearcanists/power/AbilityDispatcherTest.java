package dev.cosmerearcanists.power;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilityDispatcherTest {
    @Test void activationRoutesToTheSelectedHandlerAndPreservesPressHoldReleasePhases() {
        var events = new ArrayList<AbilityDispatcher.Phase>();
        String power = "f:test_" + UUID.randomUUID();
        AbilityDispatcher.register(power, (player, selected, phase, targets) -> {
            assertEquals(power, selected); events.add(phase);
        });
        AbilityDispatcher.activate(null, power, AbilityDispatcher.Phase.START, List.of());
        AbilityDispatcher.activate(null, "a:unimplemented", AbilityDispatcher.Phase.TICK, List.of());
        AbilityDispatcher.activate(null, power, AbilityDispatcher.Phase.TICK, List.of());
        AbilityDispatcher.activate(null, power, AbilityDispatcher.Phase.STOP, List.of());
        assertEquals(List.of(AbilityDispatcher.Phase.START, AbilityDispatcher.Phase.TICK, AbilityDispatcher.Phase.STOP), events);
    }
}
