package dev.cosmerearcanists.power;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PowerRulesTest {
    @Test void allomancyCyclesThroughThreeStatesAndBoundsUntrustedValues() {
        assertEquals(1, PowerRules.next("a:iron", 0, 5));
        assertEquals(2, PowerRules.next("a:steel", 1, 5));
        assertEquals(0, PowerRules.next("a:steel", 2, 5));
        assertEquals(2, PowerRules.clamp("a:iron", Integer.MAX_VALUE, 5));
        assertEquals(0, PowerRules.clamp("a:iron", Integer.MIN_VALUE, 5));
    }
    @Test void feruchemyHasOffAndSignedStoreTapLevels() {
        assertEquals(-5, PowerRules.next("f:iron", 5, 5));
        assertEquals(-5, PowerRules.clamp("f:iron", -100, 5));
        assertEquals(5, PowerRules.clamp("f:iron", 100, 5));
        assertEquals("Storing 3", PowerRules.label("f:iron", -3));
        assertEquals("Tapping 3", PowerRules.label("f:iron", 3));
        assertEquals("Off", PowerRules.label("f:iron", 0));
    }
    @Test void onlyIronSteelClaimImplementedEffects() {
        assertTrue(PowerRules.implemented("a:iron")); assertTrue(PowerRules.implemented("a:steel"));
        assertFalse(PowerRules.implemented("f:iron")); assertFalse(PowerRules.implemented("a:pewter"));
    }
}
