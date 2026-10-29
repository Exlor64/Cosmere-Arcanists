package dev.cosmerearcanists.mist;

import dev.cosmerearcanists.ScadrialWorld;
import dev.cosmerearcanists.ServerConfig;
import dev.cosmerearcanists.feruchemy.DuraluminFeruchemy;
import dev.cosmerearcanists.hemalurgy.AluminumSuppression;
import dev.cosmerearcanists.hemalurgy.IdentityAccess;
import dev.cosmerearcanists.power.PowerAttributes;
import dev.cosmerearcanists.power.PowerConfig;
import net.minecraft.server.level.ServerPlayer;

/** Ambient mist supply never grants powers or permanently writes free reserves. */
public final class MistBenefits {
    public static double scadrialConnection(ServerPlayer player) {
        boolean home = IdentityAccess.worlds(player).stream()
            .anyMatch(id -> id.equals("scadrial") || id.equals(ScadrialWorld.ID));
        double connection = Math.max(home ? 1 : 0, player.getAttributeValue(PowerAttributes.SCADRIAL_CONNECTION));
        return connection * DuraluminFeruchemy.connectionMultiplier(player);
    }
    /** 0 ordinary; 1 existing metals are free; 2 ambient unlimited reserves. */
    public static int tier(ServerPlayer player) {
        if (!MistConfig.BENEFITS.get() || !PowerConfig.ENABLED.get() || !ServerConfig.ALLOMANCY.enabled().get()
            || !player.isAlive() || player.isSpectator() || AluminumSuppression.active(player)
            || MistService.weather(player.serverLevel()) <= .01) return 0;
        double connection = scadrialConnection(player);
        double tapping = DuraluminFeruchemy.fundedLevels(player);
        return MistMath.benefitTier(connection, tapping, MistConfig.FREE_CONNECTION.get(),
            MistConfig.INFINITE_CONNECTION.get(), MistConfig.FREE_TAP.get(), MistConfig.INFINITE_TAP.get());
    }
    private MistBenefits() {}
}
