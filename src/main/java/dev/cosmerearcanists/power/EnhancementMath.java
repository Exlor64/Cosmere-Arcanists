package dev.cosmerearcanists.power;
/** Resource math independent of the Minecraft runtime. */
public final class EnhancementMath {
    public static double attributeCurve(double strength,double growth){return Math.max(.1,1+Math.log(Math.max(.01,strength))/Math.log(2)*growth);}
    public static int flareTicks(double reserves,double maximum,int fullTicks){return reserves<=0||maximum<=0?0:Math.max(1,(int)Math.ceil(fullTicks*Math.clamp(reserves/maximum,0,1)));}
    private EnhancementMath() { }
}
