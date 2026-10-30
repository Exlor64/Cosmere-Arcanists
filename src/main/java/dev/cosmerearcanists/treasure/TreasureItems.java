package dev.cosmerearcanists.treasure;

import dev.cosmerearcanists.*;
import dev.cosmerearcanists.compounding.IdentityMath;
import dev.cosmerearcanists.equipment.EquipmentContent;
import dev.cosmerearcanists.feruchemy.*;
import dev.cosmerearcanists.hemalurgy.*;
import dev.cosmerearcanists.nicrosil.*;
import dev.cosmerearcanists.power.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** Builds real singleton items using the ordinary ownership/charge/source formats. */
public final class TreasureItems {
    public static List<String> powers() {
        return PowerConfig.ENABLED.get() ? PowerAttributes.keys().stream()
            .filter(PowerData::powerEnabled)
            .filter(power -> !power.startsWith("f:") || FeruchemyConfig.ENABLED.get())
            .sorted().toList() : List.of();
    }
    public static List<String> metals() {
        if (!PowerConfig.ENABLED.get() || !FeruchemyConfig.ENABLED.get()) return List.of();
        return Arrays.stream(Metal.values()).map(Metal::id).filter(metal -> PowerData.powerEnabled("f:" + metal))
            .filter(metal -> !metal.equals("nicrosil") || NicrosilService.configured()
                && NicrosilConfig.POWERS.get() && !powers().isEmpty()).toList();
    }
    public static List<String> spikePowers() {
        return powers().stream().filter(power -> HemalurgyRules.POWERS.entrySet().stream()
            .anyMatch(entry -> HemalurgyService.enabled(entry.getKey()) && entry.getValue().contains(power))).toList();
    }
    private static double between(RandomSource random, double first, double second) {
        return Math.min(first, second) + random.nextDouble() * Math.abs(second - first);
    }
    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, id)));
    }
    public static ItemStack metalmind(ServerLevel level, RandomSource random, String metal, String slot) {
        if (!metals().contains(metal) || !List.of("ring", "bracelet", "necklace").contains(slot)) return ItemStack.EMPTY;
        var result = item(metal + "_metalmind_" + slot);
        if (metal.equals("nicrosil")) {
            var pool = powers();
            String power = pool.get(random.nextInt(pool.size()));
            double strength = Math.min(between(random, TreasureConfig.MIN_STRENGTH.get(), TreasureConfig.MAX_STRENGTH.get()),
                Metalminds.capacity(result) / NicrosilConfig.POWER_CHARGE.get());
            return NicrosilService.initializeUnkeyedPower(level.getServer(), result, power, strength) ? result : ItemStack.EMPTY;
        }
        String channel = metal.equals("tin") ? List.of("tin:sight", "tin:hearing", "tin:touch").get(random.nextInt(3)) : metal;
        double charge = Metalminds.capacity(result) * between(random, TreasureConfig.MIN_FILL.get(), TreasureConfig.MAX_FILL.get());
        return Metalminds.transferWithBinding(result, null, channel, charge, true) > 0 ? result : ItemStack.EMPTY;
    }
    public static ItemStack spike(RandomSource random, String power) {
        if (!spikePowers().contains(power)) return ItemStack.EMPTY;
        String metal = HemalurgyRules.POWERS.entrySet().stream()
            .filter(entry -> HemalurgyService.enabled(entry.getKey()) && entry.getValue().contains(power))
            .map(Map.Entry::getKey).sorted().findFirst().orElseThrow();
        var result = new ItemStack(EquipmentContent.SPIKES.get(metal).get());
        UUID donor = UUID.randomUUID();
        var charge = new HemalurgicSpikes.Charge(UUID.randomUUID(), donor, metal, power,
            List.of(new IdentityMath.Portion(donor, between(random, TreasureConfig.MIN_STRENGTH.get(), TreasureConfig.MAX_STRENGTH.get()))));
        return HemalurgicSpikes.store(result, charge) ? result : ItemStack.EMPTY;
    }
    public static ItemStack generate(ServerLevel level, RandomSource random) {
        var categories = new ArrayList<String>();
        if (TreasureConfig.ATIUM.get() && MetallurgyConfig.GOD_METALS.get("atium").get()) categories.add("atium");
        var metals = metals(); var powers = spikePowers();
        if (TreasureConfig.METALMINDS.get() && !metals.isEmpty()) categories.add("mind");
        if (TreasureConfig.SPIKES.get() && !powers.isEmpty()) categories.add("spike");
        if (categories.isEmpty()) return ItemStack.EMPTY;
        return switch (categories.get(random.nextInt(categories.size()))) {
            case "atium" -> item("atium_nugget"); // Existing atium item represents the atium-electrum alloy.
            case "mind" -> metalmind(level, random, metals.get(random.nextInt(metals.size())),
                List.of("ring", "bracelet", "necklace").get(random.nextInt(3)));
            default -> spike(random, powers.get(random.nextInt(powers.size())));
        };
    }
    private TreasureItems() {}
}
