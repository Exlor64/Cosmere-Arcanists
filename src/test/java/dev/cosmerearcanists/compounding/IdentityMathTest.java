package dev.cosmerearcanists.compounding;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class IdentityMathTest {
    @Test void neutralMatchesButDifferentKeysDoNot(){var a=UUID.randomUUID();var b=UUID.randomUUID();var nativePower=List.of(new IdentityMath.Portion(a,1));assertEquals(0,IdentityMath.best(nativePower,List.of(new IdentityMath.Portion(b,1))).allomancy());assertEquals(1,IdentityMath.best(nativePower,List.of(new IdentityMath.Portion(null,1))).allomancy());}
    @Test void neutralNeverBridgesIncompatiblePortions(){var a=UUID.randomUUID();var b=UUID.randomUUID();var pair=IdentityMath.best(List.of(new IdentityMath.Portion(a,1),new IdentityMath.Portion(null,.5)),List.of(new IdentityMath.Portion(b,1),new IdentityMath.Portion(null,.5)));assertTrue(pair.allomancy()*pair.feruchemy()<2.25);}
    @Test void conversionClipsToBothCapacityAndFuel(){assertEquals(.01,CompoundingMath.input(10,.05,.1,10),1e-12);assertEquals(.001,CompoundingMath.input(.001,.05,100,10),1e-12);assertEquals(0,CompoundingMath.input(10,.05,0,10));assertEquals(0,CompoundingMath.input(Double.NaN,.05,100,10));}
}
