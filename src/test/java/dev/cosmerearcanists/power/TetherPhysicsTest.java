package dev.cosmerearcanists.power;

import dev.cosmerearcanists.equipment.TetherPhysics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TetherPhysicsTest {
    @Test void hookReachesFullBowStyleChargeInTenTicks(){
        assertEquals(0,TetherPhysics.charge(0,10));assertEquals(1,TetherPhysics.charge(10,10));assertEquals(1,TetherPhysics.charge(20,10));
        assertTrue(TetherPhysics.charge(5,10)>.1 && TetherPhysics.charge(5,10)<1);
    }
    @Test void slackRopeDoesNotApplyForceAndTautRopeStopsOutwardMotion(){
        assertEquals(0,TetherPhysics.tension(5,10,2));assertEquals(0,TetherPhysics.tension(10,10,0));
        assertEquals(2.3,TetherPhysics.tension(11,10,2),1e-9);
        assertTrue(TetherPhysics.tension(11,10,-1)<TetherPhysics.tension(11,10,1));
    }
}
