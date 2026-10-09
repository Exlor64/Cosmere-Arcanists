package dev.cosmerearcanists.power;

/** Shared control rules. Feruchemy: negative stores, positive taps, zero off. */
public final class PowerRules {
    public static int clamp(String power, int state, int levels) {
        return power.startsWith("a:") ? Math.max(0, Math.min(2, state)) : Math.max(-levels, Math.min(levels, state));
    }
    public static int next(String power, int state, int levels) {
        return power.startsWith("a:") ? (state + 1) % 3 : state >= levels ? -levels : state + 1;
    }
    public static String label(String power, int state) {
        if (state == 0) return "Off";
        if (power.startsWith("a:")) return state == 2 ? "Flaring" : "Burning";
        return (state < 0 ? "Storing " : "Tapping ") + Math.abs(state);
    }
    public static boolean implemented(String power) { return power.equals("a:iron") || power.equals("a:steel"); }
    private PowerRules() { }
}
