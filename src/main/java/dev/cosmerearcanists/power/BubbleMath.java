package dev.cosmerearcanists.power;
/** Pure bounded time arithmetic, shared by server scheduling and regression tests. */
public final class BubbleMath {
    public static double rate(String metal,double strength,double minimum,double maximum){if(strength<=0)return 1;return Math.clamp(metal.equals("bendalloy")?1/(1+strength):1+strength,minimum,maximum);}
    public static double radius(double strength,double base,double maximum){return Math.clamp(base*Math.sqrt(Math.max(0,strength)),.5,maximum);}
    public static double blend(double current,double target,double ticks){return Math.abs(current-target)<1e-4?target:current+(target-current)/Math.max(1,ticks);}
    public static final class Clock {private double remainder;public int advance(double rate,int cap){double total=remainder+Math.max(0,rate);int ticks=Math.min(cap,(int)Math.floor(total+1e-9));remainder=total-Math.floor(total+1e-9);return ticks;}public double remainder(){return remainder;}}
    /** Number of actual crossings on a swept segment, including traversing the entire sphere in one frame. */
    public static int crossings(double ax,double ay,double az,double bx,double by,double bz,double radius){double dx=bx-ax,dy=by-ay,dz=bz-az,a=dx*dx+dy*dy+dz*dz;if(a<1e-12)return 0;double b=2*(ax*dx+ay*dy+az*dz),c=ax*ax+ay*ay+az*az-radius*radius,d=b*b-4*a*c;if(d<=1e-10)return 0;double s=Math.sqrt(d),t1=(-b-s)/(2*a),t2=(-b+s)/(2*a);return (t1>1e-7&&t1<=1?1:0)+(t2>1e-7&&t2<=1?1:0);}
    private BubbleMath(){}
}
