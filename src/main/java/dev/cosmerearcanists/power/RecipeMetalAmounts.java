package dev.cosmerearcanists.power;

import java.util.*;

/** Nugget-equivalent metal content, respecting ingredient multiplicity and recipe output counts. */
public final class RecipeMetalAmounts {
    public record Recipe(String output, int count, List<List<String>> ingredients) { }
    public static Map<String, Double> resolve(List<Recipe> recipes, Map<String, Double> fixed) {
        var values = new HashMap<>(fixed);
        for (int pass=0;pass<64;pass++) {
            boolean changed=false;
            for (var recipe:recipes) {
                if(fixed.containsKey(recipe.output()) || recipe.count()<=0) continue;
                double sum=0; boolean known=true;
                for(var ingredient:recipe.ingredients()) {
                    double minimum=Double.POSITIVE_INFINITY;
                    for(String option:ingredient) if(values.containsKey(option)) minimum=Math.min(minimum,values.get(option));
                    if(!Double.isFinite(minimum)){known=false;break;}
                    sum+=minimum;
                }
                if(!known)continue;
                double amount=Math.min(1e9,sum/recipe.count());
                double old=values.getOrDefault(recipe.output(),Double.POSITIVE_INFINITY);
                if(amount<old-1e-9){values.put(recipe.output(),amount);changed=true;}
            }
            if(!changed)break;
        }
        return Map.copyOf(values);
    }
    private RecipeMetalAmounts() { }
}
