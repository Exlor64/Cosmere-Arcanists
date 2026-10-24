package dev.cosmerearcanists.fortune;

/** Bounded independent protection probabilities and speed-preserving flight adjustments. */
public final class FortuneProtectionMath {
    private FortuneProtectionMath(){}
    public static double chance(double levels,double perLevel,double cap){if(!Double.isFinite(levels)||!Double.isFinite(perLevel)||!Double.isFinite(cap)||levels<=0)return 0;return Math.max(0,Math.min(.95,Math.min(cap,levels*perLevel)));}
    public static double[] turn(double x,double y,double z,double sideX,double sideY,double sideZ,double radians){
        double speed=Math.sqrt(x*x+y*y+z*z);if(!Double.isFinite(speed)||speed<1e-8||!Double.isFinite(radians))return new double[]{x,y,z};
        double ux=x/speed,uy=y/speed,uz=z/speed,dot=sideX*ux+sideY*uy+sideZ*uz;sideX-=dot*ux;sideY-=dot*uy;sideZ-=dot*uz;double length=Math.sqrt(sideX*sideX+sideY*sideY+sideZ*sideZ);if(!Double.isFinite(length)||length<1e-8)return new double[]{x,y,z};
        double c=Math.cos(radians),s=Math.sin(radians);return new double[]{speed*(ux*c+sideX/length*s),speed*(uy*c+sideY/length*s),speed*(uz*c+sideZ/length*s)};
    }
}
