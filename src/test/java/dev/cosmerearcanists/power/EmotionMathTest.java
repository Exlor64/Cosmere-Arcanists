package dev.cosmerearcanists.power;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EmotionMathTest {
    @Test void highestEmotionWinsWithStableTieOrder(){assertEquals(2,EmotionMath.dominant(new double[]{.2,.4,.8,.1,.3}));assertEquals(0,EmotionMath.dominant(new double[]{1,1,1,1,1}));}
    @Test void requestedCombatPercentagesScaleAndCap(){assertEquals(.25,EmotionMath.capped(1,.25,.75));assertEquals(.30,EmotionMath.capped(1,.30,.9));assertEquals(.25,EmotionMath.capped(2,.125,.75));assertEquals(.75,EmotionMath.capped(1024,.125,.75));}
    @Test void rangeFalloffIsBoundedAndOutsideRangeHasNoInfluence(){assertEquals(1,EmotionMath.falloff(0,16));assertEquals(.5,EmotionMath.falloff(8,16));assertEquals(.25,EmotionMath.falloff(16,16));assertEquals(0,EmotionMath.falloff(17,16));}
}
