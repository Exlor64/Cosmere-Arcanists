package dev.cosmerearcanists.nicrosil;

import dev.cosmerearcanists.power.*;
import java.util.*;
import java.util.function.BiPredicate;
import net.minecraft.server.level.ServerPlayer;

/** Only personal BASE attributes transfer. Hemalurgy/equipment MUST supply modifiers, never base values.
 * Exclusion hooks can veto additional externally sourced abilities without removing any contribution. */
public final class NicrosilPowers {
    private static final List<BiPredicate<ServerPlayer,String>> EXCLUSIONS=new ArrayList<>();
    public static synchronized void exclude(BiPredicate<ServerPlayer,String> exclusion){EXCLUSIONS.add(Objects.requireNonNull(exclusion));}
    public static double personal(ServerPlayer p,String key){if(!PowerAttributes.keys().contains(key)||EXCLUSIONS.stream().anyMatch(rule->rule.test(p,key)))return 0;return PowerAttributes.baseStrength(p,key);}
    public static String label(String key){if(key.equals("a:atium"))return "Allomantic Atium alloy";return (key.startsWith("a:")?"Allomantic ":"Feruchemical ")+key.substring(2,3).toUpperCase(Locale.ROOT)+key.substring(3);}
    private NicrosilPowers(){}
}
