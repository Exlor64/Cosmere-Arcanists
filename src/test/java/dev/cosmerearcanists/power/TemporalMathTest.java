package dev.cosmerearcanists.power;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TemporalMathTest {
    @Test void foresightHasShortBoundedHorizonsAndModestStrengthScaling() {
        assertEquals(0,TemporalMath.horizon(0,20,40));
        assertEquals(15,TemporalMath.horizon(.5,20,40));
        assertEquals(20,TemporalMath.horizon(1,20,40));
        assertEquals(25,TemporalMath.horizon(2,20,40));
        assertEquals(40,TemporalMath.horizon(1024,20,40));
        assertEquals(0,TemporalMath.horizon(Double.NaN,20,40));
    }
    @Test void relativeSweepsCatchFastProjectilesAndRejectNearMisses() {
        assertEquals(0,TemporalMath.closestDistanceSquared(8,0,0,-16,0,0),1e-9);
        assertEquals(4,TemporalMath.closestDistanceSquared(8,2,0,-16,0,0),1e-9);
        assertEquals(64,TemporalMath.closestDistanceSquared(8,0,0,16,0,0),1e-9);
        assertEquals(25,TemporalMath.closestDistanceSquared(3,4,0,0,0,0),1e-9);
    }
}
