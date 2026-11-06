package dev.cosmerearcanists.aondor;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.copper.*;
import dev.cosmerearcanists.feruchemy.*;
import dev.cosmerearcanists.power.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import static dev.cosmerearcanists.aondor.AonProgram.Type;

@GameTestHolder(dev.cosmerearcanists.CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AonMemoryGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AonMemory"));p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(2,22,2)).getCenter());return p;}
    private static AonProgram.Node node(String key,Type type,AonProgram.Node... args){return new AonProgram.Node("cosmerearcanists:"+key,0,type,List.of(args));}
    private static AonProgram.Node number(double n){return new AonProgram.Node("cosmerearcanists:number",n,Type.NUMBER,List.of());}
    private static AonEvaluator.Value numberValue(double n){return new AonEvaluator.Value(Type.NUMBER,n);}
    private static boolean rejects(Runnable operation){try{operation.run();return false;}catch(IllegalArgumentException expected){return true;}}
    @GameTest(template="empty",timeoutTicks=80) public static void temporaryMemoryExpiresAndKeyCanBeReused(GameTestHelper h){
        var p=player(h);var other=player(h);int seconds=AonDorConfig.MEMORY_SECONDS.get();try{AonDorConfig.MEMORY_SECONDS.set(1);AonMemory.remember(p,7,numberValue(12.5));}finally{AonDorConfig.MEMORY_SECONDS.set(seconds);}
        h.assertTrue(AonMemory.recall(p,7).data().equals(12.5)&&rejects(()->AonMemory.recall(other,7)),"Temporary memory retains numbers and is private to its caster");
        h.assertTrue(rejects(()->AonMemory.remember(p,.5,numberValue(1)))&&rejects(()->AonMemory.key(Double.NaN))&&rejects(()->AonMemory.key(-1)),"Memory keys require finite nonnegative integers");
        h.runAfterDelay(22,()->{h.assertTrue(rejects(()->AonMemory.recall(p,7)),"Expired memory cannot be retrieved");AonMemory.remember(p,7,numberValue(99));h.assertTrue(AonMemory.recall(p,7).data().equals(99.0),"Expired key is reusable");h.succeed();});
    }
    @GameTest(template="empty") public static void allCurrentValueTypesRetainTheirMeaning(GameTestHelper h){
        var p=player(h);var pig=EntityType.PIG.create(h.getLevel());pig.setPos(p.position().add(1,0,0));h.getLevel().addFreshEntity(pig);
        var values=List.of(numberValue(-3.5),new AonEvaluator.Value(Type.VECTOR,new Vec3(2,3,4)),new AonEvaluator.Value(Type.DIRECTION,new Vec3(0,0,1)),new AonEvaluator.Value(Type.POSITION,p.position()),new AonEvaluator.Value(Type.BLOCK,p.blockPosition()),new AonEvaluator.Value(Type.ENTITY,pig),new AonEvaluator.Value(Type.ENTITIES,List.of(pig)),new AonEvaluator.Value(Type.REGION,new AonEvaluator.Sphere(p.position(),3)));
        for(int i=0;i<values.size();i++){AonMemory.remember(p,i,values.get(i));h.assertTrue(AonMemory.recall(p,i).equals(values.get(i)),"Memory preserves "+values.get(i).type());}
        var foreign=AonMemory.encode(p,new AonEvaluator.Value(Type.POSITION,p.position()));foreign.putString("dimension","cosmerearcanists:other");h.assertTrue(rejects(()->AonMemory.decode(p,foreign)),"Coordinates preserve their dimension");
        pig.discard();h.assertTrue(rejects(()->AonMemory.recall(p,5)),"A dead/unloaded entity reference fails without spawning or loading it");h.succeed();
    }
    @GameTest(template="empty") public static void recalledValuesWorkInMathAndRejectWrongEffectTypes(GameTestHelper h){
        var p=player(h);AonMemory.remember(p,0,numberValue(3));var recall=node("recall",Type.VALUE);
        var multiply=node("multiply",Type.VALUE,recall,number(2));h.assertTrue(AonEvaluator.value(p,multiply).equals(6.0),"Recall supplies typed scalar arithmetic");
        h.assertTrue(rejects(()->AonEvaluator.execute(p,new AonProgram(node("heat",Type.EFFECT,recall),1),1)),"A recalled Number cannot be used as an Entity target");
        AonMemory.remember(p,0,new AonEvaluator.Value(Type.POSITION,p.position()));h.assertTrue(rejects(()->AonEvaluator.value(p,node("multiply",Type.VALUE,recall,number(2)))),"A position remains a position and cannot be multiplied like a vector");
        AonMemory.remember(p,0,new AonEvaluator.Value(Type.VECTOR,new Vec3(2,0,0)));h.assertTrue(AonEvaluator.value(p,multiply).equals(new Vec3(4,0,0)),"Replacing a key preserves the new vector type");h.succeed();
    }
    @GameTest(template="empty") public static void coppermindVariablesPersistAndRespectIdentityCapacityAndWipe(GameTestHelper h){
        var p=player(h);PowerAttributes.set(p,"f:copper",1);var item=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("cosmerearcanists","copper_metalmind_ring")));p.setItemInHand(InteractionHand.OFF_HAND,item);PowerData.state(p,"f:copper",-1);
        AonMemory.remember(p,42,new AonEvaluator.Value(Type.POSITION,p.position()));UUID book=CopperService.identity(item);var data=CopperBooks.get(p.server);
        h.assertTrue(data.variables.get(book).containsKey(42)&&Metalminds.bound(item),"Memory binds and writes the physical coppermind");
        var session=CopperService.open(p,false);h.assertTrue(CopperService.usedBytes(p,session)>0&&CopperService.usedBytes(p,session)<=CopperService.capacityBytes(p,item),"Variables count toward charged copper capacity");
        var reloaded=CopperBooks.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(reloaded.variables.get(book).get(42).equals(data.variables.get(book).get(42)),"Typed variables survive SavedData serialization");
        PowerData.state(p,"f:copper",1);h.assertTrue(AonMemory.recall(p,42).equals(new AonEvaluator.Value(Type.POSITION,p.position())),"Tapping retrieves indexed persistent coordinates");
        var other=player(h);PowerAttributes.set(other,"f:copper",1);PowerData.state(other,"f:copper",1);other.setItemInHand(InteractionHand.OFF_HAND,item.copy());h.assertTrue(rejects(()->AonMemory.recall(other,42)),"Foreign Identity cannot read copied keyed copper");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(rejects(()->AonMemory.recall(p,42)),"Losing contact revokes access");p.setItemInHand(InteractionHand.OFF_HAND,item);
        Metalminds.wipe(item);h.assertTrue(!data.variables.containsKey(book)&&!data.books.containsKey(book),"Physical wipe erases typed variables alongside notes");CopperService.clear(p);CopperService.clear(other);h.succeed();
    }
    @GameTest(template="empty") public static void copperEntryLimitIncludesNotesAndVariables(GameTestHelper h){
        var p=player(h);PowerAttributes.set(p,"f:copper",1);var item=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("cosmerearcanists","copper_metalmind_ring")));p.setItemInHand(InteractionHand.OFF_HAND,item);PowerData.state(p,"f:copper",-1);int limit=FeruchemyConfig.COPPER_PAGES.get();
        try{FeruchemyConfig.COPPER_PAGES.set(1);AonMemory.remember(p,1,numberValue(5));h.assertTrue(rejects(()->AonMemory.remember(p,2,numberValue(8))),"Variable entries obey the shared notebook entry cap");AonMemory.remember(p,1,numberValue(9));PowerData.state(p,"f:copper",1);h.assertTrue(AonMemory.recall(p,1).data().equals(9.0),"Replacing an existing key uses its existing entry");}finally{FeruchemyConfig.COPPER_PAGES.set(limit);Metalminds.wipe(item);CopperService.clear(p);}h.succeed();
    }
}
