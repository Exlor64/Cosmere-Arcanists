package dev.cosmerearcanists.power;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipeMetalAmountsTest {
    @Test void countsRepeatedIngredientsOutputsAndNestedMetalBlocks() {
        var recipes=List.of(
            new RecipeMetalAmounts.Recipe("anvil",1,List.of(List.of("block"),List.of("block"),List.of("block"),List.of("ingot"),List.of("ingot"),List.of("ingot"),List.of("ingot"))),
            new RecipeMetalAmounts.Recipe("block",1,Collections.nCopies(9,List.of("ingot"))),
            new RecipeMetalAmounts.Recipe("bars",16,Collections.nCopies(6,List.of("ingot"))));
        var result=RecipeMetalAmounts.resolve(recipes,Map.of("ingot",9.0));
        assertEquals(81,result.get("block")); assertEquals(279,result.get("anvil")); assertEquals(54.0/16,result.get("bars"));
    }
    @Test void ignoresNonmetalAndCannotInventMetalFromCycles() {
        var result=RecipeMetalAmounts.resolve(List.of(
            new RecipeMetalAmounts.Recipe("sword",1,List.of(List.of("ingot"),List.of("ingot"),List.of("stick"))),
            new RecipeMetalAmounts.Recipe("a",1,List.of(List.of("b"))),new RecipeMetalAmounts.Recipe("b",1,List.of(List.of("a")))),Map.of("ingot",9.0,"stick",0.0));
        assertEquals(18,result.get("sword")); assertFalse(result.containsKey("a"));
    }
}
