package dev.cosmerearcanists.power;

public final class TinMath {
    public static double range(double power,double volume,double base,double cap){return Math.min(cap,base*Math.sqrt(Math.max(0,power))*Math.sqrt(Math.clamp(volume,0,4)));}
    public static double audibility(double distance,double resistance,double range){return range<=0?0:Math.clamp(1-(distance+resistance)/range,0,1);}
    public static double zoom(double strength,double rate,double cap){return Math.clamp(1+rate*Math.sqrt(Math.max(0,strength)),1,Math.max(1,cap));}
    public static double reveal(double power,double threshold,double full){return Math.clamp((power-threshold)/Math.max(.001,full-threshold),0,1);}
    public static double echoFade(double age,double lifetime,double hold){if(lifetime<=0 || age>=lifetime)return 0;double progress=Math.clamp(age/lifetime,0,1);return Math.sqrt(Math.clamp((1-progress)/Math.max(.001,1-Math.clamp(hold,0,.75)),0,1));}
    private TinMath() { }
}
