package dev.cosmerearcanists.nicrosil;

/** Capacity and time limit each incremental transfer; quantities are moved rather than amplified. */
public final class NicrosilProgress {
    public static double target(double available,double requested){return Double.isFinite(available)&&available>0&&Double.isFinite(requested)&&requested>=0?Math.min(available,requested==0?available:requested):0;}
    public static double step(double remaining,double available,double space,double cost,double perSecond,double timeRate){
        for(double n:new double[]{remaining,available,space,cost,perSecond,timeRate})if(!Double.isFinite(n)||n<=0)return 0;
        return Math.max(0,Math.min(Math.min(remaining,available),Math.min(space/cost,perSecond/20*Math.max(1,timeRate))));
    }
    private NicrosilProgress(){}
}
