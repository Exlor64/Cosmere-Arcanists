package dev.cosmerearcanists.feruchemy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ElectrumMathTest {
    @Test void damageReductionScalesWithFundedLevelAndStopsAtHalf(){assertEquals(.05,ElectrumMath.damageReduction(1,.05,.5),1e-9);assertEquals(.25,ElectrumMath.damageReduction(5,.05,.5),1e-9);assertEquals(.5,ElectrumMath.damageReduction(10,.05,.5),1e-9);assertEquals(.5,ElectrumMath.damageReduction(100000,10,10),1e-9);}
    @Test void partialFuelAndLowerCapsAreRespected(){assertEquals(.0125,ElectrumMath.damageReduction(.25,.05,.5),1e-9);assertEquals(.2,ElectrumMath.damageReduction(10,.05,.2),1e-9);assertEquals(0,ElectrumMath.damageReduction(10,.05,0));}
    @Test void storingUnfundedOrInvalidInputNeverGrantsProtection(){assertEquals(0,ElectrumMath.damageReduction(-3,.05,.5));assertEquals(0,ElectrumMath.damageReduction(0,.05,.5));assertEquals(0,ElectrumMath.damageReduction(Double.NaN,.05,.5));assertEquals(0,ElectrumMath.damageReduction(10,Double.POSITIVE_INFINITY,.5));}
}
