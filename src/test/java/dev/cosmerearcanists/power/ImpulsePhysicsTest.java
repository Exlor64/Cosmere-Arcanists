package dev.cosmerearcanists.power;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImpulsePhysicsTest {
    @Test void steelInFrontPushesBackwardWithoutReinjectingWalking() {
        double[] filtered=ImpulsePhysics.walkingInput(0,1,0,-.1);
        assertArrayEquals(new double[]{0,0},filtered,1e-9);
        double[] velocity={0,0,0};
        for(int i=0;i<5;i++) velocity=ImpulsePhysics.add(velocity,new double[]{0,0,-.1},2);
        assertArrayEquals(new double[]{0,0,-.5},velocity,1e-9);
    }
    @Test void preserveStrafingAndNeverMultiplyAlignedWalkingInput() {
        assertArrayEquals(new double[]{1,0},ImpulsePhysics.walkingInput(1,1,0,-.1),1e-9);
        assertArrayEquals(new double[]{1,-1},ImpulsePhysics.walkingInput(1,-1,0,-.1),1e-9);
        assertArrayEquals(new double[]{0,0},ImpulsePhysics.walkingInput(1,0,-.1,0),1e-9);
    }
    @Test void preservesRealInertiaAndAppliesDiagonalForce() {
        assertArrayEquals(new double[]{.3,-1.9,-.1},ImpulsePhysics.add(new double[]{.4,-2,0},new double[]{-.1,.1,-.1},2),1e-9);
        assertArrayEquals(new double[]{0,0,.3},ImpulsePhysics.add(new double[]{0,0,.4},new double[]{0,0,-.1},2),1e-9);
    }
    @Test void speedCapDoesNotInstantlyCancelAFastFall() {
        double[] v=ImpulsePhysics.add(new double[]{0,-3,0},new double[]{0,.1,0},2);
        assertEquals(-2.9,v[1],1e-9);
        v=ImpulsePhysics.add(new double[]{0,0,1.99},new double[]{0,0,.1},2);
        assertEquals(2,v[2],1e-9);
    }
    @Test void verticalForceDoesNotDisableWalking() {
        assertArrayEquals(new double[]{1,1},ImpulsePhysics.walkingInput(1,1,0,0),1e-9);
    }
}
