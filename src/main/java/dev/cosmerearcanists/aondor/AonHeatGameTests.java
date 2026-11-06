package dev.cosmerearcanists.aondor;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import static dev.cosmerearcanists.aondor.AonProgram.Type;

@GameTestHolder(dev.cosmerearcanists.CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AonHeatGameTests {
    private static FakePlayer player(GameTestHelper h){for(int x=0;x<=4;x++)for(int y=20;y<=25;y++)for(int z=0;z<=12;z++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AonHeat"));p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(2,20,2)).getCenter());p.setYRot(0);p.setXRot(0);return p;}
    private static AonProgram.Node node(String key,Type type,AonProgram.Node... args){return new AonProgram.Node("cosmerearcanists:"+key,0,type,List.of(args));}
    private static AonProgram.Node number(double n){return new AonProgram.Node("cosmerearcanists:number",n,Type.NUMBER,List.of());}
    private static AonProgram.Node skip(){return node("skip",Type.SKIP);}
    private static String cast(FakePlayer p,AonProgram.Node target,double power){return AonEvaluator.execute(p,new AonProgram(node("heat",Type.EFFECT,target,number(power)),1),1);}
    private static boolean rejects(Runnable operation){try{operation.run();return false;}catch(IllegalArgumentException expected){return true;}}
    @GameTest(template="empty") public static void skippedHeatTargetUsesGazeAndPowerAddsFireDamage(GameTestHelper h){
        var p=player(h);var pig=EntityType.PIG.create(h.getLevel());pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);pig.setHealth(20);pig.setPos(p.getX(),p.getEyeY()-.4,p.getZ()+3);pig.setTicksFrozen(200);h.getLevel().addFreshEntity(pig);
        h.assertTrue(AonDorEffects.target(p)==pig,"Fixture gaze must hit pig");String result=cast(p,skip(),2);
        h.assertTrue(result.contains("warmed 1")&&pig.getTicksFrozen()==60&&pig.getHealth()==18&&pig.isOnFire(),"Skip defaults to gaze, power2 deals2fire damage and ignites while thawing; result="+result+" health="+pig.getHealth());pig.discard();h.succeed();
    }
    @GameTest(template="empty") public static void zeroPowerIsWarmthOnlyAndFireResistanceStillApplies(GameTestHelper h){
        var p=player(h);var pig=EntityType.PIG.create(h.getLevel());pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);pig.setHealth(20);pig.setPos(p.position().add(1,0,0));pig.setTicksFrozen(200);h.getLevel().addFreshEntity(pig);AonMemory.remember(p,1,new AonEvaluator.Value(Type.ENTITY,pig));var target=node("recall",Type.VALUE,number(1));
        cast(p,target,0);h.assertTrue(pig.getTicksFrozen()==60&&pig.getHealth()==20&&!pig.isOnFire(),"Power0 only thaws");pig.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,200));cast(p,target,8);h.assertTrue(pig.getHealth()==20&&!pig.isOnFire(),"Vanilla fire resistance protects against actual fire-tagged Heat");pig.discard();h.succeed();
    }
    @GameTest(template="empty") public static void heatIgnitesLoadedBlockAndHonorsBuildPermission(GameTestHelper h){
        var p=player(h);var block=h.absolutePos(new BlockPos(2,20,5));h.getLevel().setBlock(block,Blocks.STONE.defaultBlockState(),3);h.getLevel().setBlock(block.above(),Blocks.AIR.defaultBlockState(),3);
        AonMemory.remember(p,1,new AonEvaluator.Value(Type.BLOCK,block));var target=node("recall",Type.VALUE,number(1));h.assertTrue(cast(p,target,1).contains("ignited")&&h.getLevel().getBlockState(block.above()).is(Blocks.FIRE),"Power1 creates real vanilla fire above the targeted block");
        h.getLevel().setBlock(block.above(),Blocks.AIR.defaultBlockState(),3);p.getAbilities().mayBuild=false;cast(p,target,8);h.assertTrue(h.getLevel().getBlockState(block.above()).isAir(),"No fire is placed without build permission");h.succeed();
    }
    @GameTest(template="empty") public static void cookingAndSmeltingUseRecipesAndPreserveStacksAndEntityIdentity(GameTestHelper h){
        var p=player(h);var food=new ItemEntity(h.getLevel(),p.getX()+1,p.getY(),p.getZ(),new ItemStack(Items.BEEF,4));var ore=new ItemEntity(h.getLevel(),p.getX()+2,p.getY(),p.getZ(),new ItemStack(Items.RAW_IRON,3));h.getLevel().addFreshEntity(food);h.getLevel().addFreshEntity(ore);UUID identity=food.getUUID();
        AonHeat.entities(p,List.of(food,ore),1,4);h.assertTrue(food.getItem().is(Items.COOKED_BEEF)&&food.getItem().getCount()==4&&food.getUUID().equals(identity)&&!food.isOnFire()&&ore.getItem().is(Items.RAW_IRON),"Power4 cooks the entire recipe stack without destroying items or smelting ore");
        AonHeat.entities(p,List.of(ore),1,8);h.assertTrue(ore.getItem().is(Items.IRON_INGOT)&&ore.getItem().getCount()==3&&!ore.isOnFire(),"Power8 uses the furnace recipe for raw iron");food.discard();ore.discard();h.succeed();
    }
    @GameTest(template="empty") public static void invalidPowerAndOversizedRecipeRejectBeforeAnyEffects(GameTestHelper h){
        var p=player(h);var pig=EntityType.PIG.create(h.getLevel());pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);pig.setHealth(20);pig.setPos(p.position().add(1,0,0));pig.setTicksFrozen(200);h.getLevel().addFreshEntity(pig);AonMemory.remember(p,1,new AonEvaluator.Value(Type.ENTITY,pig));var target=node("recall",Type.VALUE,number(1));
        h.assertTrue(rejects(()->cast(p,target,-1))&&rejects(()->cast(p,target,Double.POSITIVE_INFINITY))&&pig.getTicksFrozen()==200&&pig.getHealth()==20&&!pig.isOnFire(),"Invalid powers reject before warmth/damage/ignition");
        var food=new ItemEntity(h.getLevel(),p.getX()+2,p.getY(),p.getZ(),new ItemStack(Items.BEEF,3));h.getLevel().addFreshEntity(food);int limit=AonDorConfig.HEAT_ITEMS.get();try{AonDorConfig.HEAT_ITEMS.set(2);h.assertTrue(rejects(()->AonHeat.entities(p,List.of(pig,food),1,8))&&pig.getTicksFrozen()==200&&pig.getHealth()==20&&!pig.isOnFire()&&food.getItem().is(Items.BEEF),"All recipes preflight before any list target changes");}finally{AonDorConfig.HEAT_ITEMS.set(limit);pig.discard();food.discard();}h.succeed();
    }
    @GameTest(template="empty") public static void skipDefaultsWorkForOtherOptionalInputs(GameTestHelper h){
        var p=player(h);AonEvaluator.execute(p,new AonProgram(node("remember",Type.EFFECT,skip(),skip()),1),1);h.assertTrue(AonEvaluator.value(p,node("recall",Type.VALUE,skip()))==p,"Skipped memory key/value use0andCaster");
        h.assertTrue(AonEvaluator.value(p,node("look_direction",Type.DIRECTION,skip())).equals(p.getLookAngle().normalize()),"Skipped Look Direction entity uses caster");
        String light=AonEvaluator.execute(p,new AonProgram(node("light",Type.EFFECT,skip()),1),1);h.assertTrue(light.contains("temporary light"),"Skipped Light position uses the standalone default");h.succeed();
    }
    @GameTest(template="empty") public static void heatAboveEightKeepsIncreasingDamageWithoutDefaultCaps(GameTestHelper h){
        var p=player(h);var pig=EntityType.PIG.create(h.getLevel());pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);pig.setHealth(200);pig.setPos(p.position().add(1,0,0));h.getLevel().addFreshEntity(pig);AonMemory.remember(p,1,new AonEvaluator.Value(Type.ENTITY,pig));
        h.assertTrue(AonDorConfig.HEAT_MAX_POWER.get()==0&&AonDorConfig.HEAT_DAMAGE_CAP.get()==0,"Heat balance caps are disabled by default");cast(p,node("recall",Type.VALUE,number(1)),80);h.assertTrue(pig.getHealth()==120,"Power80 deals80direct fire damage rather than stopping at8,40or64");pig.discard();h.succeed();
    }

}
