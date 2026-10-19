package dev.cosmerearcanists.power;

/** Pure pewter balance rules; all health/debt values use health points, not hearts. */
public final class PewterMath {
    public static double mitigation(double attribute,double effective,double rate,double baseCap,double strongCap) {
        return Math.max(0,Math.min(effective*rate,attribute>1?Math.min(.70,strongCap):Math.min(.50,baseCap)));
    }
    public static double exhaustion(double amount,double effective,double reduction) {
        return Math.max(0,amount)*(1-Math.min(.90,Math.max(0,effective*reduction)));
    }
    public static double deferred(double damage,double absorption,double fraction) {
        return Math.max(0,damage-Math.max(0,absorption))*Math.clamp(fraction,0,.90);
    }
    public static double rested(double debt,double effective,double fraction) {
        return Math.max(0,debt)*(1-Math.clamp(effective*fraction,0,1));
    }
    private PewterMath() { }
}
