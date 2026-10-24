package dev.cosmerearcanists.feruchemy;

/** Ordinary electrum retains its50% cap; the duralumin integration applies a separate amplified ceiling. */
public final class ElectrumMath {
    public static double damageReduction(double fundedLevel,double perLevel,double configuredCap){
        if(!Double.isFinite(fundedLevel)||!Double.isFinite(perLevel)||!Double.isFinite(configuredCap)||fundedLevel<=0||perLevel<=0||configuredCap<=0)return 0;
        return Math.min(.5,Math.min(configuredCap,fundedLevel*perLevel));
    }
    private ElectrumMath(){}
}
