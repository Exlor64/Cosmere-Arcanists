package dev.cosmerearcanists.power;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AllomanticPhysicsTest {
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
}
