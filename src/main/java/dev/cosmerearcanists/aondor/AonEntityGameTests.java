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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import static dev.cosmerearcanists.aondor.AonProgram.Type;

@GameTestHolder(dev.cosmerearcanists.CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AonEntityGameTests {
    private static FakePlayer player(GameTestHelper h){for(int x=0;x<=4;x++)for(int y=20;y<=25;y++)for(int z=0;z<=12;z++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AonEntities"));p.setPos(h.absolutePos(new BlockPos(2,20,2)).getCenter());p.setYRot(0);p.setXRot(0);return p;}
    private static AonProgram.Node node(String key,Type type,AonProgram.Node... args){return new AonProgram.Node("cosmerearcanists:"+key,3,type,List.of(args));}
    private static AonProgram.Node selection(){return node("entities_in",Type.ENTITIES,node("sphere",Type.REGION,node("position_of",Type.POSITION,node("caster",Type.ENTITY)),node("number",Type.NUMBER)));}
    private static List<Entity> mixed(GameTestHelper h,FakePlayer p){
        var result=new ArrayList<Entity>();result.add(new ItemEntity(h.getLevel(),0,0,0,new ItemStack(Items.IRON_INGOT)));result.add(EntityType.ARROW.create(h.getLevel()));result.add(EntityType.EXPERIENCE_ORB.create(h.getLevel()));result.add(EntityType.PIG.create(h.getLevel()));result.add(EntityType.ARMOR_STAND.create(h.getLevel()));
        for(int i=0;i<result.size();i++){var entity=result.get(i);entity.setPos(p.position().add(.5+i*.4,0,0));h.getLevel().addFreshEntity(entity);}return result;
    }
    @GameTest(template="empty") public static void gazeAcceptsNonPickableItemsOrbsProjectilesVehiclesAndLivingEntities(GameTestHelper h){
        var p=player(h);var types=List.of(EntityType.ARROW,EntityType.EXPERIENCE_ORB,EntityType.ARMOR_STAND,EntityType.PIG,EntityType.BOAT,EntityType.MINECART,EntityType.END_CRYSTAL);
        for(var type:types){var entity=type.create(h.getLevel());entity.setPos(p.getX(),p.getEyeY()-.1,p.getZ()+3);h.getLevel().addFreshEntity(entity);h.assertTrue(AonDorEffects.target(p)==entity,"Gaze resolves entity type "+type);entity.discard();}
        var item=new ItemEntity(h.getLevel(),p.getX(),p.getEyeY()-.1,p.getZ()+3,new ItemStack(Items.APPLE));h.getLevel().addFreshEntity(item);
        h.assertTrue(!item.isPickable()&&AonEvaluator.value(p,node("gaze_entity",Type.ENTITY))==item,"Gaze selects dropped items even though vanilla marks them nonpickable");
        var position=node("position_of",Type.POSITION,node("gaze_entity",Type.ENTITY));h.assertTrue(AonEvaluator.value(p,position).equals(item.position()),"Position Of accepts a nonliving gaze result");
        h.assertTrue(AonEvaluator.value(p,node("look_direction",Type.DIRECTION,node("gaze_entity",Type.ENTITY))).equals(item.getLookAngle().normalize()),"Look Direction accepts a generic entity");
        h.assertTrue(AonDorEffects.cast(p,"cosmerearcanists:force",1).contains("target pushed")&&item.getDeltaMovement().z>0,"Standalone Force can push a gaze-targeted item");
        h.assertTrue(AonDorEffects.cast(p,"cosmerearcanists:heat",1).contains("no basic warming interaction"),"Standalone Heat reports an unsupported nonliving interaction without targeting through it");item.discard();h.succeed();
    }
    @GameTest(template="empty") public static void genericGazeStillRespectsWallsNearestAndOriginInsideBounds(GameTestHelper h){
        var p=player(h);var near=new ItemEntity(h.getLevel(),p.getX(),p.getEyeY()-.1,p.getZ()+2,new ItemStack(Items.APPLE));var far=EntityType.ARROW.create(h.getLevel());far.setPos(p.getX(),p.getEyeY()-.1,p.getZ()+4);h.getLevel().addFreshEntity(near);h.getLevel().addFreshEntity(far);
        h.assertTrue(AonDorEffects.target(p)==near,"Closest intersection wins across entity types");near.discard();
        var wall=BlockPos.containing(p.getEyePosition().add(p.getLookAngle().scale(2)));h.getLevel().setBlock(wall,Blocks.STONE.defaultBlockState(),3);h.assertTrue(AonDorEffects.target(p)==null,"A solid block occludes nonliving ray targets");h.getLevel().setBlock(wall,Blocks.AIR.defaultBlockState(),3);
        far.setPos(p.getEyePosition());h.assertTrue(AonDorEffects.target(p)==far,"A ray beginning inside the entity bounds resolves that entity");far.discard();h.succeed();
    }
    @GameTest(template="empty") public static void zonesFiltersAndEffectsHandleMixedEntityLists(GameTestHelper h){
        var p=player(h);var mixed=mixed(h,p);var input=selection();h.assertTrue(AonEvaluator.value(p,input).equals(mixed),"Unfiltered zone includes dropped items, arrows, XP orbs, mobs and armor stands in distance order");
        var mobs=node("mobs",Type.ENTITIES,input);h.assertTrue(AonEvaluator.value(p,mobs).equals(List.of(mixed.get(3))),"Mobs narrows the generic list to mobs");h.assertTrue(((List<?>)AonEvaluator.value(p,node("players",Type.ENTITIES,input))).isEmpty(),"Players excludes every nonplayer in the mixed list");
        var nearest=node("nearest",Type.ENTITY,input);h.assertTrue(AonEvaluator.value(p,nearest)==mixed.get(0),"Nearest can return an item");h.assertTrue(AonEvaluator.value(p,node("exclude",Type.ENTITIES,input,nearest)).equals(mixed.subList(1,mixed.size())),"Exclude removes a nonliving entity");
        var pig=(LivingEntity)mixed.get(3);pig.setTicksFrozen(200);AonEvaluator.execute(p,new AonProgram(node("heat",Type.EFFECT,input),1),1);h.assertTrue(pig.getTicksFrozen()==60,"Heat safely ignores nonliving entries and warms living entries");
        String pushed=AonEvaluator.execute(p,new AonProgram(node("force",Type.EFFECT,input),1),1);h.assertTrue(pushed.contains("pushed 5")&&mixed.stream().allMatch(e->e.getDeltaMovement().z>0),"Force applies additive motion to every valid entity in the zone");mixed.forEach(Entity::discard);h.succeed();
    }
    @GameTest(template="empty") public static void temporaryAndCopperMemoryRetainGenericEntityReferences(GameTestHelper h){
        var p=player(h);var entities=mixed(h,p);var value=new AonEvaluator.Value(Type.ENTITIES,List.copyOf(entities));AonMemory.remember(p,1,value);h.assertTrue(AonMemory.recall(p,1).equals(value),"Temporary memory resolves a mixed entity reference list");
        for(int i=0;i<entities.size();i++){AonMemory.remember(p,10+i,new AonEvaluator.Value(Type.ENTITY,entities.get(i)));h.assertTrue(AonMemory.recall(p,10+i).data()==entities.get(i),"Individual nonliving/living UUID reference survives recall");}
        PowerAttributes.set(p,"f:copper",1);PowerData.state(p,"f:copper",-1);var mind=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("cosmerearcanists","copper_metalmind_ring")));p.setItemInHand(InteractionHand.OFF_HAND,mind);
        try{AonMemory.remember(p,2,value);var data=CopperBooks.get(p.server);var tag=data.variables.get(CopperService.identity(mind)).get(2).copy();PowerData.state(p,"f:copper",1);h.assertTrue(AonMemory.recall(p,2).equals(value)&&AonMemory.decode(p,tag).equals(value),"Copper stores and recalls mixed UUID references with the same schema");
            entities.get(0).discard();boolean rejected=false;try{AonMemory.recall(p,2);}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Despawned item references reject the whole list without recreating entities");
        }finally{Metalminds.wipe(mind);CopperService.clear(p);entities.forEach(Entity::discard);}h.succeed();
    }
}
