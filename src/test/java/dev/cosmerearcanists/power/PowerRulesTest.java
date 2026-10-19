package dev.cosmerearcanists.power;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PowerRulesTest {
    @Test void allomancyCyclesThroughOffAndThreeBurnLevelsAndBoundsUntrustedValues() {
        assertEquals(1, PowerRules.next("a:iron", 0, 5));
        assertEquals(2, PowerRules.next("a:steel", 1, 5));
        assertEquals(3, PowerRules.next("a:steel", 2, 5));
        assertEquals(0, PowerRules.next("a:steel", 3, 5));
        assertEquals(3, PowerRules.clamp("a:iron", Integer.MAX_VALUE, 5));
        assertEquals(0, PowerRules.clamp("a:iron", Integer.MIN_VALUE, 5));
    }
    @Test void burnLevelsHaveDistinctStrengthConsumptionAndLabels() {
        assertEquals("Low burn", PowerRules.label("a:iron",1));
        assertEquals("Burning", PowerRules.label("a:steel",2));
        assertEquals("Flaring", PowerRules.label("a:steel",3));
        assertEquals(0, PowerRules.levelMultiplier(0,.5,1,2));
        assertEquals(.5, PowerRules.levelMultiplier(1,.5,1,2));
        assertEquals(1, PowerRules.levelMultiplier(2,.5,1,2));
        assertEquals(2, PowerRules.levelMultiplier(3,.5,1,2));
        assertEquals(3, PowerRules.levelMultiplier(3,.5,1,3));
    }
    @Test void feruchemyHasOffAndSignedStoreTapLevels() {
        assertEquals(-5, PowerRules.next("f:iron", 5, 5));
        assertEquals(-5, PowerRules.clamp("f:iron", -100, 5));
        assertEquals(5, PowerRules.clamp("f:iron", 100, 5));
        assertEquals("Storing 3", PowerRules.label("f:iron", -3));
        assertEquals("Tapping 3", PowerRules.label("f:iron", 3));
        assertEquals("Off", PowerRules.label("f:iron", 0));
    }
    @Test void onlyIronSteelAndPewterClaimImplementedEffects() {
        assertTrue(PowerRules.implemented("a:iron")); assertTrue(PowerRules.implemented("a:steel"));
        assertFalse(PowerRules.implemented("f:iron")); assertTrue(PowerRules.implemented("a:pewter")); assertFalse(PowerRules.implemented("a:tin"));
    }
}
