package dev.cosmerearcanists.aondor;

/** Pure finite Heat power validation; no rounding of requested numeric arguments. */
public final class AonHeatPower {
    public static double effective(double requested,double strength,double maximum){double power=requested*strength;if(!Double.isFinite(requested)||requested<0||!Double.isFinite(strength)||strength<0||!Double.isFinite(power)||power>Float.MAX_VALUE||maximum>0&&power>maximum)throw new IllegalArgumentException("Heat power must be finite and nonnegative, within the configured limit (0 = uncapped) and Minecraft numeric range");return power;}
    public static boolean reaches(double power,double threshold){return power>0&&power>=threshold;}
    public static double damage(double power,double threshold,double rate,double cap){double damage=reaches(power,threshold)?power*rate:0;if(cap>0)damage=Math.min(cap,damage);if(!Double.isFinite(damage)||damage>Float.MAX_VALUE)throw new IllegalArgumentException("Heat damage exceeds Minecraft numeric range");return damage;}
    private AonHeatPower(){}
}
