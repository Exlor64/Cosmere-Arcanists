package dev.cosmerearcanists;
/** Stable independent terrain seed; does not change an existing Overworld's generation. */
public final class ScadrialWorld {
    public static final String ID="cosmerearcanists:scadrial";
    public static long seed(long worldSeed){return worldSeed^0x534341445249414CL;}
    private ScadrialWorld(){}
}
