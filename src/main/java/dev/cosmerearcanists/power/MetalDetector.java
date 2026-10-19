package dev.cosmerearcanists.power;

import java.util.*;
import dev.cosmerearcanists.CosmereArcanists;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class MetalDetector {
    private static MinecraftServer owner;
    private static MetalGraph.Result classification = new MetalGraph.Result(Set.of(), Set.of());
    private static Set<String> excluded = Set.of();
    public static void rebuild(MinecraftServer server) {
        owner = server;
        Set<String> metals = new HashSet<>(), aluminum = new HashSet<>(), deny = new HashSet<>();
        Set<String> metalNames = new HashSet<>();
        BuiltInRegistries.ITEM.forEach(item -> item.getDefaultInstance().getTags().forEach(tag -> {
            String path = tag.location().getPath();
            if (tag.location().getNamespace().equals("c") && (path.startsWith("ingots/") || path.startsWith("nuggets/")))
                metalNames.add(path.substring(path.indexOf('/') + 1));
        }));
        BuiltInRegistries.ITEM.forEach(item -> {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            item.getDefaultInstance().getTags().forEach(tag -> {
                var key = tag.location(); String path = key.getPath();
                if (key.getNamespace().equals("c") && (path.startsWith("ingots/") || path.startsWith("nuggets/")
                    || (path.startsWith("raw_materials/") || path.startsWith("ores/")) && metalNames.contains(path.substring(path.indexOf('/') + 1)))) metals.add(id);
                if (key.equals(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "metals"))) metals.add(id);
                if (key.equals(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "aluminum_containing"))
                    || path.endsWith("/aluminum") || path.endsWith("/aluminium")) aluminum.add(id);
                if (key.equals(ResourceLocation.fromNamespaceAndPath(CosmereArcanists.MOD_ID, "undetectable"))) deny.add(id);
            });
        });
        for (String id : List.of("minecraft:iron_ingot", "minecraft:iron_nugget", "minecraft:raw_iron", "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
            "minecraft:copper_ingot", "minecraft:raw_copper", "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
            "minecraft:gold_ingot", "minecraft:gold_nugget", "minecraft:raw_gold", "minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:nether_gold_ore",
            "minecraft:netherite_ingot", "minecraft:netherite_scrap", "minecraft:ancient_debris")) metals.add(id);
        Map<String, Set<String>> recipes = new HashMap<>();
        if (PowerConfig.RECIPE_DETECTION.get()) for (var holder : server.getRecipeManager().getRecipes()) {
            var recipe = holder.value();
            // Smelting recycling does not carry an alloy's ingredients back into purified metals.
            if (recipe.getType() != net.minecraft.world.item.crafting.RecipeType.CRAFTING
                && recipe.getType() != net.minecraft.world.item.crafting.RecipeType.SMITHING
                && recipe.getType() != net.minecraft.world.item.crafting.RecipeType.STONECUTTING) continue;
            var result = recipe.getResultItem(server.registryAccess());
            if (result.isEmpty()) continue;
            String output = id(result.getItem());
            var inputs = recipes.computeIfAbsent(output, k -> new HashSet<>());
            for (var ingredient : recipe.getIngredients()) for (var stack : ingredient.getItems()) if (!stack.isEmpty()) inputs.add(id(stack.getItem()));
        }
        classification = MetalGraph.resolve(recipes, metals, aluminum); excluded = Set.copyOf(deny);
        MetalAmounts.rebuild(server);
    }
    public static boolean detects(MinecraftServer server, ItemStack stack) {
        if (owner != server) rebuild(server);
        if(dev.cosmerearcanists.filing.Filings.dynamic(stack))return !excluded.contains(id(stack.getItem())) && dev.cosmerearcanists.filing.Filings.detectable(stack);
        return !stack.isEmpty() && classification.detectable(id(stack.getItem())) && !excluded.contains(id(stack.getItem()));
    }
    public static boolean containsAluminum(MinecraftServer server,ItemStack stack) {
        if(owner!=server)rebuild(server);
        return !stack.isEmpty() && classification.aluminum().contains(id(stack.getItem()));
    }
    public static void clear() { owner = null; classification = new MetalGraph.Result(Set.of(), Set.of()); excluded = Set.of(); MetalAmounts.clear(); }
    private static String id(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }
    private MetalDetector() { }
}
