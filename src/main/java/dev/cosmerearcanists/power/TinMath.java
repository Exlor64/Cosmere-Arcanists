package dev.cosmerearcanists.power;

public final class TinMath {
    public static double range(double power,double volume,double base,double cap){return Math.min(cap,base*Math.sqrt(Math.max(0,power))*Math.sqrt(Math.clamp(volume,0,4)));}
    public static double audibility(double distance,double resistance,double range){return range<=0?0:Math.clamp(1-(distance+resistance)/range,0,1);}
    public static double zoom(double strength,double rate,double cap){return Math.clamp(1+rate*Math.sqrt(Math.max(0,strength)),1,Math.max(1,cap));}
    public static double reveal(double power,double threshold,double full){return Math.clamp((power-threshold)/Math.max(.001,full-threshold),0,1);}
    public static double echoFade(double age,double lifetime,double hold){if(lifetime<=0 || age>=lifetime)return 0;double progress=Math.clamp(age/lifetime,0,1);return Math.sqrt(Math.clamp((1-progress)/Math.max(.001,1-Math.clamp(hold,0,.75)),0,1));}
    public static double waveReveal(double age,double distance,double speed){double t=Math.clamp((age-Math.max(0,distance)/Math.max(.01,speed))/2,0,1);return t*t*(3-2*t);}
    public static double smoothProgress(double age,double ticks){double t=Math.clamp(age/Math.max(1,ticks),0,1);return t*t*(3-2*t);}
    /** Smooth saturation: stronger senses keep improving without a jump to fullbright. */
    public static double nightVision(double power,double response,double cap){
        if(!Double.isFinite(power)||power<=0||!Double.isFinite(response)||response<=0)return 0;
        return Math.clamp(cap,0,1)*-Math.expm1(-power*response);
    }
    /** NativeImage RGBA pixels are ABGR packed ints. Lift shadow channels, preserving alpha. */
    public static int liftLightmap(int color,double amount){
        if(!Double.isFinite(amount)||amount<=0)return color;amount=Math.clamp(amount,0,1);
        int red=color&255,green=color>>>8&255,blue=color>>>16&255;
        red+=(int)Math.round((255-red)*amount);green+=(int)Math.round((255-green)*amount);blue+=(int)Math.round((255-blue)*amount);
        return color&0xFF000000 | blue<<16 | green<<8 | red;
    }
    private TinMath() { }
}
