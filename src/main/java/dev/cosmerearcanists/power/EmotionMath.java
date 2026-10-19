package dev.cosmerearcanists.power;

public final class EmotionMath {
    public static int dominant(double[] levels){int result=0;for(int i=1;i<levels.length;i++)if(levels[i]>levels[result])result=i;return result;}
    public static double falloff(double distance,double range){return range<=0||distance>range?0:Math.clamp(1-distance/range,.25,1);}
    public static double capped(double level,double rate,double cap){return Math.clamp(Math.max(0,level)*rate,0,cap);}
    private EmotionMath() { }
}
