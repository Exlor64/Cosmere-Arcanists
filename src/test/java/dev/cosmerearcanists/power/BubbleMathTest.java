package dev.cosmerearcanists.power;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BubbleMathTest {
    @Test void userRequestedRatesAndPowerScalingAreBounded(){assertEquals(.5,BubbleMath.rate("bendalloy",1,.125,4));assertEquals(2,BubbleMath.rate("cadmium",1,.125,4));assertEquals(.125,BubbleMath.rate("bendalloy",1024,.125,4));assertEquals(4,BubbleMath.rate("cadmium",1024,.125,4));assertEquals(4,BubbleMath.radius(1,4,16));assertEquals(16,BubbleMath.radius(1024,4,16));}
    @Test void fractionalClockPreservesSlowLogicBudget(){var clock=new BubbleMath.Clock();int count=0;for(int i=0;i<100;i++)count+=clock.advance(.125,4);assertEquals(12,count);assertEquals(.5,clock.remainder(),1e-8);}
    @Test void fractionalClockPreservesFastLogicBudgetAndCapsWork(){var clock=new BubbleMath.Clock();int count=0;for(int i=0;i<10;i++)count+=clock.advance(2.5,4);assertEquals(25,count);assertEquals(4,clock.advance(1000,4));}
    @Test void sweptSphereDetectsEnteringExitingAndCompleteTraversal(){assertEquals(1,BubbleMath.crossings(-5,0,0,0,0,0,4));assertEquals(1,BubbleMath.crossings(0,0,0,5,0,0,4));assertEquals(2,BubbleMath.crossings(-10,0,0,10,0,0,4));assertEquals(0,BubbleMath.crossings(-10,4,0,10,4,0,4));assertEquals(0,BubbleMath.crossings(0,0,0,1,0,0,4));}
    @Test void transitionsConvergeWithoutOvershoot(){double value=1;for(int i=0;i<100;i++){value=BubbleMath.blend(value,.125,4);assertTrue(value>=.125&&value<=1);}assertEquals(.125,value,1e-8);}
}
