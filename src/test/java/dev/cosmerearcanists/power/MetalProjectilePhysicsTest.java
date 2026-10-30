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
    @Test void heavierShotsUseTwoPlusPointTwoTimesTotalNuggets() {
        assertEquals(2,MetalProjectilePhysics.baseDamage(1,2,.2),1e-9);
        for(double units:new double[]{2,9,81,279}) {
            double expected=2+.2*units;
            assertEquals(expected,MetalProjectilePhysics.baseDamage(units,2,.2),1e-9);
            assertEquals(expected,MetalProjectilePhysics.damage(units,MetalProjectilePhysics.speed(units,1,3.5,8),3.5,2,.2,100),1e-9);
        }
        assertEquals(3.8,MetalProjectilePhysics.baseDamage(9,2,.2),1e-9);
        assertEquals(57.8,MetalProjectilePhysics.baseDamage(279,2,.2),1e-9);
    }
    @Test void powerAndImpactSpeedScaleTheWholeBaseFormula() {
        double reference=MetalProjectilePhysics.speed(9,1,3.5,8);
        double stronger=MetalProjectilePhysics.speed(9,2,3.5,8);
        assertEquals(3.8,MetalProjectilePhysics.damage(9,reference,3.5,2,.2,100),1e-9);
        assertEquals(7.6,MetalProjectilePhysics.damage(9,stronger,3.5,2,.2,100),1e-9);
        assertEquals(1.9,MetalProjectilePhysics.damage(9,reference/2,3.5,2,.2,100),1e-9);
    }
    @Test void realShotsUseOwnedPowerRatherThanTheMovementBoost() {
        double speed=MetalProjectilePhysics.speed(9,4,3.5,8);
        assertEquals(3.8,MetalProjectilePhysics.shotDamage(9,speed,speed,1,2,.2,100),1e-9);
        assertEquals(7.6,MetalProjectilePhysics.shotDamage(9,speed,speed,2,2,.2,100),1e-9);
        assertEquals(1.9,MetalProjectilePhysics.shotDamage(9,speed/2,speed,1,2,.2,100),1e-9);
        assertEquals(2,MetalProjectilePhysics.shotDamage(1,7,7,1,2,.2,100),1e-9);
        assertEquals(100,MetalProjectilePhysics.shotDamage(279,2,2,10,2,.2,100),1e-9);
    }
    @Test void launchSpeedScalesDirectlyWithEffectivePushStrength() {
        assertEquals(1.75,MetalProjectilePhysics.speed(1,.5,3.5,8),1e-9);
        assertEquals(3.5,MetalProjectilePhysics.speed(1,1,3.5,8),1e-9);
        assertEquals(7,MetalProjectilePhysics.speed(1,2,3.5,8),1e-9);
        assertEquals(8,MetalProjectilePhysics.speed(1,4,3.5,8),1e-9);
        double heavy=MetalProjectilePhysics.speed(279,1,3.5,8);
        assertEquals(heavy*2,MetalProjectilePhysics.speed(279,2,3.5,8),1e-9);
        assertEquals(0,MetalProjectilePhysics.speed(1,0,3.5,8));
    }
    @Test void anvilIsSlowerAndHasFarMoreRecoilThanNugget() {
        double light=MetalProjectilePhysics.speed(1,1,3.5,8), heavy=MetalProjectilePhysics.speed(279,1,3.5,8);
        assertTrue(heavy<light*.3);
        assertTrue(MetalProjectilePhysics.recoil(279,heavy,.2,80)>MetalProjectilePhysics.recoil(1,light,.2,80)*50);
        assertTrue(MetalProjectilePhysics.damage(279,heavy,3.5,2,100)>2);
    }
    @Test void increasingMassRemainsMonotonicAndRespectsTheHardCeiling() {
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
