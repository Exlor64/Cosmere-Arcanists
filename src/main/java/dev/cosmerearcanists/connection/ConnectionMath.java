package dev.cosmerearcanists.connection;

/** Pure policies shared by server effects and standalone verification. */
public final class ConnectionMath {
    private ConnectionMath(){}
    public static double requiredBond(double levels,double first,double baseline,double sensitivity){return levels<first||!Double.isFinite(levels)?Double.POSITIVE_INFINITY:Math.max(1,baseline/(1+Math.max(0,levels-first)*sensitivity));}
    public static double interactionGain(double base,double bond,double diminishing){return Math.max(0,base)*(1-Math.max(0,Math.min(1,diminishing))*Math.max(0,Math.min(100,bond))/100);}
    public static double expressedBond(double bond,double targetConnection){return Math.max(0,Math.min(100,bond))*Math.max(0,Math.min(1,targetConnection));}
    public static double detectionMultiplier(double stored,double reduction){return 1-Math.min(.95,Math.max(0,stored)*Math.max(0,reduction));}
    public static int priceAdjustment(int base,double fraction){return (int)(Math.copySign(Math.floor(Math.max(0,base)*Math.abs(fraction)),fraction));}
}
