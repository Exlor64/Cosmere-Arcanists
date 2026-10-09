package dev.cosmerearcanists.power;

public final class AllomanticPhysics {
    public static double acceleration(double force, double distance, double range, double mass) {
        return force * Math.max(0.15, 1.0 - distance / range) / Math.max(0.1, mass);
    }
    private AllomanticPhysics() { }
}
