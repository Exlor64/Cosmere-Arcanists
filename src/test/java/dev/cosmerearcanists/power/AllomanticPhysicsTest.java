package dev.cosmerearcanists.power;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AllomanticPhysicsTest {
    @Test void rangeScalesByBurnAndAttributeWithBoundedGrowth(){
        double exponent=Math.log(1.3)/Math.log(2);
        assertEquals(9.6,AllomanticPhysics.range(16,1,.6,exponent,32),1e-9);
        assertEquals(12.8,AllomanticPhysics.range(16,1,.8,exponent,32),1e-9);
        assertEquals(16,AllomanticPhysics.range(16,1,1,exponent,32),1e-9);
        assertEquals(20.8,AllomanticPhysics.range(16,2,1,exponent,32),1e-9);
        assertEquals(32,AllomanticPhysics.range(16,1024,1,exponent,32),1e-9);
        assertEquals(0,AllomanticPhysics.range(16,0,1,exponent,32));
        assertEquals(0,AllomanticPhysics.range(16,1,0,exponent,32));
        assertEquals(16,AllomanticPhysics.range(16,2,1,0,32));
    }
    @Test void equalOppositeForcesProduceDifferentAccelerationsAccordingToMass() {
        double player = AllomanticPhysics.acceleration(7, 4, 16, 80);
        double item = AllomanticPhysics.acceleration(7, 4, 16, 2);
        assertEquals(player * 80, item * 2, 1e-9);
        assertTrue(item > player);
    }
    @Test void distanceWeakensForceAndFlaringScalesIt() {
        double near = AllomanticPhysics.acceleration(7, 2, 16, 80);
        assertTrue(near > AllomanticPhysics.acceleration(7, 14, 16, 80));
        assertEquals(near * 2, AllomanticPhysics.acceleration(14, 2, 16, 80), 1e-9);
    }
    @Test void brakingReducesDamageByRemainingDescentEnergy() {
        assertEquals(25f, AllomanticPhysics.brakedFallDistance(100, -2, -1, 1, 1));
        assertEquals(62.5f, AllomanticPhysics.brakedFallDistance(100, -2, -1, 1, .5));
        assertEquals(100f, AllomanticPhysics.brakedFallDistance(100, -2, -1, 1, 0));
    }
    @Test void arrestingDescentCancelsAccumulatedDistanceButDoesNotProtectLaterFalls() {
        assertEquals(0f, AllomanticPhysics.brakedFallDistance(100, -2, 0, 2, 1));
        assertEquals(0f, AllomanticPhysics.brakedFallDistance(100, -2, .5, 2.5, 1));
        assertEquals(8f, AllomanticPhysics.brakedFallDistance(8, -1, -1.08, 0, 1));
    }
    @Test void sidewaysDownwardAndNonBrakingForcesDoNotReduceDamage() {
        assertEquals(100f, AllomanticPhysics.brakedFallDistance(100, -2, -2, 0, 1));
        assertEquals(100f, AllomanticPhysics.brakedFallDistance(100, -2, -3, -1, 1));
        assertEquals(100f, AllomanticPhysics.brakedFallDistance(100, 1, 2, 1, 1));
        assertEquals(100f, AllomanticPhysics.brakedFallDistance(100, -2, -2.1, .1, 1));
    }
}