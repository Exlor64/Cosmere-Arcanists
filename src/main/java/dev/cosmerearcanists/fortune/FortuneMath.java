package dev.cosmerearcanists.fortune;

/** Pure bounded luck policies, independent of loot contents and Minecraft state. */
public final class FortuneMath {
    private FortuneMath(){}
    public static int attempts(double budget,double random){if(!Double.isFinite(budget)||budget<=0)return 0;budget=Math.min(4,budget);int guaranteed=(int)Math.floor(budget);return Math.min(4,guaranteed+(random<budget-guaranteed?1:0));}
    public static double probability(double fraction,double rate){return Double.isFinite(fraction)&&Double.isFinite(rate)?Math.max(0,Math.min(1,fraction*rate)):0;}
    public static int score(int rarity,boolean valuable,boolean enchanted,int count){return (1+4*Math.max(0,Math.min(3,rarity))+(valuable?16:0)+(enchanted?8:0))*1024+Math.max(0,Math.min(64,count));}
    public static double[] rotate(double x,double z,double radians){return new double[]{x*Math.cos(radians)-z*Math.sin(radians),x*Math.sin(radians)+z*Math.cos(radians)};}
}
