package dev.cosmerearcanists.power;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SteelMovementTest {
    @Test void requiresAnAnchorInTheRequestedDirection() {
        assertEquals(0, SteelMovement.weights(new double[]{1,0,0}, new double[][]{{-1,0,0},{0,1,0}}, new double[]{1,1}, .5)[0]);
        assertEquals(0, SteelMovement.weights(new double[]{1,0,0}, new double[0][], new double[0], .5).length);
        assertEquals(1, SteelMovement.weights(new double[]{1,0,0}, new double[][]{{1,0,0}}, new double[]{1}, .5)[0], 1e-9);
    }
    @Test void obliqueAnchorsHaveLimitedForceAndDenseAnchorsShareABudget() {
        double[] oblique = SteelMovement.weights(new double[]{1,0,0}, new double[][]{{.8,.6,0}}, new double[]{1}, .5);
        assertTrue(oblique[0] > 0 && oblique[0] <= .36 + 1e-9);
        double[] dense = SteelMovement.weights(new double[]{1,0,0}, new double[][]{{1,0,0},{1,0,0}}, new double[]{1,1}, .5);
        assertEquals(1, dense[0]+dense[1], 1e-9);
    }
    @Test void rejectsExcessiveSidewaysMotionAndBalancesRealAnchors() {
        assertEquals(0, SteelMovement.weights(new double[]{1,0,0}, new double[][]{{.6,.8,0}}, new double[]{1}, .5)[0]);
        double[] pair = SteelMovement.weights(new double[]{1,0,0}, new double[][]{{.8,.6,0},{.8,-.6,0}}, new double[]{1,1}, .5);
        assertTrue(pair[0] > 0 && pair[1] > 0);
        assertTrue(Math.abs(.6*(pair[0]-pair[1])) < .05);
    }
    @Test void jumpRequiresMetalBelowAndCannotUseMetalAbove() {
        double[] weights = SteelMovement.weights(new double[]{0,1,0}, new double[][]{{0,1,0},{0,-1,0}}, new double[]{1,1}, .5);
        assertEquals(1, weights[0], 1e-9); assertEquals(0, weights[1]);
    }
}
