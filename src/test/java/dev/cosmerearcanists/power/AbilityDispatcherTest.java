package dev.cosmerearcanists.power;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilityDispatcherTest {
    @Test void activationRoutesToTheSelectedHandlerAndPreservesPressHoldReleasePhases() {
        var events = new ArrayList<AbilityDispatcher.Phase>();
        var dispatcher = new AbilityDispatcher<String, String>();
        String power = "f:test_" + UUID.randomUUID();
        dispatcher.register(power, (player, selected, phase, targets) -> {
            assertEquals(power, selected); events.add(phase);
        });
        dispatcher.activate(null, power, AbilityDispatcher.Phase.START, List.of());
        dispatcher.activate(null, "a:unimplemented", AbilityDispatcher.Phase.TICK, List.of());
        dispatcher.activate(null, power, AbilityDispatcher.Phase.TICK, List.of());
        dispatcher.activate(null, power, AbilityDispatcher.Phase.STOP, List.of());
        assertEquals(List.of(AbilityDispatcher.Phase.START, AbilityDispatcher.Phase.TICK, AbilityDispatcher.Phase.STOP), events);
    }
}
