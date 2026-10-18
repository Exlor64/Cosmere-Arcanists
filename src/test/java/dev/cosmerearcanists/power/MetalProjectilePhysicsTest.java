package dev.cosmerearcanists.power;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetalProjectilePhysicsTest {
    @Test void oneNuggetDealsTwoDamageAtBaselineAndAllLevelsScaleIt() {
        double speed=MetalProjectilePhysics.speed(1,1,3.5,8);
        assertEquals(2,MetalProjectilePhysics.damage(1,speed,3.5,2,100),1e-9);
        assertTrue(MetalProjectilePhysics.damage(1,MetalProjectilePhysics.speed(1,.5,3.5,8),3.5,2,100)<2);
        assertTrue(MetalProjectilePhysics.damage(1,MetalProjectilePhysics.speed(1,2,3.5,8),3.5,2,100)>2);
    }
    @Test void anvilIsSlowerAndHasFarMoreRecoilThanNugget() {
        double light=MetalProjectilePhysics.speed(1,1,3.5,8), heavy=MetalProjectilePhysics.speed(279,1,3.5,8);
        assertTrue(heavy<light*.3);
        assertTrue(MetalProjectilePhysics.recoil(279,heavy,.2,80)>MetalProjectilePhysics.recoil(1,light,.2,80)*50);
        assertTrue(MetalProjectilePhysics.damage(279,heavy,3.5,2,100)>2);
    }
    @Test void increasingMassApproachesTheHardCeilingSmoothly() {
        double previous=0;
        for(double mass:new double[]{1,9,81,279,1000,10000,1e6,1e12}) {
            double damage=MetalProjectilePhysics.damage(mass,MetalProjectilePhysics.speed(mass,1,3.5,8),3.5,2,100);
            assertTrue(damage>=previous && damage<=100); previous=damage;
        }
        assertTrue(previous>99.9);
        assertEquals(100,MetalProjectilePhysics.damage(1e12,8,3.5,2,1000));
        assertTrue(MetalProjectilePhysics.damage(10000,1,3.5,2,40)<=40);
    }
    @Test void impactSpeedMattersAndPowerCannotBypassSpeedCap() {
        assertEquals(8,MetalProjectilePhysics.speed(1,10000,3.5,8));
        assertEquals(0,MetalProjectilePhysics.damage(279,0,3.5,2,100));
        assertTrue(MetalProjectilePhysics.damage(9,1,3.5,2,100)<MetalProjectilePhysics.damage(9,2,3.5,2,100));
    }
}
