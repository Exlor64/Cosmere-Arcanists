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