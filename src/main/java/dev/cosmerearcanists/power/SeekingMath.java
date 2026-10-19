package dev.cosmerearcanists.power;

public final class SeekingMath {
    public static double range(double strength,double intensity,double base,double cap){return strength<=0 || intensity<=0?0:Math.min(cap,base*Math.sqrt(strength)*Math.sqrt(Math.min(4,intensity)));}
    public static double transmission(double bronze,double copper,boolean piercing,double ratio){
        if(bronze<=0)return 0;if(copper<=0)return 1;
        return !piercing || bronze<=copper*ratio?0:Math.clamp(1-copper*ratio/bronze,0,1);
    }
    public static double clarity(double distance,double range,double strength,double difficulty,double transmission){return range<=0?0:Math.clamp((1-distance/range)*Math.sqrt(Math.max(0,strength)/Math.max(.01,difficulty))*transmission,0,1);}
    public static boolean emotionBlocked(double copper,double incoming,boolean piercing,double ratio){return copper>0 && (!piercing || incoming<=copper*ratio);}
    private SeekingMath() { }
}
