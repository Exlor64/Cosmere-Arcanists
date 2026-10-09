package dev.cosmerearcanists.power;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import dev.cosmerearcanists.CosmereArcanists;
import dev.cosmerearcanists.OriginData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PowerGameTests {
    @GameTest(template = "empty")
    public static void hardnessBlocksDetection(GameTestHelper helper) {
        var level = helper.getLevel(); var server = level.getServer();
        var player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "Resistance"), ClientInformation.createDefault());
        var start = helper.absolutePos(new net.minecraft.core.BlockPos(1, 3, 1));
        var end = start.offset(4, 0, 0);
        var from = net.minecraft.world.phys.Vec3.atCenterOf(start); var to = net.minecraft.world.phys.Vec3.atCenterOf(end);
        for (int x=0;x<=4;x++) level.setBlock(start.offset(x,0,0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(end, net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState(), 3);
        helper.assertTrue(MetalResistance.cost(player,from,to)==0, "Target metal must not resist itself");
        level.setBlock(start.offset(2,0,0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        double stone=MetalResistance.cost(player,from,to);
        helper.assertTrue(stone>0, "Solid stone must resist detection");
        level.setBlock(start.offset(2,0,0), net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(), 3);
        double obsidian=MetalResistance.cost(player,from,to);
        helper.assertTrue(obsidian>stone, "Harder blocks must provide more resistance");
        helper.assertTrue(from.distanceTo(to)+obsidian>PowerConfig.RANGE.get(), "Obsidian must hide a distant target at default range");
        level.setBlock(start.offset(1,0,0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(MetalResistance.cost(player,from,to)>obsidian, "Multiple blockers must accumulate");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void nearFirstBlockScanning(GameTestHelper helper) {
        var offsets = PowerService.scanOffsets(16);
        helper.assertTrue(offsets.getFirst().equals(net.minecraft.core.BlockPos.ZERO), "Nearby blocks must be scanned first");
        helper.assertTrue(new java.util.HashSet<>(offsets).size() == offsets.size(), "Scan positions must be unique");
        helper.assertTrue(offsets.size() < 18000, "Scan budget must not be wasted on cube corners");
        double previous = -1;
        for (var offset : offsets) {
            double distance = offset.distSqr(net.minecraft.core.BlockPos.ZERO);
            helper.assertTrue(distance >= previous && distance <= 256, "Scan must progress outward inside the configured sphere");
            previous = distance;
        }
        helper.assertTrue(offsets.contains(new net.minecraft.core.BlockPos(16, 0, 0)) && offsets.contains(new net.minecraft.core.BlockPos(-16, 0, 0)), "Both range edges must be scanned");
        var server = helper.getLevel().getServer();
        var player = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "ScanMovement"), ClientInformation.createDefault());
        var start = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        player.setPos(start.getX() + .5, start.getY(), start.getZ() + .5);
        var metal = start.offset(1, 0, 0);
        helper.getLevel().setBlock(metal, net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState(), 3);
        var session = new PowerService.Session();
        PowerService.scan(player, session);
        helper.assertTrue(session.blocks.contains(metal), "Nearby discoveries must publish before a pass finishes");
        int cursor = session.cursor;
        player.setPos(player.getX() + 5, player.getY(), player.getZ());
        PowerService.scan(player, session);
        helper.assertTrue(session.cursor > cursor, "Moving five blocks must not restart the scan");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void recipeDetection(GameTestHelper helper) {
        var server = helper.getLevel().getServer(); MetalDetector.rebuild(server);
        helper.assertTrue(MetalDetector.detects(server, new ItemStack(Items.IRON_INGOT)), "Iron must be detectable");
        helper.assertTrue(MetalDetector.detects(server, new ItemStack(Items.IRON_SWORD)), "Crafted metal equipment must be detectable");
        helper.assertTrue(MetalDetector.detects(server, new ItemStack(Items.ANVIL)), "Crafted metal blocks must be detectable");
        helper.assertTrue(!MetalDetector.detects(server, new ItemStack(Items.COAL)), "Coal is not metal");
        helper.assertTrue(!MetalDetector.detects(server, new ItemStack(Items.DIAMOND_ORE)), "Nonmetal ores must not become metal seeds");
        for (String id : new String[]{"aluminum_ingot", "duralumin_ingot", "aluminum_ore"})
            helper.assertTrue(!MetalDetector.detects(server, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, id)))), "Aluminum-containing " + id + " must be excluded");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void playerStatePersistence(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var original = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "TestOriginal"), ClientInformation.createDefault());
        var clone = new ServerPlayer(server, helper.getLevel(), new GameProfile(original.getUUID(), "TestOriginal"), ClientInformation.createDefault());
        new OriginData("scadrial", "human", "iron", "steel").save(original);
        PowerData.grant(original, "a:steel", true);
        PowerData.data(original).putString("selected", "a:steel");
        PowerData.state(original, "a:steel", 2); PowerData.reserve(original, "steel", 42);
        OriginData.copy(original, clone); PowerData.clonePlayer(original, clone);
        helper.assertTrue(PowerData.available(clone).containsAll(java.util.List.of("a:iron", "a:steel", "f:steel")), "Natural and granted powers must survive cloning");
        helper.assertTrue(PowerData.selected(clone).equals("a:steel"), "Selection must survive cloning");
        helper.assertTrue(PowerData.reserve(clone, "steel") == 42, "Reserves must survive cloning");
        helper.assertTrue(PowerData.state(clone, "a:steel") == 0, "States must be off after respawn");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void persistentPowerAttributes(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var original = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "Attributes"), ClientInformation.createDefault());
        var clone = new ServerPlayer(server, helper.getLevel(), new GameProfile(original.getUUID(), "Attributes"), ClientInformation.createDefault());
        new OriginData("scadrial", "human", "steel", "iron").save(original);
        var legacy = new net.minecraft.nbt.CompoundTag(); legacy.putBoolean("a:iron", true);
        PowerData.data(original).put("grants", legacy);
        helper.assertTrue(PowerData.available(original).containsAll(java.util.List.of("a:steel", "a:iron", "f:iron")), "Legacy powers must migrate");
        var attribute = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "allomancy_steel")).orElseThrow();
        var instance = original.getAttribute(attribute);
        instance.setBaseValue(2);
        instance.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "test_strength"), 0.5,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        helper.assertTrue(PowerAttributes.strength(original, "a:steel") == 2.5, "Effective strength must include modifiers");
        PowerData.clonePlayer(original, clone);
        helper.assertTrue(PowerAttributes.strength(clone, "a:steel") == 2.5, "Base and permanent modifiers must survive respawn/save-load");
        instance.removeModifiers(); instance.setBaseValue(0);
        helper.assertTrue(!PowerData.available(original).contains("a:steel"), "Origin metadata must not restore a zero attribute");
        PowerData.clonePlayer(original, clone);
        helper.assertTrue(!PowerData.available(clone).contains("a:steel"), "Revocation must survive respawn");
        helper.assertTrue(PowerAttributes.keys().size() == 32, "All Metallic Art attributes must be registered");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void commandsParseNamespacedPowerIds(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        for (String command : new String[]{"cosmere power grant TestPlayer a:steel", "cosmere power revoke TestPlayer f:iron", "attribute @s cosmerearcanists:allomancy_steel base set 2"}) {
            var parsed = dispatcher.parse(command, server.createCommandSourceStack());
            helper.assertTrue(parsed.getExceptions().isEmpty() && !parsed.getReader().canRead(), "Command must consume the full namespaced power ID: " + command);
        }
        helper.succeed();
    }
}