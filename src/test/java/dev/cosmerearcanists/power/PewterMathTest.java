package dev.cosmerearcanists.power;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PewterMathTest {
    @Test void mitigationScalesAcrossBurnLevelsAndHasSeparateAttributeCaps() {
        assertEquals(.125,PewterMath.mitigation(1,.5,.25,.5,.7),1e-9);
        assertEquals(.25,PewterMath.mitigation(1,1,.25,.5,.7),1e-9);
        assertEquals(.5,PewterMath.mitigation(1,2,.25,.5,.7),1e-9);
        assertEquals(.5,PewterMath.mitigation(1,100,.25,1,1),1e-9);
        assertEquals(.7,PewterMath.mitigation(2,4,.25,1,1),1e-9);
        assertEquals(.3,PewterMath.mitigation(5,10,.25,.2,.3),1e-9);
    }
    @Test void deferredInjuryExcludesAbsorbedDamageAndCannotInventInjury() {
        assertEquals(.75,PewterMath.deferred(7.5,0,.1),1e-9);
        assertEquals(.35,PewterMath.deferred(7.5,4,.1),1e-9);
        assertEquals(0,PewterMath.deferred(3,4,.1));
        assertEquals(0,PewterMath.deferred(-3,0,.1));
    }
    @Test void exhaustionScalesButCannotBecomeNegative() {
        assertEquals(3,PewterMath.exhaustion(4,.5,.5),1e-9);
        assertEquals(2,PewterMath.exhaustion(4,1,.5),1e-9);
        assertEquals(.4,PewterMath.exhaustion(4,100,.5),1e-9);
        assertEquals(4,PewterMath.exhaustion(4,0,.5),1e-9);
    }
    @Test void restClearsAPercentageOfRemainingDebtAndScales() {
        assertEquals(9.9,PewterMath.rested(10,1,.01),1e-9);
        assertEquals(9.8,PewterMath.rested(10,2,.01),1e-9);
        assertEquals(0,PewterMath.rested(10,1024,.01),1e-9);
    }
}
