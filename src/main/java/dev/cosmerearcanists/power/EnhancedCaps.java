package dev.cosmerearcanists.power;

/** Gameplay caps may rise; bounded fractions never become negative damage or unbounded immunity. */
public final class EnhancedCaps {
    public static double scaled(double cap,double multiplier){return cap*Math.max(1,multiplier);}
    public static double velocity(double cap,double multiplier){return Math.max(cap,Math.min(64,scaled(cap,multiplier)));}
    public static double fraction(double cap,double multiplier){if(multiplier<=1||cap<=0||cap>=1)return cap;return Math.max(cap,Math.min(.95,1-Math.pow(1-cap,multiplier)));}
    public static double scale(net.minecraft.server.level.ServerPlayer p,String power,double cap){return scaled(cap,EnhancementService.capMultiplier(p,power));}
    public static double fraction(net.minecraft.server.level.ServerPlayer p,String power,double cap){return fraction(cap,EnhancementService.capMultiplier(p,power));}
    private EnhancedCaps(){}
}
