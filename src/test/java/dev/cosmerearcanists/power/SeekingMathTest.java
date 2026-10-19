package dev.cosmerearcanists.power;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SeekingMathTest {
    @Test void rangeAndClarityScaleWithStrengthButStayBounded() {
        assertEquals(24,SeekingMath.range(1,1,24,64));assertTrue(SeekingMath.range(2,1,24,64)>24);
        assertEquals(64,SeekingMath.range(1024,100,24,64));assertEquals(0,SeekingMath.range(0,1,24,64));
        assertEquals(.5,SeekingMath.clarity(12,24,1,1,1),1e-9);assertEquals(0,SeekingMath.clarity(24,24,1,1,1));
        assertTrue(SeekingMath.clarity(6,24,1,8,1)<SeekingMath.clarity(6,24,1,1,1));
    }
    @Test void cloudPiercingRequiresAnEdgeAndOverlapsStrengthenConcealment() {
        assertEquals(0,SeekingMath.transmission(1,1,true,3));assertEquals(0,SeekingMath.transmission(3,1,true,3));
        assertEquals(.25,SeekingMath.transmission(4,1,true,3),1e-9);
        assertEquals(0,SeekingMath.transmission(4,2,true,3));assertEquals(0,SeekingMath.transmission(1024,1,false,3));
        assertEquals(1,SeekingMath.transmission(1,0,true,3));
    }
    @Test void emotionalProtectionHasAnExplicitExtremeStrengthException() {
        assertTrue(SeekingMath.emotionBlocked(1,1,true,4));assertTrue(SeekingMath.emotionBlocked(1,4,true,4));
        assertFalse(SeekingMath.emotionBlocked(1,5,true,4));assertTrue(SeekingMath.emotionBlocked(1,100,false,4));
        assertFalse(SeekingMath.emotionBlocked(0,1,false,4));
    }
}
