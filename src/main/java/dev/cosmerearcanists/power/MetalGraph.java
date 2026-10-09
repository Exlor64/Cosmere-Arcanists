package dev.cosmerearcanists.power;

import java.util.*;

/** Reverse recipe closure handles deep components, alternative recipes and cycles in linear time. */
public final class MetalGraph {
    public record Result(Set<String> metal, Set<String> aluminum) {
        public boolean detectable(String item) { return metal.contains(item) && !aluminum.contains(item); }
    }
    public static Result resolve(Map<String, Set<String>> ingredients, Set<String> metal, Set<String> aluminum) {
        Map<String, Set<String>> products = new HashMap<>();
        ingredients.forEach((output, inputs) -> inputs.forEach(input -> products.computeIfAbsent(input, k -> new HashSet<>()).add(output)));
        return new Result(closure(products, metal), closure(products, aluminum));
    }
    private static Set<String> closure(Map<String, Set<String>> products, Set<String> seeds) {
        Set<String> found = new HashSet<>(seeds);
        ArrayDeque<String> queue = new ArrayDeque<>(seeds);
        while (!queue.isEmpty()) for (String product : products.getOrDefault(queue.removeFirst(), Set.of()))
            if (found.add(product)) queue.addLast(product);
        return Set.copyOf(found);
    }
    private MetalGraph() { }
}
