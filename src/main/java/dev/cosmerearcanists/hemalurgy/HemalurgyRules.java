package dev.cosmerearcanists.hemalurgy;
import java.util.*;
/** Published power-stealing metal groups. Natural traits are defined separately; insertion is pending. */
public final class HemalurgyRules {
    private static Set<String> group(String art,String... metals){var keys=new HashSet<String>();for(String metal:metals)keys.add(art+":"+metal);return Set.copyOf(keys);}
    public static final Map<String,Set<String>> POWERS=Map.of(
        "steel",group("a","iron","steel","tin","pewter"),
        "pewter",group("f","iron","steel","tin","pewter"),
        "bronze",group("a","zinc","brass","copper","bronze"),
        "brass",group("f","zinc","brass","copper","bronze"),
        "cadmium",group("a","gold","electrum","cadmium","bendalloy","atium"),
        "gold",group("f","gold","electrum","cadmium","bendalloy"),
        "electrum",group("a","aluminum","duralumin","chromium","nicrosil"),
        "bendalloy",group("f","aluminum","duralumin","chromium","nicrosil"));
    public static boolean accepts(String metal,String key){return POWERS.getOrDefault(metal,Set.of()).contains(key);}
    /** Strongest personal power, ties broken by stable key order. Temporary modifiers never enter this map. */
    public static String choose(String metal,Map<String,Double> candidates){return candidates.entrySet().stream().filter(e->accepts(metal,e.getKey())&&Double.isFinite(e.getValue())&&e.getValue()>0&&e.getValue()<=1024).sorted(Comparator.<Map.Entry<String,Double>>comparingDouble(Map.Entry::getValue).reversed().thenComparing(Map.Entry::getKey)).map(Map.Entry::getKey).findFirst().orElse("");}
    public static double retained(double strength,double fraction){return Double.isFinite(strength)&&strength>0&&strength<=1024&&Double.isFinite(fraction)&&fraction>0&&fraction<=1?strength*fraction:0;}
    public static String description(String metal){return switch(metal){case "duralumin"->"Player Connection and Identity";case "aluminum"->"Suppress a killed player's powers temporarily (no mob effect)";case "zinc"->"Mental fortitude (emotional resistance)";case "copper"->"Mental strength and memory";case "iron"->"Physical strength (attack damage)";case "tin"->"Natural sight, hearing and touch";case "steel"->"Physical Allomancy";case "pewter"->"Physical Feruchemy";case "bronze"->"Mental Allomancy";case "brass"->"Cognitive Feruchemy";case "cadmium"->"Temporal Allomancy";case "gold"->"Hybrid Feruchemy";case "electrum"->"Enhancement Allomancy";case "bendalloy"->"Spiritual Feruchemy";default->"This metal does not steal an implemented power category.";};}
    private HemalurgyRules(){}
}
