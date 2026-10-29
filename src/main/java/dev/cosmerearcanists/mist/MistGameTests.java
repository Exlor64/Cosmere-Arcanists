package dev.cosmerearcanists.mist;

import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.feruchemy.*;
import dev.cosmerearcanists.power.*;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MistGameTests {
    private static void inMist(GameTestHelper h, Consumer<FakePlayer> test) {
        var dimensions = MistConfig.DIMENSIONS.get();
        double chance = MistConfig.CHANCE.get();
        boolean enabled = MistConfig.ENABLED.get(), benefits = MistConfig.BENEFITS.get();
        long time = h.getLevel().getDayTime();
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "MistWalker"));
        p.setGameMode(GameType.SURVIVAL);
        PowerAttributes.migrate(p);
        try {
            MistConfig.DIMENSIONS.set(List.of(h.getLevel().dimension().location().toString()));
            MistConfig.CHANCE.set(1.0); MistConfig.ENABLED.set(true); MistConfig.BENEFITS.set(true);
            h.getLevel().setDayTime(18000);
            test.accept(p);
        } finally {
            DuraluminFeruchemy.clear(p);
            MistConfig.DIMENSIONS.set(dimensions); MistConfig.CHANCE.set(chance);
            MistConfig.ENABLED.set(enabled); MistConfig.BENEFITS.set(benefits);
            h.getLevel().setDayTime(time);
        }
        h.succeed();
    }
    @GameTest(template = "empty") public static void independentNightsAllowStreaksAndSurviveReloads(GameTestHelper h) {
        int misty = 0, clearStreaks = 0, mistStreaks = 0;
        boolean previous = false;
        for (int day = 0; day < 10000; day++) {
            double density = MistMath.weather(928374, day * 24000L + 18000, .5);
            boolean active = density > 0;
            if (active) misty++;
            if (day > 0 && active == previous) { if (active) mistStreaks++; else clearStreaks++; }
            previous = active;
            h.assertTrue(density == MistMath.weather(928374, day * 24000L + 18000, .5), "Reloading cannot reroll a night");
            h.assertTrue(MistMath.weather(928374, day * 24000L + 6000, 1) == 0, "Daylight is clear even at100%chance");
        }
        h.assertTrue(misty > 4500 && misty < 5500 && clearStreaks > 0 && mistStreaks > 0, "Independent nights average half, with both types of streak");
        h.assertTrue(MistMath.weather(1, 18000, 0) == 0 && MistMath.weather(1, 18000, 1) > 0, "Chance endpoints work");
        h.succeed();
    }
    @GameTest(template = "empty") public static void sightGraduallyPenetratesMistWithoutWeakeningOtherFog(GameTestHelper h) {
        double previous = 1;
        for (int sight = 0; sight <= 6; sight++) {
            double density = MistMath.perceivedDensity(1, sight, 6);
            h.assertTrue(density <= previous, "Sight strength monotonically reduces perceived mist"); previous = density;
        }
        h.assertTrue(previous == 0 && MistMath.perceivedDensity(.8, 0, 6) == .8, "Ordinary vision sees full mist; strong vision clears it");
        h.assertTrue(MistMath.farPlane(10, 24, 1) == 10 && MistMath.farPlane(200, 24, 1) == 24,
            "Mist never extends blindness/darkness or render distance");
        inMist(h, p -> {
            double ordinary = MistService.snapshot(p).density();
            p.getAttribute(PowerAttributes.SIGHT).setBaseValue(2);
            h.assertTrue(MistService.snapshot(p).density() < ordinary, "Actual enhanced sight affects server-computed visibility");
        });
    }
    @GameTest(template = "empty") public static void nativeConnectionSupplyIsTemporaryAndDimensionScoped(GameTestHelper h) {
        inMist(h, p -> {
            PowerAttributes.set(p, "a:steel", 1);
            new OriginData("scadrial", "human", "steel", "").save(p);
            h.assertTrue(MistBenefits.tier(p) == 0, "Ordinary Scadrian ancestry alone is not strong Connection");
            p.getAttribute(PowerAttributes.SCADRIAL_CONNECTION).setBaseValue(1.15);
            PowerData.reserve(p, "steel", .1);
            EnhancementService.consume(p, "steel", 10);
            h.assertTrue(PowerData.reserve(p, "steel") == .1, "Strong Connection prevents consumption even for costs exceeding remaining metal");
            h.assertTrue(PowerData.canSpendReserve(p, "steel", 10), "Free abilities remain usable with a nonzero seed reserve");
            PowerData.reserve(p, "steel", 0);
            h.assertTrue(PowerData.reserve(p, "steel") == 0 && !PowerData.canSpendReserve(p, "steel", 10), "Free use does not create missing reserves");
            p.getAttribute(PowerAttributes.SCADRIAL_CONNECTION).setBaseValue(4);
            h.assertTrue(PowerData.reserve(p, "steel") == PowerConfig.RESERVE_CAP.get(), "Very strong Connection supplies empty known-metal reserves");
            h.assertTrue(PowerData.reserve(p, "iron") == 0, "Ambient reserves never grant unknown powers");
            PowerData.spendReserve(p, "steel", 50);
            h.getLevel().setDayTime(6000);
            h.assertTrue(PowerData.reserve(p, "steel") == 0 && MistBenefits.tier(p) == 0, "Virtual supply disappears in daylight without banking free reserves");
            h.getLevel().setDayTime(18000);
            MistConfig.DIMENSIONS.set(List.of());
            h.assertTrue(MistBenefits.tier(p) == 0 && MistService.snapshot(p).density() == 0, "Other dimensions do not supply mist or reserves");
        });
    }
    @GameTest(template = "empty") public static void fundedDuraluminRequiresContactAndLevelTwentyForInfiniteFuel(GameTestHelper h) {
        inMist(h, p -> {
            PowerAttributes.set(p, "a:steel", 1); PowerAttributes.set(p, "f:duralumin", 2);
            var mind = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("cosmerearcanists:duralumin_metalmind_ring")));
            p.setItemInHand(InteractionHand.OFF_HAND, mind);
            Metalminds.transfer(mind, p.getUUID(), "duralumin", 100);
            PowerData.state(p, "f:duralumin", 1); DuraluminFeruchemy.tick(p);
            h.assertTrue(MistBenefits.tier(p) == 1, "Funded tap1 provides free metals even without native Scadrian origin");
            PowerData.state(p, "f:duralumin", 19); DuraluminFeruchemy.tick(p);
            h.assertTrue(MistBenefits.tier(p) == 1, "Tap19 remains below infinite threshold");
            PowerData.state(p, "f:duralumin", 20); DuraluminFeruchemy.tick(p);
            h.assertTrue(MistBenefits.tier(p) == 2 && Metalminds.charge(mind) < 100, "Tap20 supplies infinite reserves while still spending Connection charge");
            p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            h.assertTrue(MistBenefits.tier(p) == 0, "Losing metalmind contact immediately ends tap-based benefits");
        });
    }
    @GameTest(template = "empty") public static void freeMistSupplyCoversCompoundingAndStillHonorsPurges(GameTestHelper h) {
        inMist(h, p -> {
            PowerAttributes.set(p, "a:steel", 1); PowerAttributes.set(p, "f:steel", 1);
            p.getAttribute(PowerAttributes.SCADRIAL_CONNECTION).setBaseValue(1.15);
            var mind = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("cosmerearcanists:steel_metalmind_ring")));
            p.setItemInHand(InteractionHand.OFF_HAND, mind);
            Metalminds.transfer(mind, p.getUUID(), "steel", 1);
            PowerData.reserve(p, "steel", 1);
            PowerData.state(p, "a:steel", -2);
            dev.cosmerearcanists.compounding.CompoundingService.tick(p);
            h.assertTrue(Metalminds.charge(mind) > 1 && PowerData.reserve(p, "steel") == 1,
                "Compounding produces charge at the existing reduced rate without consuming mist-supported metal");
            PowerData.reserve(p, "steel", 0);
            h.assertTrue(PowerData.reserve(p, "steel") == 0, "Free spending does not block explicit aluminum/chromium reserve clearing");
        });
    }

    @GameTest(template = "empty") public static void disabledWeatherAndSuppressionDoNotSupplyMetals(GameTestHelper h) {
        inMist(h, p -> {
            PowerAttributes.set(p, "a:steel", 1); p.getAttribute(PowerAttributes.SCADRIAL_CONNECTION).setBaseValue(4);
            MistConfig.ENABLED.set(false);
            h.assertTrue(MistBenefits.tier(p) == 0 && MistService.snapshot(p).density() == 0, "Disabled mist is inactive");
            MistConfig.ENABLED.set(true); MistConfig.CHANCE.set(0.0);
            h.assertTrue(MistBenefits.tier(p) == 0, "Clear nights grant no benefit");
            MistConfig.CHANCE.set(1.0);
            dev.cosmerearcanists.hemalurgy.AluminumSuppression.apply(p);
            h.assertTrue(MistBenefits.tier(p) == 0 && PowerData.reserve(p, "steel") == 0, "Aluminum suppression cannot be bypassed by mist");
        });
    }
}
