package dev.cosmerearcanists.feruchemy;

import dev.cosmerearcanists.*;
import dev.cosmerearcanists.power.*;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Item-owned wakefulness-seconds. Changes native insomnia, never the world's clock. */
public final class BronzeFeruchemy {
    private static final String POWER = "f:bronze", REMAINDER = "bronze_rest_remainder";
    private static final ResourceLocation DROWSY = ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "feruchemical_bronze_drowsiness");
    private static final Map<ServerPlayer, Double> ACTIVE = new WeakHashMap<>();

    public static boolean configured() {
        return PowerConfig.ENABLED.get() && FeruchemyConfig.ENABLED.get() && FeruchemyConfig.BRONZE.get()
            && ServerConfig.FERUCHEMY.enabled().get() && ServerConfig.FERUCHEMY.metals().get(Metal.BRONZE).get();
    }
    public static int storeLevels(ServerPlayer p) {
        return FeruchemyLevels.storeLevels(PowerAttributes.strength(p, POWER), FeruchemyConfig.BRONZE_STORE_STEP.get(), FeruchemyConfig.BRONZE_STORE_PER_ATTRIBUTE.get(), FeruchemyConfig.MAX_STORE.get());
    }
    public static int tapLevels(ServerPlayer p) {
        return FeruchemyLevels.tapLevels(PowerAttributes.strength(p, POWER), FeruchemyConfig.BRONZE_TAP_LEVELS.get());
    }
    public static double strength(ServerPlayer p) {
        double actual = ACTIVE.getOrDefault(p, 0.0);
        int state = PowerData.state(p, POWER);
        return configured() && p.isAlive() && !p.isSpectator() && state != 0 && Math.signum(state) == Math.signum(actual)
            ? PowerAttributes.strength(p, POWER) * Math.abs(actual) : 0;
    }
    private static void drowsiness(ServerPlayer p, double fraction) {
        double amount = -Math.min(.95, fraction * FeruchemyConfig.BRONZE_SLOW.get());
        for (var key : java.util.List.of(Attributes.MOVEMENT_SPEED, Attributes.ATTACK_SPEED, Attributes.BLOCK_BREAK_SPEED)) {
            var attribute = p.getAttribute(key);
            if (attribute == null) continue;
            var old = attribute.getModifier(DROWSY);
            if (old != null && Math.abs(old.amount() - amount) < 1e-10) continue;
            attribute.removeModifier(DROWSY);
            if (amount < 0) attribute.addTransientModifier(new AttributeModifier(DROWSY, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
    public static void clear(ServerPlayer p) {
        ACTIVE.remove(p);
        BronzeAlertness.clear(p);
        drowsiness(p, 0);
        // Fractional insomnia corrections persist with PowerData: toggling/relogging cannot erase them.
    }
    private static void rest(ServerPlayer p, double correction) {
        var data = PowerData.data(p);
        if (p.isSleeping()) { data.remove(REMAINDER); return; } // Native beds still provide normal rest.
        var stat = Stats.CUSTOM.get(Stats.TIME_SINCE_REST);
        int before = p.getStats().getValue(stat);
        double remainder = data.getDouble(REMAINDER);
        if (!Double.isFinite(remainder) || Math.abs(remainder) >= 1) remainder = 0;
        double balance = remainder + correction;
        long whole = (long) (balance >= 0 ? Math.floor(balance + 1e-9) : Math.ceil(balance - 1e-9));
        int after = (int) Math.clamp((long) before + whole, 0L, (long) Integer.MAX_VALUE);
        // No banking unused rest below zero or debt above the native statistic's limit.
        boolean clipped = before + whole != after || after == 0 && balance < 0 || after == Integer.MAX_VALUE && balance > 0;
        double remaining = balance - whole;
        data.putDouble(REMAINDER, clipped || Math.abs(remaining) < 1e-9 ? 0 : remaining);
        if (after != before) p.getStats().setValue(p, stat, after);
    }
    public static void tick(ServerPlayer p) {
        StorageActivity.tick(p);
        int state = PowerData.state(p, POWER);
        double attribute = PowerAttributes.strength(p, POWER);
        if (!configured() || !p.isAlive() || p.isSpectator() || state == 0 || attribute <= 0) { clear(p); return; }
        boolean storing = state < 0;
        double magnitude = storing
            ? FeruchemyLevels.storedFraction(attribute, -state, FeruchemyConfig.BRONZE_STORE_STEP.get(), FeruchemyConfig.BRONZE_STORE_PER_ATTRIBUTE.get(), FeruchemyConfig.MAX_STORE.get())
            : FeruchemyLevels.extraMass(state, FeruchemyConfig.BRONZE_TAP_STEP.get(), FeruchemyConfig.BRONZE_CAP.get());
        double cost = magnitude * (storing ? 1 : Math.pow(Math.max(1, magnitude), FeruchemyConfig.BRONZE_LOSS.get())) / 20;
        double moved = Metalminds.transferAccessible(p, "bronze", storing ? cost : -cost);
        double paid = cost > 0 ? Math.min(1, Math.abs(moved) / cost) : 0;
        if (paid <= 0) { PowerData.state(p, POWER, 0); clear(p); return; }
        double actual = magnitude * paid;
        ACTIVE.put(p, storing ? -actual : actual);
        drowsiness(p, storing && FeruchemyConfig.BRONZE_DROWSY.get() ? actual : 0);
        if (FeruchemyConfig.BRONZE_REST.get()) rest(p, (storing ? actual : -actual) * FeruchemyConfig.BRONZE_REST_RATE.get());
        BronzeAlertness.tick(p, storing ? 0 : actual / FeruchemyConfig.BRONZE_TAP_STEP.get());
        if (paid < 1 - 1e-9) PowerData.state(p, POWER, 0);
    }
    @SubscribeEvent public void death(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) clear(p); }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) { clear(p); PowerData.state(p, POWER, 0); } }
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) { clear(p); PowerData.state(p, POWER, 0); } }
}
