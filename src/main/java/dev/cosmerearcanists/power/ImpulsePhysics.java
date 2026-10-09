package dev.cosmerearcanists.power;

/** Operates on actual velocity and raw walking input, never measured displacement as a new force. */
public final class ImpulsePhysics {
    public static double[] add(double[] velocity, double[] impulse, double maxSpeed) {
        double[] result = {velocity[0]+impulse[0],velocity[1]+impulse[1],velocity[2]+impulse[2]};
        double cap=Math.max(maxSpeed,length(velocity)), speed=length(result);
        if(speed>cap) for(int i=0;i<3;i++) result[i]*=cap/speed;
        return result;
    }
    public static double[] walkingInput(double x, double z, double forceX, double forceZ) {
        double square=forceX*forceX+forceZ*forceZ;
        if(square<1e-12) return new double[]{x,z};
        double dot=x*forceX+z*forceZ;
        // Remove only the walking acceleration that opposes the applied force, preserving strafing.
        if(dot<0) {x-=forceX*dot/square;z-=forceZ*dot/square;}
        return new double[]{x,z};
    }
    private static double length(double[] v){return Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);}
    private ImpulsePhysics() { }
}
