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
    @Test void dodgeScalesGentlyAndCannotExceedCap() {
        assertEquals(.1,TemporalMath.dodgeChance(2,2,.1,.25),1e-9);
        assertEquals(.2,TemporalMath.dodgeChance(8,2,.1,.25),1e-9);
        assertEquals(.25,TemporalMath.dodgeChance(2048,2,.1,.25),1e-9);
        assertEquals(0,TemporalMath.dodgeChance(0,2,.1,.25));
        assertEquals(0,TemporalMath.dodgeChance(Double.NaN,2,.1,.25));
        assertEquals(0,TemporalMath.dodgeChance(2,0,.1,.25));
    }
    @Test void relativeSweepsCatchFastProjectilesAndRejectNearMisses() {
        assertEquals(0,TemporalMath.closestDistanceSquared(8,0,0,-16,0,0),1e-9);
        assertEquals(4,TemporalMath.closestDistanceSquared(8,2,0,-16,0,0),1e-9);
        assertEquals(64,TemporalMath.closestDistanceSquared(8,0,0,16,0,0),1e-9);
        assertEquals(25,TemporalMath.closestDistanceSquared(3,4,0,0,0,0),1e-9);
    }
}
