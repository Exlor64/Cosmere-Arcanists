package dev.cosmerearcanists.power;

/** Shared control rules. Feruchemy: negative stores, positive taps, zero off. */
public final class PowerRules {
    public static int clamp(String power, int state, int levels) {
        return power.startsWith("a:") ? Math.max(0, Math.min(3, state)) : Math.max(-levels, Math.min(levels, state));
    }
    public static int next(String power, int state, int levels) {
        return power.startsWith("a:") ? (clamp(power, state, levels) + 1) % 4 : state >= levels ? -levels : state + 1;
    }
    public static String label(String power, int state) {
        if (state == 0) return "Off";
        if (power.startsWith("a:")) return switch (state) { case 1 -> "Low burn"; case 2 -> "Burning"; default -> "Flaring"; };
        return (state < 0 ? "Storing " : "Tapping ") + Math.abs(state);
    }
    public static double levelMultiplier(int state, double low, double burning, double flaring) {
        return switch (state) { case 1 -> low; case 2 -> burning; case 3 -> flaring; default -> 0; };
    }
    public static boolean implemented(String power) { return power.equals("a:iron") || power.equals("a:steel") || power.equals("a:pewter"); }
    private PowerRules() { }
}
