package dev.cosmerearcanists.compounding;
/** Finite resource accounting shared by gameplay and standalone validation. */
public final class CompoundingMath {
    public static double fullness(double charge,double capacity){return Double.isFinite(charge)&&Double.isFinite(capacity)&&capacity>0?Math.max(0,Math.min(1,charge/capacity)):0;}
    public static double input(double reserve,double requested,double space,double gain){if(!Double.isFinite(reserve)||!Double.isFinite(requested)||!Double.isFinite(space)||!Double.isFinite(gain)||reserve<=0||requested<=0||space<=0||gain<=0)return 0;return Math.min(reserve,Math.min(requested,space/gain));}
    public static double boost(double charge,double capacity,double maximum){return Math.max(0,maximum)*fullness(charge,capacity);}
    public static int ticks(double charge,double capacity,double seconds){double fraction=fullness(charge,capacity);return fraction<=0?0:Math.max(1,(int)Math.min(Integer.MAX_VALUE,Math.ceil(20*seconds*fraction)));}
    private CompoundingMath(){}
}
