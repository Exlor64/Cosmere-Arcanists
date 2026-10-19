package dev.cosmerearcanists.power;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EnhancementMathTest {
    @Test void attributeGrowthFavorsEfficiency(){assertEquals(1,EnhancementMath.attributeCurve(1,.1));assertEquals(1.1,EnhancementMath.attributeCurve(2,.1),1e-9);assertEquals(1.5,EnhancementMath.attributeCurve(2,.5),1e-9);assertTrue(EnhancementMath.attributeCurve(.01,.5)>0);}
    @Test void flareTimeScalesWithReserveFullness(){assertEquals(100,EnhancementMath.flareTicks(1000,1000,100));assertEquals(50,EnhancementMath.flareTicks(500,1000,100));assertEquals(25,EnhancementMath.flareTicks(250,1000,100));}
    @Test void flareTimeHandlesEmptyAndTinyReserves(){assertEquals(0,EnhancementMath.flareTicks(0,1000,100));assertEquals(0,EnhancementMath.flareTicks(1,0,100));assertEquals(1,EnhancementMath.flareTicks(.01,1000,100));assertEquals(100,EnhancementMath.flareTicks(2000,1000,100));}
}
