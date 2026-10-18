package dev.cosmerearcanists.equipment;

public final class TetherPhysics {
    public static double charge(int ticks,int fullCharge){
        double f=Math.min(1,Math.max(0,ticks)/(double)Math.max(1,fullCharge)); return (f*f+2*f)/3;
    }
    /** Radial tension only: tangential momentum is left intact for swinging. */
    public static double tension(double distance,double ropeLength,double outwardVelocity){
        if(distance<=ropeLength)return 0;
        return Math.max(0,outwardVelocity)+Math.min(.4,(distance-ropeLength)*.3);
    }
    private TetherPhysics(){ }
}
