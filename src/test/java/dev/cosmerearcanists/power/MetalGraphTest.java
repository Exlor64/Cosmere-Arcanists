package dev.cosmerearcanists.power;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetalGraphTest {
    @Test void tracesComponentsAndRejectsAluminumAcrossAlternativeRecipes() {
        var graph = Map.of("gear", Set.of("iron"), "machine", Set.of("gear", "wood"),
                "mixed", Set.of("machine", "aluminum"), "mixed_component", Set.of("mixed"));
        var result = MetalGraph.resolve(graph, Set.of("iron", "aluminum"), Set.of("aluminum"));
        assertTrue(result.detectable("machine"));
        assertFalse(result.detectable("wood"));
        assertFalse(result.detectable("mixed"));
        assertFalse(result.detectable("mixed_component"));
    }
    @Test void cyclicRecipesTerminateAndOnlyBecomeMetalWhenConnectedToASeed() {
        var graph = Map.of("a", Set.of("b"), "b", Set.of("a", "iron"), "c", Set.of("d"), "d", Set.of("c"));
        var result = MetalGraph.resolve(graph, Set.of("iron"), Set.of());
        assertTrue(result.detectable("a")); assertTrue(result.detectable("b"));
        assertFalse(result.detectable("c")); assertFalse(result.detectable("d"));
    }
}
