package dev.cosmerearcanists.power;

public final class TemporalMath {
    public static int horizon(double strength,double base,double cap) {
        if(!Double.isFinite(strength)||strength<=0)return 0;
        return (int)Math.clamp(base*(1+.25*Math.log(strength)/Math.log(2)),4,cap);
    }
    /** Closest approach between two moving points over one tick (prevents fast arrows tunneling through warnings). */
    public static double closestDistanceSquared(double x,double y,double z,double dx,double dy,double dz) {
        double length=dx*dx+dy*dy+dz*dz;
        double t=length<1e-12?0:Math.clamp(-(x*dx+y*dy+z*dz)/length,0,1);
        x+=t*dx;y+=t*dy;z+=t*dz;return x*x+y*y+z*z;
    }
    public static double dodgeChance(double strength,double baselineFlare,double base,double cap) {
        if(!Double.isFinite(strength)||strength<=0||!Double.isFinite(baselineFlare)||baselineFlare<=0)return 0;
        return Math.clamp(base*Math.sqrt(strength/baselineFlare),0,Math.clamp(cap,0,1));
    }
    private TemporalMath() { }
}
