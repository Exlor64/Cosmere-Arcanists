package dev.cosmerearcanists.aondor;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import static dev.cosmerearcanists.aondor.AonProgram.Type;

@GameTestHolder(dev.cosmerearcanists.CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AonLightGameTests {
    private static FakePlayer player(GameTestHelper h){for(int x=0;x<=4;x++)for(int y=20;y<=25;y++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AonLight"));p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(2,20,2)).getCenter());return p;}
    private static AonProgram.Node node(String key,Type type,AonProgram.Node... args){return new AonProgram.Node("cosmerearcanists:"+key,1,type,List.of(args));}
    private static AonProgram.Node recall(){return node("recall",Type.VALUE,node("number",Type.NUMBER));}
    private static boolean rejects(Runnable operation){try{operation.run();return false;}catch(IllegalArgumentException expected){return true;}}
    @GameTest(template="empty") public static void permanentCoordinatesSurviveAnOldTemporaryExpiry(GameTestHelper h){
        var p=player(h);var point=p.position().add(0,2,0);var pos=BlockPos.containing(point);h.assertTrue(AonLightBlock.place(h.getLevel(),pos,8,2),"Fixture starts as temporary light");AonMemory.remember(p,1,new AonEvaluator.Value(Type.POSITION,point));
        String result=AonEvaluator.execute(p,new AonProgram(node("light",Type.EFFECT,recall()),1),1);var state=h.getLevel().getBlockState(pos);
        h.assertTrue(result.contains("permanent light")&&state.getValue(BlockStateProperties.PERSISTENT)&&state.getLightEmission()==12,"Explicit coordinates upgrade to permanent light with real emission");
        h.assertTrue(!state.getShape(h.getLevel(),pos).isEmpty()&&state.getCollisionShape(h.getLevel(),pos).isEmpty(),"Permanent mote can be selected/broken and does not obstruct movement");
        h.runAfterDelay(6,()->{h.assertTrue(h.getLevel().getBlockState(pos).is(AonLightBlock.LIGHT.get())&&h.getLevel().getBlockState(pos).getValue(BlockStateProperties.PERSISTENT),"A stale temporary scheduled tick cannot delete a permanent light");h.succeed();});
    }
    @GameTest(template="empty") public static void lightEntityArgumentsGlowAndPreserveLongerExistingEffects(GameTestHelper h){
        var p=player(h);var pig=EntityType.PIG.create(h.getLevel());pig.setPos(p.position().add(1,0,0));h.getLevel().addFreshEntity(pig);pig.addEffect(new MobEffectInstance(MobEffects.GLOWING,1200));AonMemory.remember(p,1,new AonEvaluator.Value(Type.ENTITY,pig));
        var targets=node("list",Type.LIST,node("caster",Type.ENTITY),recall());String result=AonEvaluator.execute(p,new AonProgram(node("light",Type.EFFECT,targets),1),1);
        h.assertTrue(result.contains("2 target")&&p.hasEffect(MobEffects.GLOWING)&&pig.getEffect(MobEffects.GLOWING).getDuration()>=1200,"Generic entity-list Light applies Glowing and never shortens an existing longer glow");pig.discard();h.succeed();
    }
    @GameTest(template="empty") public static void nonlivingGlowExpiresAndPreexistingGlowRemains(GameTestHelper h){
        var p=player(h);var item=new ItemEntity(h.getLevel(),p.getX()+1,p.getY(),p.getZ(),new ItemStack(Items.APPLE));var arrow=EntityType.ARROW.create(h.getLevel());arrow.setPos(p.position().add(2,0,0));var orb=EntityType.EXPERIENCE_ORB.create(h.getLevel());orb.setPos(p.position().add(3,0,0));h.getLevel().addFreshEntity(item);h.getLevel().addFreshEntity(arrow);h.getLevel().addFreshEntity(orb);orb.setGlowingTag(true);
        AonLightEffects.applyGlow(item,3);AonLightEffects.applyGlow(arrow,3);AonLightEffects.applyGlow(orb,3);h.assertTrue(item.isCurrentlyGlowing()&&arrow.isCurrentlyGlowing()&&orb.isCurrentlyGlowing(),"Nonliving entities support the same glowing outline");
        h.runAfterDelay(6,()->{h.assertTrue(!item.isCurrentlyGlowing()&&!arrow.isCurrentlyGlowing()&&orb.isCurrentlyGlowing(),"Owned glow expires without clearing a preexisting glow");item.discard();arrow.discard();orb.discard();h.succeed();});
    }
    @GameTest(template="empty") public static void savedNonlivingGlowExpiresOnReload(GameTestHelper h){
        var p=player(h);var item=new ItemEntity(h.getLevel(),p.getX()+1,p.getY(),p.getZ(),new ItemStack(Items.APPLE));h.getLevel().addFreshEntity(item);AonLightEffects.applyGlow(item,2);var saved=item.saveWithoutId(new CompoundTag());item.discard();
        h.runAfterDelay(5,()->{var loaded=EntityType.ITEM.create(h.getLevel());loaded.load(saved);h.getLevel().addFreshEntity(loaded);h.assertTrue(!loaded.isCurrentlyGlowing(),"Reloading after expiry clears owned glow rather than making it permanent");loaded.discard();h.succeed();});
    }
    @GameTest(template="empty") public static void lightPreflightsAllEntitiesAndDoesNotOverwriteProtectedOrSolidBlocks(GameTestHelper h){
        var p=player(h);var near=EntityType.PIG.create(h.getLevel());near.setPos(p.position().add(1,0,0));var far=EntityType.PIG.create(h.getLevel());far.setPos(p.position().add(20,0,0));h.getLevel().addFreshEntity(near);h.getLevel().addFreshEntity(far);
        h.assertTrue(rejects(()->AonLightEffects.glow(p,List.of(near,far),1))&&!near.hasEffect(MobEffects.GLOWING),"Out-of-range list entry rejects before any glow changes");var pos=BlockPos.containing(p.position().add(0,2,0));h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);AonLightEffects.permanent(p,pos.getCenter(),1);h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE),"Coordinates do not overwrite solid blocks");h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);p.getAbilities().mayBuild=false;AonLightEffects.permanent(p,pos.getCenter(),1);h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"Build permission gates permanent placement");near.discard();far.discard();h.succeed();
    }
}
