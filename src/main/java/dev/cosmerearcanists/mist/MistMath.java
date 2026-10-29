package dev.cosmerearcanists.mist;

/** Stateless nightly weather: reproducible across reloads, independent rolls across nights. */
public final class MistMath {
    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
    private static double roll(long seed, long day, long salt) {
        return (mix(seed ^ mix(day + salt)) >>> 11) * 0x1.0p-53;
    }
    public static double weather(long seed, long dayTime, double chance) {
        long day = Math.floorDiv(dayTime, 24000);
        if (roll(seed, day, 0x534341445249414cL) >= chance) return 0;
        double time = Math.floorMod(dayTime, 24000);
        double start = 12500 + 1500 * roll(seed, day, 17);
        double end = 22500 + 1000 * roll(seed, day, 31);
        double envelope = smooth((time - start) / 800) * smooth((end - time) / 800);
        return envelope * (.65 + .35 * roll(seed, day, 47));
    }
    private static double smooth(double value) {
        double t = Math.max(0, Math.min(1, value));
        return t * t * (3 - 2 * t);
    }
    public static double perceivedDensity(double weather, double sight, double clearStrength) {
        if (!Double.isFinite(weather) || !Double.isFinite(sight) || !Double.isFinite(clearStrength)
            || clearStrength <= 0) return 0;
        return Math.max(0, Math.min(1, weather))
            * Math.max(0, 1 - Math.max(0, sight) / clearStrength);
    }
    /** Never extend existing visibility, including blindness/darkness fog. */
    public static float farPlane(float vanilla, float distance, float density) {
        return density <= .0001f ? vanilla : Math.min(vanilla, distance / density);
    }
    public static int benefitTier(double connection, double tap, double freeConnection,
                                  double infiniteConnection, double freeTap, double infiniteTap) {
        if (connection + 1e-7 >= Math.max(freeConnection, infiniteConnection)
            || tap + 1e-7 >= Math.max(freeTap, infiniteTap)) return 2;
        return connection + 1e-7 >= freeConnection || tap + 1e-7 >= freeTap ? 1 : 0;
    }
    private MistMath() {}
}
