package dev.cosmerearcanists.power;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TinMathTest {
    @Test void hearingRangeScalesWithBurnStrengthAndVolumeButStaysBounded() {
        assertEquals(24,TinMath.range(1,1,24,64));
        assertTrue(TinMath.range(.5,1,24,64)<24);
        assertTrue(TinMath.range(2,1,24,64)>24);
        assertEquals(48,TinMath.range(1,4,24,64));
        assertEquals(64,TinMath.range(1024,1000,24,64));assertEquals(128,TinMath.range(1024,1,48,128));
        assertEquals(0,TinMath.range(0,1,24,64));
    }
    @Test void obstructionMutesAndUltimatelyEliminatesReveals() {
        assertEquals(.75,TinMath.audibility(6,0,24));
        assertEquals(.5,TinMath.audibility(6,6,24));
        assertEquals(0,TinMath.audibility(6,18,24));
        assertEquals(0,TinMath.audibility(6,Double.POSITIVE_INFINITY,24));
        assertEquals(0,TinMath.audibility(6,0,0));
    }
    @Test void baselineWireframesUnlockWeaklyAtFlaringAndGrowWithProgression() {
        assertEquals(0,TinMath.reveal(.5,1.5,6));
        assertEquals(0,TinMath.reveal(1,1.5,6));
        assertEquals(0,TinMath.reveal(1.5,1.5,6));
        assertEquals(1.0/9,TinMath.reveal(2,1.5,6),1e-9);
        assertTrue(TinMath.reveal(4,1.5,6)>TinMath.reveal(2,1.5,6));
        assertEquals(1,TinMath.reveal(6,1.5,6));
        assertEquals(1,TinMath.reveal(1024,1.5,6));
    }
    @Test void echoesHoldThenFadeSmoothlyWithoutOutlivingTheirLifetime(){
        assertEquals(1,TinMath.echoFade(0,100,.2));assertEquals(1,TinMath.echoFade(20,100,.2));
        assertTrue(TinMath.echoFade(50,100,.2)>.5);assertTrue(TinMath.echoFade(80,100,.2)<TinMath.echoFade(50,100,.2));
        assertEquals(0,TinMath.echoFade(100,100,.2));assertEquals(0,TinMath.echoFade(120,100,.2));assertEquals(0,TinMath.echoFade(0,0,.2));
    }
    @Test void waveArrivesAtDistantGeometryLaterAndRampsSmoothly(){assertEquals(0,TinMath.waveReveal(2,4,1));assertEquals(0,TinMath.waveReveal(4,4,1));assertEquals(.5,TinMath.waveReveal(5,4,1),1e-9);assertEquals(1,TinMath.waveReveal(6,4,1));assertTrue(TinMath.waveReveal(3,1,1)>TinMath.waveReveal(3,4,1));}
    @Test void snapshotBridgesInterpolateWithoutOvershooting(){assertEquals(0,TinMath.smoothProgress(0,4));assertEquals(.5,TinMath.smoothProgress(2,4));assertEquals(1,TinMath.smoothProgress(4,4));assertEquals(1,TinMath.smoothProgress(100,4));}
    @Test void heldZoomScalesAndCapsWithoutAffectingInactiveVision() {
        assertEquals(1,TinMath.zoom(0,1.5,8));
        assertEquals(2.5,TinMath.zoom(1,1.5,8));
        assertTrue(TinMath.zoom(2,1.5,8)>2.5);
        assertEquals(8,TinMath.zoom(1024,1.5,8));
    }
}
