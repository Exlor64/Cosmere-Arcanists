package dev.cosmerearcanists.power;
import dev.cosmerearcanists.feruchemy.FeruchemyLevels;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FeruchemyLevelsTest {
    @Test void fractionalStrengthGivesProportionalIntegerTapLimits(){
        assertEquals(10,FeruchemyLevels.tapLevels(1,10));assertEquals(15,FeruchemyLevels.tapLevels(1.5,10));assertEquals(20,FeruchemyLevels.tapLevels(2,10));assertEquals(14,FeruchemyLevels.tapLevels(1.49,10));assertEquals(0,FeruchemyLevels.tapLevels(0,10));assertEquals(0,FeruchemyLevels.tapLevels(Double.NaN,10));assertEquals(100000,FeruchemyLevels.tapLevels(1e9,10));
    }
    @Test void storageScalesFromAttributesAndFinalLevelIsPartialAtTheGlobalCeiling(){
        assertEquals(.3,FeruchemyLevels.maximumStored(1,.3,.95),1e-9);assertEquals(.45,FeruchemyLevels.maximumStored(1.5,.3,.95),1e-9);assertEquals(.6,FeruchemyLevels.maximumStored(2,.3,.95),1e-9);
        assertEquals(3,FeruchemyLevels.storeLevels(1,.1,.3,.95));assertEquals(5,FeruchemyLevels.storeLevels(1.5,.1,.3,.95));assertEquals(10,FeruchemyLevels.storeLevels(4,.1,.3,.95));
        assertEquals(.45,FeruchemyLevels.storedFraction(1.5,5,.1,.3,.95),1e-9);assertEquals(.95,FeruchemyLevels.storedFraction(100,10,.1,.3,1),1e-9);assertEquals(.95,FeruchemyLevels.maximumStored(100,.3,100),1e-9);
    }
    @Test void tapStrengthPerLevelIsFixedAndDoesNotMultiplyAgainByTheAttribute(){
        assertEquals(1.5,FeruchemyLevels.extraMass(10,.15,10000),1e-9);assertEquals(2.25,FeruchemyLevels.extraMass(15,.15,10000),1e-9);assertEquals(3,FeruchemyLevels.extraMass(20,.15,10000),1e-9);assertEquals(30,FeruchemyLevels.extraMass(200,.15,10000),1e-9);
    }
}
