package dev.cosmerearcanists.treasure;

import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.*;
import dev.cosmerearcanists.feruchemy.*;
import dev.cosmerearcanists.fortune.FortuneConfig;
import dev.cosmerearcanists.hemalurgy.*;
import dev.cosmerearcanists.nicrosil.*;
import dev.cosmerearcanists.power.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TreasureGameTests {
    private static final ResourceKey<LootTable> LIBRARY = ResourceKey.create(Registries.LOOT_TABLE,
        ResourceLocation.parse("minecraft:chests/stronghold_library"));
    private static Item alloy() { return BuiltInRegistries.ITEM.get(ResourceLocation.parse("cosmerearcanists:atium_nugget")); }
    private static ItemStack band(String metal) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("cosmerearcanists:" + metal + "_metalmind_ring"))); }
    private static LootParams params(FakePlayer player) {
        return new LootParams.Builder(player.serverLevel()).withParameter(LootContextParams.ORIGIN, player.position())
            .withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.CHEST);
    }
    private static void configured(GameTestHelper h, Consumer<FakePlayer> test) {
        boolean enabled = TreasureConfig.ENABLED.get(), atium = TreasureConfig.ATIUM.get(), minds = TreasureConfig.METALMINDS.get(), spikes = TreasureConfig.SPIKES.get();
        int odds = TreasureConfig.ONE_IN.get();
        var tables = TreasureConfig.TABLES.get(); var dimensions = TreasureConfig.DIMENSIONS.get();
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "TreasureFinder"));
        p.setGameMode(GameType.SURVIVAL); PowerAttributes.migrate(p);
        try {
            TreasureConfig.ENABLED.set(true); TreasureConfig.ATIUM.set(true);
            TreasureConfig.METALMINDS.set(false); TreasureConfig.SPIKES.set(false);
            TreasureConfig.ONE_IN.set(1); TreasureConfig.TABLES.set(List.of(LIBRARY.location().toString()));
            TreasureConfig.DIMENSIONS.set(List.of()); test.accept(p);
        } finally {
            ChromiumFeruchemy.clear(p); NicrosilService.clear(p);
            TreasureConfig.ENABLED.set(enabled); TreasureConfig.ATIUM.set(atium); TreasureConfig.METALMINDS.set(minds); TreasureConfig.SPIKES.set(spikes);
            TreasureConfig.ONE_IN.set(odds); TreasureConfig.TABLES.set(tables); TreasureConfig.DIMENSIONS.set(dimensions);
        }
        h.succeed();
    }
    @GameTest(template = "empty") public static void registeredTreasureLootIsAdditiveAndChestCannotReroll(GameTestHelper h) {
        configured(h, p -> {
            var table = p.server.reloadableRegistries().getLootTable(LIBRARY);
            TreasureConfig.ENABLED.set(false);
            var baseline = table.getRandomItems(params(p), RandomSource.create(81));
            TreasureConfig.ENABLED.set(true);
            var bonus = table.getRandomItems(params(p), RandomSource.create(81));
            h.assertTrue(bonus.size() == baseline.size() + 1 && bonus.getLast().is(alloy()) && bonus.getLast().getCount() == 1,
                "Registered global loot modifier adds exactly one alloy nugget");
            for (int i = 0; i < baseline.size(); i++) h.assertTrue(ItemStack.matches(baseline.get(i), bonus.get(i)), "Original chest stacks remain unchanged");
            var pos = h.absolutePos(new BlockPos(2, 3, 2));
            h.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
            try {
                var chest = (ChestBlockEntity)h.getLevel().getBlockEntity(pos);
                chest.setLootTable(LIBRARY, 81); chest.unpackLootTable(p);
                var saved = new ArrayList<ItemStack>();
                for (int i = 0; i < chest.getContainerSize(); i++) saved.add(chest.getItem(i).copy());
                h.assertTrue(saved.stream().filter(stack -> stack.is(alloy())).mapToInt(ItemStack::getCount).sum() == 1,
                    "Native chest generation places the rare singleton");
                chest.unpackLootTable(p);
                h.assertTrue(chest.getLootTable() == null, "Native one-time loot reference is consumed");
                for (int i = 0; i < saved.size(); i++) h.assertTrue(ItemStack.matches(saved.get(i), chest.getItem(i)), "Reopening never rerolls treasure");
            } finally { h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
        });
    }
    @GameTest(template = "empty") public static void tableDimensionAndCategoryGatesDoNotProduceJunk(GameTestHelper h) {
        configured(h, p -> {
            var table = p.server.reloadableRegistries().getLootTable(LIBRARY);
            TreasureConfig.TABLES.set(List.of());
            h.assertTrue(table.getRandomItems(params(p), RandomSource.create(17)).stream().noneMatch(s -> s.is(alloy())), "Unlisted tables receive no treasure");
            TreasureConfig.TABLES.set(List.of(LIBRARY.location().toString()));
            TreasureConfig.DIMENSIONS.set(List.of("cosmerearcanists:missing_test_dimension"));
            h.assertTrue(table.getRandomItems(params(p), RandomSource.create(17)).stream().noneMatch(s -> s.is(alloy())), "Dimension filter applies");
            TreasureConfig.DIMENSIONS.set(List.of()); TreasureConfig.ATIUM.set(false);
            h.assertTrue(TreasureItems.generate(h.getLevel(), RandomSource.create(17)).isEmpty(), "All categories disabled yields nothing");
            boolean steel = ServerConfig.FERUCHEMY.metals().get(Metal.STEEL).get();
            try {
                ServerConfig.FERUCHEMY.metals().get(Metal.STEEL).set(false);
                h.assertTrue(TreasureItems.metalmind(h.getLevel(), RandomSource.create(1), "steel", "ring").isEmpty(), "Disabled powers are not generated");
            } finally { ServerConfig.FERUCHEMY.metals().get(Metal.STEEL).set(steel); }
        });
    }
    @GameTest(template = "empty") public static void everyMetalmindHasValidRandomChargeAndUsableUnkeyedBinding(GameTestHelper h) {
        configured(h, p -> {
            var random = RandomSource.create(987);
            for (String metal : TreasureItems.metals()) for (String slot : List.of("ring", "bracelet", "necklace")) {
                var item = TreasureItems.metalmind(h.getLevel(), random, metal, slot);
                try {
                    h.assertTrue(item.getCount() == 1 && Metalminds.bound(item) && Metalminds.unkeyed(item)
                        && Metalminds.owner(item) == null && Metalminds.charge(item) > 0 && Metalminds.charge(item) <= Metalminds.capacity(item),
                        "Generated " + metal + " " + slot + " has positive bounded unkeyed charge");
                    h.assertTrue(Metalminds.usableBy(item, p, Metalminds.storedMetal(item)), "Ordinary access honors the neutral binding");
                    if (!metal.equals("nicrosil")) h.assertTrue(Metalminds.transferFor(item, p, Metalminds.storedMetal(item), -.001) < 0,
                        "Charge is real and transferable through the normal metalmind boundary");
                } finally { if (NicrosilService.specialized(item)) NicrosilService.eraseItem(item); }
            }
        });
    }
    @GameTest(template = "empty") public static void spikePowersUseCompatibleMaterialsAndUniqueSavedSources(GameTestHelper h) {
        configured(h, p -> {
            var random = RandomSource.create(515);
            var ids = new HashSet<UUID>(); var donors = new HashSet<UUID>();
            for (String power : TreasureItems.spikePowers()) {
                var item = TreasureItems.spike(random, power); var charge = HemalurgicSpikes.contents(item);
                h.assertTrue(item.getCount() == 1 && charge != null && charge.power().equals(power)
                    && HemalurgyRules.accepts(charge.metal(), power), "Treasure spike material is compatible with its actual power");
                h.assertTrue(charge.strength() >= TreasureConfig.MIN_STRENGTH.get() && charge.strength() <= TreasureConfig.MAX_STRENGTH.get()
                    && ids.add(charge.id()) && donors.add(charge.donor()), "Random strength and distinct anti-duplication/source Identities are preserved");
                var loaded = ItemStack.parse(h.getLevel().registryAccess(), item.save(h.getLevel().registryAccess())).orElseThrow();
                h.assertTrue(charge.equals(HemalurgicSpikes.contents(loaded)), "Native item serialization preserves all spike charge data");
            }
            h.assertTrue(TreasureItems.spike(random, "trait:unsupported").isEmpty(), "Excluded spike effects are never invented");
        });
    }
    @GameTest(template = "empty") public static void treasureNicrosilPersistsAndTransfersOnceToAnUnpoweredFinder(GameTestHelper h) {
        configured(h, p -> {
            var item = band("nicrosil");
            h.assertTrue(NicrosilService.initializeUnkeyedPower(p.server, item, "a:steel", .75), "Treasure initializer creates authoritative unkeyed power contents");
            var copy = item.copy(); var mind = NicrosilService.contents(p, item); var entry = mind.entries().getFirst();
            try {
                var saved = NicrosilData.get(p.server).save(new CompoundTag(), h.getLevel().registryAccess());
                var loaded = NicrosilData.load(saved, h.getLevel().registryAccess()).ledger.get(mind.id);
                h.assertTrue(loaded.owner == null && loaded.entry(entry.id()).amount() == .75
                    && loaded.entry(entry.id()).sourceIdentity() == null, "Neutral power/source and charge survive SavedData roundtrip");
                p.setItemInHand(InteractionHand.OFF_HAND, item);
                var session = NicrosilService.open(p, item);
                h.assertTrue(session != null && PowerAttributes.strength(p, "f:nicrosil") == 0, "Unpowered finder can access a treasure nicrosilmind");
                NicrosilService.tap(session, entry.id());
                h.assertTrue(PowerAttributes.baseStrength(p, "a:steel") == .75 && NicrosilService.contents(p, copy) == null,
                    "Actual tapping grants strength once and consumes the shared record, including copied-item references");
            } finally {
                NicrosilService.clear(p); if (NicrosilService.specialized(item)) NicrosilService.eraseItem(item);
            }
        });
    }
    @GameTest(template = "empty") public static void nicrosilInitializationRejectsOverwriteAndLedgerOverflow(GameTestHelper h) {
        configured(h, p -> {
            var item = band("nicrosil");
            h.assertTrue(NicrosilService.initializeUnkeyedPower(p.server, item, "f:gold", .5), "First treasure content is valid");
            try {
                var before = NicrosilService.contents(p, item).entries();
                h.assertTrue(!NicrosilService.initializeUnkeyedPower(p.server, item, "a:steel", 1)
                    && NicrosilService.contents(p, item).entries().equals(before), "Existing contents cannot be overwritten by the acquisition integration");
                int limit = NicrosilConfig.MINDS.get();
                try {
                    NicrosilConfig.MINDS.set(NicrosilData.get(p.server).ledger.size());
                    var empty = band("nicrosil");
                    h.assertTrue(!NicrosilService.initializeUnkeyedPower(p.server, empty, "a:steel", .5)
                        && !Metalminds.bound(empty) && !NicrosilService.specialized(empty), "A full ledger never creates an orphaned charged item");
                } finally { NicrosilConfig.MINDS.set(limit); }
            } finally { NicrosilService.eraseItem(item); }
        });
    }
    @GameTest(template = "empty") public static void fortuneExtraTableSamplesDoNotMultiplyTheRareRoll(GameTestHelper h) {
        configured(h, p -> {
            PowerAttributes.set(p, "f:chromium", 1); var ring = band("chromium");
            p.setItemInHand(InteractionHand.OFF_HAND, ring); Metalminds.transfer(ring, p.getUUID(), "chromium", 100);
            double rate = FortuneConfig.BONUS_STEP.get();
            try {
                FortuneConfig.BONUS_STEP.set(1.0); PowerData.state(p, "f:chromium", 10); ChromiumFeruchemy.tick(p);
                var loot = p.server.reloadableRegistries().getLootTable(LIBRARY).getRandomItems(params(p), RandomSource.create(18));
                h.assertTrue(loot.stream().filter(s -> s.is(alloy())).mapToInt(ItemStack::getCount).sum() == 1,
                    "Fortune's bounded raw-table rerolls still produce exactly one rare treasure roll");
            } finally { FortuneConfig.BONUS_STEP.set(rate); ChromiumFeruchemy.clear(p); }
        });
    }
}
