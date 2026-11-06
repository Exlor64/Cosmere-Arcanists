package dev.cosmerearcanists.aondor;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(dev.cosmerearcanists.CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AonFilterGameTests {
    private static AonProgram.Node node(String key,AonProgram.Type type,AonProgram.Node... args){return new AonProgram.Node("cosmerearcanists:"+key,3,type,List.of(args));}
    private static AonProgram.Node selection(){return node("entities_in",AonProgram.Type.ENTITIES,node("sphere",AonProgram.Type.REGION,node("position_of",AonProgram.Type.POSITION,node("caster",AonProgram.Type.ENTITY)),node("number",AonProgram.Type.NUMBER)));}
    @GameTest(template="empty") public static void filtersKeepTypesExcludeListsAndSelectNearest(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AonFilters"));var anchor=h.absolutePos(new BlockPos(2,2,2)).getCenter();p.setPos(anchor.x,h.getLevel().getMaxBuildHeight()-12,anchor.z);
        var a=EntityType.PIG.create(h.getLevel());a.setPos(p.position().add(1,0,0));h.getLevel().addFreshEntity(a);
        var b=EntityType.PIG.create(h.getLevel());b.setPos(p.position().add(-1,0,0));h.getLevel().addFreshEntity(b);
        var stand=EntityType.ARMOR_STAND.create(h.getLevel());stand.setPos(p.position().add(0,0,2));h.getLevel().addFreshEntity(stand);
        var input=selection();var mobs=node("mobs",AonProgram.Type.ENTITIES,input);
        Object selected=AonEvaluator.value(p,mobs);h.assertTrue(selected.equals(List.of(a,b)),"Mobs retains animals in deterministic order and excludes armor stands");
        h.assertTrue(((List<?>)AonEvaluator.value(p,node("players",AonProgram.Type.ENTITIES,input))).isEmpty(),"Players excludes animals and armor stands");
        h.assertTrue(AonEvaluator.value(p,node("nearest",AonProgram.Type.ENTITY,mobs))==a,"Equal-distance nearest uses lower entity ID");
        var nearest=node("nearest",AonProgram.Type.ENTITY,mobs);
        var excluded=AonEvaluator.value(p,node("exclude",AonProgram.Type.ENTITIES,input,nearest));h.assertTrue(excluded.equals(List.of(b,stand)),"Exclude removes a single entity and preserves order; actual="+excluded);
        h.assertTrue(AonEvaluator.value(p,node("exclude",AonProgram.Type.ENTITIES,input,mobs)).equals(List.of(stand)),"Exclude also accepts a whole list");
        var origin=node("position_of",AonProgram.Type.POSITION,nearest);
        h.assertTrue(AonEvaluator.value(p,node("nearest",AonProgram.Type.ENTITY,input,origin))==a,"Nearest accepts an explicit position");
        a.setTicksFrozen(200);b.setTicksFrozen(200);
        AonEvaluator.execute(p,new AonProgram(node("heat",AonProgram.Type.EFFECT,node("exclude",AonProgram.Type.ENTITIES,mobs,nearest)),1),1);
        h.assertTrue(a.getTicksFrozen()==200&&b.getTicksFrozen()==60,"Filtered Heat changes only the remaining target");
        var empty=node("exclude",AonProgram.Type.ENTITIES,input,input);boolean rejected=false;
        try{AonEvaluator.execute(p,new AonProgram(node("heat",AonProgram.Type.EFFECT,node("nearest",AonProgram.Type.ENTITY,empty)),1),1);}catch(IllegalArgumentException expected){rejected=expected.getMessage().contains("Nearest found no targets");}
        h.assertTrue(rejected&&a.getTicksFrozen()==200,"Empty Nearest rejects before any default target is affected");a.discard();b.discard();stand.discard();h.succeed();
    }
    @GameTest(template="empty") public static void playersAndSelfExclusionUseActualPlayers(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AonFilterSelf"));p.setPos(h.absolutePos(new BlockPos(2,22,2)).getCenter());h.getLevel().addNewPlayer(p);
        try{
            var input=selection();h.assertTrue(AonEvaluator.value(p,node("players",AonProgram.Type.ENTITIES,input)).equals(List.of(p)),"Players retains the actual caster in a region query");
            h.assertTrue(((List<?>)AonEvaluator.value(p,node("mobs",AonProgram.Type.ENTITIES,input))).isEmpty(),"Mobs excludes the player");
            h.assertTrue(((List<?>)AonEvaluator.value(p,node("exclude",AonProgram.Type.ENTITIES,input,node("caster",AonProgram.Type.ENTITY)))).isEmpty(),"Exclude Caster removes self");
        }finally{p.discard();}h.succeed();
    }

}
