package dev.cosmerearcanists.power;

/** Shared control rules. Feruchemy: negative stores, positive taps, zero off. */
public final class PowerRules {
    public static int clamp(String power,int state,int levels){return clamp(power,state,levels,levels);}
    public static int clamp(String power,int state,int storeLevels,int tapLevels){
        return power.startsWith("a:")?Math.max(-Math.min(3,storeLevels),Math.min(3,state)):Math.max(-storeLevels,Math.min(tapLevels,state));
    }
    public static int next(String power,int state,int levels){return next(power,state,levels,levels);}
    public static int next(String power,int state,int storeLevels,int tapLevels){
        int current=clamp(power,state,storeLevels,tapLevels);
        return power.startsWith("a:")?(current>=3?0:current+1):current>=tapLevels?-storeLevels:current+1;
    }
    public static String label(String power, int state) {
        if (state == 0) return "Off";
        if (power.startsWith("a:")&&state<0) return "Compounding "+Math.abs(state);
        if (power.startsWith("a:")) return switch (state) { case 1 -> "Low burn"; case 2 -> "Burning"; default -> "Flaring"; };
        return (state < 0 ? "Storing " : "Tapping ") + Math.abs(state);
    }
    public static double levelMultiplier(int state, double low, double burning, double flaring) {
        return switch (state) { case 1 -> low; case 2 -> burning; case 3 -> flaring; default -> 0; };
    }
    public static boolean implemented(String power) { return power.equals("f:nicrosil") || power.equals("f:chromium") || power.equals("f:duralumin") || power.equals("f:electrum") || power.equals("f:zinc") || power.equals("f:aluminum") || power.equals("f:copper") || power.equals("f:cadmium") || power.equals("f:bendalloy") || power.equals("f:bronze") || power.equals("f:brass") || power.equals("f:tin") || power.equals("f:gold") || power.equals("f:pewter") || power.equals("f:steel") || power.equals("f:iron") || power.equals("a:bendalloy") || power.equals("a:cadmium") || power.equals("a:gold") || power.equals("a:chromium") || power.equals("a:nicrosil") || power.equals("a:aluminum") || power.equals("a:duralumin") || power.equals("a:iron") || power.equals("a:steel") || power.equals("a:pewter") || power.equals("a:tin") || power.equals("a:atium") || power.equals("a:electrum") || power.equals("a:bronze") || power.equals("a:copper") || power.equals("a:zinc") || power.equals("a:brass"); }
    private PowerRules() { }
}
