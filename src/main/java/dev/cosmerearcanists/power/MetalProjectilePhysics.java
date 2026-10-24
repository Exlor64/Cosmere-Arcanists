package dev.cosmerearcanists.power;

public final class MetalProjectilePhysics {
    public static double speed(double metalUnits,double power,double nuggetSpeed,double maximumSpeed) {
        return Math.min(maximumSpeed,nuggetSpeed*Math.max(0,power)/Math.pow(Math.max(.001,metalUnits),.25));
    }
    public static double damage(double metalUnits,double speed,double nuggetSpeed,double nuggetDamage,double maximumDamage) {
        return enhancedDamage(metalUnits,speed,nuggetSpeed,nuggetDamage,Math.min(100,maximumDamage));
    }
    public static double enhancedDamage(double metalUnits,double speed,double nuggetSpeed,double nuggetDamage,double maximumDamage){
        double cap=Math.min(100000,Math.max(.001,maximumDamage));
        double reference=Math.min(nuggetDamage,cap*.999999);
        double energy=Math.max(0,metalUnits)*Math.pow(speed/Math.max(.001,nuggetSpeed),2);
        // Calibrated exponential saturation: one nugget at reference speed deals nuggetDamage.
        return Math.min(cap,cap*-Math.expm1(Math.log1p(-reference/cap)*energy));
    }
    public static double recoil(double metalUnits,double speed,double nuggetMass,double playerMass) {
        return Math.max(0,metalUnits)*Math.max(0,nuggetMass)*speed/Math.max(1,playerMass);
    }
    private MetalProjectilePhysics() { }
}
