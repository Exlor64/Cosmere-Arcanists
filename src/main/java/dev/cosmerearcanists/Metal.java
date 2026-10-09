package dev.cosmerearcanists;

import java.util.Locale;

/** Stable save identifiers for the sixteen ordinary Metallic Arts metals. */
public enum Metal {
    IRON, STEEL, TIN, PEWTER, ZINC, BRASS, COPPER, BRONZE,
    ALUMINUM, DURALUMIN, CHROMIUM, NICROSIL, GOLD, ELECTRUM, CADMIUM, BENDALLOY;
    public String id() { return name().toLowerCase(Locale.ROOT); }
}
