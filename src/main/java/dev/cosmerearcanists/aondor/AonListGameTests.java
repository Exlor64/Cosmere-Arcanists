package dev.cosmerearcanists.aondor;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.copper.*;
import dev.cosmerearcanists.feruchemy.Metalminds;
import dev.cosmerearcanists.power.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import static dev.cosmerearcanists.aondor.AonProgram.Type;

@GameTestHolder(dev.cosmerearcanists.CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AonListGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AonLists"));p.setPos(h.absolutePos(new BlockPos(2,22,2)).getCenter());return p;}
    private static AonProgram.Node node(String key,Type type,AonProgram.Node... args){return new AonProgram.Node("cosmerearcanists:"+key,0,type,List.of(args));}
    private static AonProgram.Node number(double n){return new AonProgram.Node("cosmerearcanists:number",n,Type.NUMBER,List.of());}
    private static AonProgram.Node list(AonProgram.Node... values){return node("list",Type.LIST,values);}
    private static AonProgram.Node numbers(double... numbers){return list(Arrays.stream(numbers).mapToObj(AonListGameTests::number).toArray(AonProgram.Node[]::new));}
    private static AonProgram.Node recall(double key){return node("recall",Type.VALUE,number(key));}
    private static boolean rejects(Runnable action){try{action.run();return false;}catch(IllegalArgumentException expected){return true;}}
    private static AonEvaluator.Value numeric(double n){return new AonEvaluator.Value(Type.NUMBER,n);}
    @GameTest(template="empty") public static void createAppendRemoveAndIndexPreserveTypedValues(GameTestHelper h){
        var p=player(h);var caster=node("caster",Type.ENTITY);var original=list(number(3),caster,number(3));
        h.assertTrue(AonEvaluator.value(p,list()).equals(List.of()),"List creates an empty list");
        h.assertTrue(AonEvaluator.value(p,node("at",Type.VALUE,original,number(1)))==p,"At returns the actual entity type from a generic list");
        h.assertTrue(AonEvaluator.value(p,node("at",Type.VALUE,original,number(0))).equals(3.0),"Indexing starts at zero");
        var appended=node("append",Type.LIST,original,number(8),list(number(5)));
        h.assertTrue(AonEvaluator.value(p,node("size",Type.NUMBER,appended)).equals(5.0),"Append adds values including a nested list without flattening");
        var removed=node("remove_at",Type.LIST,original,number(1));h.assertTrue(AonEvaluator.value(p,removed).equals(List.of(numeric(3),numeric(3)))&&((List<?>)AonEvaluator.value(p,original)).size()==3,"Removal preserves other duplicate entries and does not mutate the original");
        for(double bad:new double[]{-1,.5,3})h.assertTrue(rejects(()->AonEvaluator.value(p,node("at",Type.VALUE,original,number(bad)))),"Invalid index rejects: "+bad);h.succeed();
    }
    @GameTest(template="empty") public static void multiwaySetsHaveStableUniqueOutputs(GameTestHelper h){
        var p=player(h);var a=numbers(3,1,3,2);var b=numbers(2,3,4);var c=numbers(3,5);
        h.assertTrue(AonEvaluator.value(p,node("intersection",Type.LIST,a,b,c)).equals(List.of(numeric(3))),"Intersection accepts three lists and keeps entries shared by all");
        h.assertTrue(AonEvaluator.value(p,node("union",Type.LIST,a,b,c)).equals(List.of(numeric(3),numeric(1),numeric(2),numeric(4),numeric(5))),"Union is unique and preserves first encounter order");
        h.assertTrue(AonEvaluator.value(p,node("difference",Type.LIST,a,b,c)).equals(List.of(numeric(1))),"Difference removes entries in any subsequent list");
        h.assertTrue(AonEvaluator.value(p,node("contains",Type.BOOLEAN,numbers(-0.0),number(0))).equals(true),"Numeric membership treats positive and negative zero alike");h.succeed();
    }
    @GameTest(template="empty") public static void conditionalsAndBooleanOperatorsSkipUnusedMissingMemories(GameTestHelper h){
        var p=player(h);var yes=node("contains",Type.BOOLEAN,numbers(1),number(1));var no=node("contains",Type.BOOLEAN,list(),number(1));var missing=recall(999);
        h.assertTrue(AonEvaluator.value(p,node("if",Type.VALUE,yes,number(7),missing)).equals(7.0),"If skips unused missing Recall");
        h.assertTrue(AonEvaluator.value(p,node("if",Type.VALUE,no,missing,number(8))).equals(8.0),"False selects the third clockwise argument");
        h.assertTrue(AonEvaluator.value(p,node("and",Type.BOOLEAN,no,missing)).equals(false),"And short-circuits at false");
        h.assertTrue(AonEvaluator.value(p,node("or",Type.BOOLEAN,yes,missing)).equals(true),"Or short-circuits at true");
        h.assertTrue(AonEvaluator.value(p,node("not",Type.BOOLEAN,yes)).equals(false),"Not negates a Boolean");
        var pig=EntityType.PIG.create(h.getLevel());pig.setPos(p.position().add(1,0,0));h.getLevel().addFreshEntity(pig);AonMemory.remember(p,1,new AonEvaluator.Value(Type.ENTITY,pig));
        var selected=node("if",Type.VALUE,yes,recall(1),missing);h.assertTrue(AonEvaluator.execute(p,new AonProgram(node("force",Type.EFFECT,selected),1),1).contains("pushed 1")&&pig.getDeltaMovement().lengthSqr()>0,"A logical value output can select the actual spell target");pig.discard();h.succeed();
    }
    @GameTest(template="empty") public static void genericTargetListsRejectMixedValuesAndPushEachEntityOnce(GameTestHelper h){
        var p=player(h);var pig=EntityType.PIG.create(h.getLevel());pig.setPos(p.position().add(1,0,0));h.getLevel().addFreshEntity(pig);AonMemory.remember(p,1,new AonEvaluator.Value(Type.ENTITY,pig));
        var duplicate=list(recall(1),recall(1));h.assertTrue(AonEvaluator.execute(p,new AonProgram(node("force",Type.EFFECT,duplicate),1),1).contains("pushed 1"),"Repeated list entries cannot multiply Force on the same entity");
        pig.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);var invalid=list(recall(1),number(3));h.assertTrue(rejects(()->AonEvaluator.execute(p,new AonProgram(node("force",Type.EFFECT,invalid),1),1))&&pig.getDeltaMovement().lengthSqr()==0,"Mixed nonentity values reject before touching any target");pig.discard();h.succeed();
    }
    @GameTest(template="empty") public static void nestedListAndBooleanMemoryRoundTripThroughCopper(GameTestHelper h){
        var p=player(h);PowerAttributes.set(p,"f:copper",1);PowerData.state(p,"f:copper",-1);var mind=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("cosmerearcanists","copper_metalmind_ring")));p.setItemInHand(InteractionHand.OFF_HAND,mind);
        var source=list(number(4),list(node("caster",Type.ENTITY)),node("contains",Type.BOOLEAN,numbers(1),number(1)));
        try{var expected=AonEvaluator.evaluate(p,source);AonMemory.remember(p,4,expected);var encoded=CopperBooks.get(p.server).variables.get(CopperService.identity(mind)).get(4).copy();PowerData.state(p,"f:copper",1);
            h.assertTrue(AonMemory.recall(p,4).equals(expected)&&AonMemory.decode(p,encoded).equals(expected),"Copper persists nested typed lists and Boolean values");
            h.assertTrue(AonEvaluator.value(p,node("at",Type.VALUE,recall(4),number(2))).equals(true),"A recalled list entry retains its Boolean type");
            var deep=numeric(0);for(int i=0;i<10;i++)deep=new AonEvaluator.Value(Type.LIST,List.of(deep));var invalid=deep;h.assertTrue(rejects(()->AonMemory.encode(p,invalid)),"Deep nested memories reject before serialization growth");
        }finally{Metalminds.wipe(mind);CopperService.clear(p);}h.succeed();
    }
}
