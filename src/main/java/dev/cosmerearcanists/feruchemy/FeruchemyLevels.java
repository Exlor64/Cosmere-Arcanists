package dev.cosmerearcanists.feruchemy;
/** Pure level bounds. Fractional attributes count directly; a final store level can be partial. */
public final class FeruchemyLevels {
    public static final int MAX_LEVELS=100000;
    public static double maximumStored(double attribute,double perAttribute,double cap){return !Double.isFinite(attribute)||attribute<=0?0:Math.max(0,Math.min(.95,Math.min(cap,attribute*perAttribute)));}
    public static int storeLevels(double attribute,double step,double perAttribute,double cap){return step<=0?0:(int)Math.min(MAX_LEVELS,Math.ceil(maximumStored(attribute,perAttribute,cap)/step-1e-9));}
    public static int tapLevels(double attribute,double perAttribute){return !Double.isFinite(attribute)||attribute<=0?0:(int)Math.min(MAX_LEVELS,Math.max(0,Math.floor(attribute*perAttribute+1e-9)));}
    public static double storedFraction(double attribute,int level,double step,double perAttribute,double cap){return Math.min(maximumStored(attribute,perAttribute,cap),Math.max(0,level)*step);}
    public static double extraMass(int level,double step,double totalCap){return Math.max(0,Math.min(totalCap-1,Math.max(0,level)*step));}
    private FeruchemyLevels(){}
}
