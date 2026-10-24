package dev.cosmerearcanists.power;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EnhancedCapsTest {
    @Test void ordinaryCapsAreIdenticalWithoutAmplification(){assertEquals(.5,EnhancedCaps.fraction(.5,1));assertEquals(8,EnhancedCaps.scaled(8,1));assertEquals(0,EnhancedCaps.fraction(0,5));assertEquals(1,EnhancedCaps.fraction(1,5));}
    @Test void enhancedFractionsExceedNormalCapsButNeverInvertDamage(){assertTrue(EnhancedCaps.fraction(.5,1.75)>.5);assertEquals(.95,EnhancedCaps.fraction(.5,100),1e-9);assertTrue(EnhancedCaps.fraction(.75,5)<1);assertEquals(40,EnhancedCaps.scaled(8,5));}
    @Test void velocityAmplificationNeverLowersAnExistingConfiguredLimit(){assertEquals(128,EnhancedCaps.velocity(128,1));assertEquals(128,EnhancedCaps.velocity(128,5));assertEquals(64,EnhancedCaps.velocity(32,5));}
    @Test void onlyEnhancedProjectilesMayExceedTheOrdinaryHundredDamageCap(){assertTrue(MetalProjectilePhysics.damage(1000000,10,3.5,2,500)<=100);assertTrue(MetalProjectilePhysics.enhancedDamage(1000000,10,3.5,2,500)>100);assertTrue(MetalProjectilePhysics.enhancedDamage(1000000,10,3.5,2,500)<=500);}
}
