package dev.cosmerearcanists.power;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public final class MetalAmounts {
    private static Map<String,Double> amounts=Map.of();
    public static void rebuild(MinecraftServer server) {
        Map<String,Double> fixed=new HashMap<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            var stack=item.getDefaultInstance(); String id=BuiltInRegistries.ITEM.getKey(item).toString();
            if(!MetalDetector.detects(server,stack)){fixed.put(id,0.0);return;}
            if(stack.is(net.minecraft.world.item.Items.ANVIL)||stack.is(net.minecraft.world.item.Items.CHIPPED_ANVIL)||stack.is(net.minecraft.world.item.Items.DAMAGED_ANVIL)){fixed.put(id,279.0);return;}
            var tags=stack.getTags().map(t->t.location().getPath()).toList();
            String path=BuiltInRegistries.ITEM.getKey(item).getPath();
            if(path.endsWith("_nugget") || tags.stream().anyMatch(t->t.startsWith("nuggets/"))) fixed.put(id,1.0);
            else if(tags.stream().anyMatch(t->t.startsWith("storage_blocks/") && !t.startsWith("storage_blocks/raw_"))) fixed.put(id,81.0);
            else if(path.endsWith("_ingot") || path.startsWith("raw_") && !path.endsWith("_block") || path.endsWith("_ore")
                || tags.stream().anyMatch(t->t.startsWith("ingots/") || t.startsWith("raw_materials/") || t.startsWith("ores/"))) fixed.put(id,9.0);
        });
        var recipes=new ArrayList<RecipeMetalAmounts.Recipe>();
        if(PowerConfig.RECIPE_DETECTION.get()) for(var holder:server.getRecipeManager().getRecipes()) {
            var recipe=holder.value();
            if(recipe.getType()!=RecipeType.CRAFTING && recipe.getType()!=RecipeType.SMITHING && recipe.getType()!=RecipeType.STONECUTTING)continue;
            var output=recipe.getResultItem(server.registryAccess()); if(output.isEmpty())continue;
            var ingredients=new ArrayList<List<String>>();
            for(var ingredient:recipe.getIngredients()) {
                var options=Arrays.stream(ingredient.getItems()).filter(s->!s.isEmpty()).map(s->BuiltInRegistries.ITEM.getKey(s.getItem()).toString()).toList();
                if(!options.isEmpty())ingredients.add(options);
            }
            recipes.add(new RecipeMetalAmounts.Recipe(BuiltInRegistries.ITEM.getKey(output.getItem()).toString(),output.getCount(),ingredients));
        }
        amounts=RecipeMetalAmounts.resolve(recipes,fixed);
    }
    public static double units(MinecraftServer server,ItemStack stack) {
        if(!MetalDetector.detects(server,stack))return 0;
        if(dev.cosmerearcanists.currency.Coins.coin(stack))return dev.cosmerearcanists.currency.Coins.units(stack);
        if(dev.cosmerearcanists.filing.Filings.dynamic(stack) || dev.cosmerearcanists.filing.Filings.filed(stack))return dev.cosmerearcanists.filing.Filings.units(stack);
        double perItem=amounts.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),PowerConfig.UNKNOWN_METAL_UNITS.get());
        return Math.max(.001,perItem)*stack.getCount();
    }
    public static void clear(){amounts=Map.of();}
    private MetalAmounts() { }
}
