package dev.cosmerearcanists.power;

public final class MetalProjectilePhysics {
    public static double speed(double metalUnits,double power,double nuggetSpeed,double maximumSpeed) {
        return Math.min(maximumSpeed,nuggetSpeed*Math.max(0,power)/Math.pow(Math.max(.001,metalUnits),.25));
    }
    public static double baseDamage(double metalUnits,double nuggetDamage,double damagePerNugget) {
        if(!Double.isFinite(metalUnits)||metalUnits<=0)return 0;
        return Math.max(0,nuggetDamage)+(metalUnits<=1?0:Math.max(0,damagePerNugget)*metalUnits);
    }
    public static double damage(double metalUnits,double speed,double nuggetSpeed,double nuggetDamage,double maximumDamage) {
        return damage(metalUnits,speed,nuggetSpeed,nuggetDamage,.2,maximumDamage);
    }
    public static double damage(double metalUnits,double speed,double nuggetSpeed,double nuggetDamage,double damagePerNugget,double maximumDamage) {
        return enhancedDamage(metalUnits,speed,nuggetSpeed,nuggetDamage,damagePerNugget,Math.min(100,maximumDamage));
    }
    public static double enhancedDamage(double metalUnits,double speed,double nuggetSpeed,double nuggetDamage,double maximumDamage) {
        return enhancedDamage(metalUnits,speed,nuggetSpeed,nuggetDamage,.2,maximumDamage);
    }
    public static double enhancedDamage(double metalUnits,double speed,double nuggetSpeed,double nuggetDamage,double damagePerNugget,double maximumDamage) {
        if(!Double.isFinite(metalUnits)||metalUnits<=0||!Double.isFinite(speed)||speed<=0)return 0;
        double cap=Math.min(100000,Math.max(.001,maximumDamage));
        // Compare against this mass's launch speed at reference power, so a heavier
        // projectile's lower launch speed does not cancel its requested damage formula.
        double referenceSpeed=Math.max(.001,nuggetSpeed)/Math.pow(Math.max(.001,metalUnits),.25);
        double velocityFactor=speed/referenceSpeed;
        return Math.min(cap,baseDamage(metalUnits,nuggetDamage,damagePerNugget)*velocityFactor);
    }
    /** Real shots capture power separately from the movement-force multiplier and speed cap. */
    public static double shotDamage(double metalUnits,double speed,double launchSpeed,double power,
                                    double nuggetDamage,double damagePerNugget,double maximumDamage) {
        if(!Double.isFinite(speed)||speed<=0||!Double.isFinite(launchSpeed)||launchSpeed<=0
            ||!Double.isFinite(power)||power<=0)return 0;
        return Math.min(Math.min(100000,Math.max(.001,maximumDamage)),
            baseDamage(metalUnits,nuggetDamage,damagePerNugget)*power*(speed/launchSpeed));
    }
    public static double recoil(double metalUnits,double speed,double nuggetMass,double playerMass) {
        return Math.max(0,metalUnits)*Math.max(0,nuggetMass)*speed/Math.max(1,playerMass);
    }
    private MetalProjectilePhysics() { }
}
