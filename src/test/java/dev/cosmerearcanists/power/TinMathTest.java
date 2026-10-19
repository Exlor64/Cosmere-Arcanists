package dev.cosmerearcanists.power;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TinMathTest {
    @Test void hearingRangeScalesWithBurnStrengthAndVolumeButStaysBounded() {
        assertEquals(24,TinMath.range(1,1,24,64));
        assertTrue(TinMath.range(.5,1,24,64)<24);
        assertTrue(TinMath.range(2,1,24,64)>24);
        assertEquals(48,TinMath.range(1,4,24,64));
        assertEquals(64,TinMath.range(1024,1000,24,64));
        assertEquals(0,TinMath.range(0,1,24,64));
    }
    @Test void obstructionMutesAndUltimatelyEliminatesReveals() {
        assertEquals(.75,TinMath.audibility(6,0,24));
        assertEquals(.5,TinMath.audibility(6,6,24));
        assertEquals(0,TinMath.audibility(6,18,24));
        assertEquals(0,TinMath.audibility(6,Double.POSITIVE_INFINITY,24));
        assertEquals(0,TinMath.audibility(6,0,0));
    }
    @Test void heldZoomScalesAndCapsWithoutAffectingInactiveVision() {
        assertEquals(1,TinMath.zoom(0,1.5,8));
        assertEquals(2.5,TinMath.zoom(1,1.5,8));
        assertTrue(TinMath.zoom(2,1.5,8)>2.5);
        assertEquals(8,TinMath.zoom(1024,1.5,8));
    }
}
