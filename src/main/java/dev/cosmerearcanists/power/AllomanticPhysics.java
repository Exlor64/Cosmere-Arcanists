package dev.cosmerearcanists.power;

public final class AllomanticPhysics {
    public static double acceleration(double force, double distance, double range, double mass) {
        return force * Math.max(0.15, 1.0 - distance / range) / Math.max(0.1, mass);
    }
    public static float brakedFallDistance(float distance, double beforeY, double afterY, double impulseY, double strength) {
        if (distance <= 0 || beforeY >= 0 || impulseY <= 0 || afterY <= beforeY) return distance;
        double ratio = Math.max(0, -afterY) / -beforeY;
        double reduction = Math.max(0, Math.min(1, strength));
        return (float)(distance * (1 - reduction + reduction * ratio * ratio));
    }
    private AllomanticPhysics() { }
}
