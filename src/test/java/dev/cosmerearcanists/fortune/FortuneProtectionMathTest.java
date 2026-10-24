package dev.cosmerearcanists.fortune;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FortuneProtectionMathTest {
    @Test void chancesScaleAndStayBounded(){assertEquals(.2,FortuneProtectionMath.chance(10,.02,.5),1e-9);assertEquals(.1,FortuneProtectionMath.chance(10,.01,.4),1e-9);assertEquals(.95,FortuneProtectionMath.chance(100,1,1),1e-9);assertEquals(0,FortuneProtectionMath.chance(Double.NaN,.02,.5));}
    @Test void slightTurnPreservesSpeedForHorizontalAndVerticalShots(){var horizontal=FortuneProtectionMath.turn(2,0,0,0,0,1,Math.toRadians(12));assertEquals(4,horizontal[0]*horizontal[0]+horizontal[1]*horizontal[1]+horizontal[2]*horizontal[2],1e-9);assertTrue(horizontal[2]>0);var vertical=FortuneProtectionMath.turn(0,-3,0,1,0,0,Math.toRadians(12));assertEquals(9,vertical[0]*vertical[0]+vertical[1]*vertical[1]+vertical[2]*vertical[2],1e-9);assertTrue(vertical[0]>0&&vertical[1]<0);}
}
