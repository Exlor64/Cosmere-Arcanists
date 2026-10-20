package dev.cosmerearcanists.power;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TinMathTest {
    @Test void visionContinuouslyScalesWithBurningAndStrengthAndStaysBounded(){
        double low=TinMath.nightVision(.5,.6,.95),normal=TinMath.nightVision(1,.6,.95),flare=TinMath.nightVision(2,.6,.95);
        assertTrue(low>0 && low<normal && normal<flare && flare<TinMath.nightVision(4,.6,.95));
        assertEquals(.95*(1-Math.exp(-.6)),normal,1e-9);assertEquals(.95,TinMath.nightVision(1024,.6,.95),1e-9);
        assertEquals(0,TinMath.nightVision(0,.6,.95));assertEquals(0,TinMath.nightVision(Double.NaN,.6,.95));assertEquals(0,TinMath.nightVision(1,0,.95));assertEquals(0,TinMath.nightVision(1,.6,0));
    }
    @Test void lightmapLiftPreservesAlphaAndNeverDimsBrightPixels(){
        int color=0x7F804020;assertEquals(color,TinMath.liftLightmap(color,0));assertEquals(0x7FFFFFFF,TinMath.liftLightmap(color,1));
        assertEquals(0x7FC0A090,TinMath.liftLightmap(color,.5));assertEquals(0xFFFFFFFF,TinMath.liftLightmap(0xFFFFFFFF,.7));
        assertEquals(color,TinMath.liftLightmap(color,Double.NaN));assertEquals(0xFF808080,TinMath.liftLightmap(0xFF000000,.5));
    }
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
    @Test void storedSightDimsWorldLightAndPreservesPixelAlpha(){assertEquals(0x7F402010,TinMath.dimLightmap(0x7F804020,.5));assertEquals(0x7F804020,TinMath.dimLightmap(0x7F804020,2));assertEquals(0x7F000000,TinMath.dimLightmap(0x7F804020,0));}
    @Test void invisibleSightIsStrictlyBeyondTenAndBecomesClearer(){assertEquals(0,TinMath.invisibleClarity(10,10,30,.85));assertEquals(0,TinMath.invisibleClarity(10+1e-10,10,30,.85));assertTrue(TinMath.invisibleClarity(11,10,30,.85)>0);assertTrue(TinMath.invisibleClarity(20,10,30,.85)>TinMath.invisibleClarity(11,10,30,.85));assertEquals(.85,TinMath.invisibleClarity(100,10,30,.85));assertEquals(0,TinMath.invisibleClarity(Double.NaN,10,30,.85));}
    @Test void movementBlurRequiresMotionAndTappingAndScalesWithBoth(){assertEquals(0,TinMath.movementBlur(0,10,4,.08,.35));assertEquals(0,TinMath.movementBlur(.2,0,4,.08,.35));double weak=TinMath.movementBlur(.1,5,4,.08,.35);assertTrue(weak>0);assertTrue(TinMath.movementBlur(.3,5,4,.08,.35)>weak);assertTrue(TinMath.movementBlur(.1,20,4,.08,.35)>weak);assertTrue(TinMath.movementBlur(8,10000,4,.08,.35)<=.35);}
}
