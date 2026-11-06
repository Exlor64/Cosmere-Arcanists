package dev.cosmerearcanists.aondor;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import dev.cosmerearcanists.power.*;

@GameTestHolder(dev.cosmerearcanists.CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AonDorEffectsGameTests {
    @GameTest(template="empty") public static void aonLightOnlyOccupiesAirAndExpires(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(1,2,1));h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        h.assertTrue(AonLightBlock.place(h.getLevel(),pos,12,4),"Light occupies air");
        h.assertTrue(h.getLevel().getBlockState(pos).getLightEmission()==12,"Actual block light is emitted");
        h.assertTrue(!AonLightBlock.place(h.getLevel(),pos,15,100),"Recasting cannot overwrite an existing light or its expiry");
        var stone=pos.east();h.getLevel().setBlock(stone,Blocks.STONE.defaultBlockState(),3);h.assertTrue(!AonLightBlock.place(h.getLevel(),stone,12,4),"No solid block is overwritten");
        h.runAfterDelay(6,()->{h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"Scheduled light expires");h.assertTrue(h.getLevel().getBlockState(stone).is(Blocks.STONE),"Adjacent solid block stays intact");h.succeed();});
    }
    @GameTest(template="empty") public static void aonForceAddsMotionAndHeatThawsByPotency(GameTestHelper h){
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AonCaster"));
        var pig=EntityType.PIG.create(h.getLevel());pig.setPos(h.absolutePos(new BlockPos(2,2,2)).getCenter());pig.setTicksFrozen(200);
        h.assertTrue(AonDorEffects.warm(pig,.75)&&pig.getTicksFrozen()==95,"Shape-only Heat thaws105 ticks at default strength");
        h.assertTrue(AonDorEffects.warm(pig,1)&&pig.getTicksFrozen()==0&&!AonDorEffects.warm(pig,1),"Heat never makes freezing negative");
        var previous=new net.minecraft.world.phys.Vec3(.1,.2,.3);pig.setDeltaMovement(previous);
        h.assertTrue(AonDorEffects.push(player,pig,.75),"Base Force accepts a valid target");
        h.assertTrue(pig.getDeltaMovement().subtract(previous).distanceTo(player.getLookAngle().scale(AonDorConfig.FORCE.get()*.75))<1e-8,"Force adds a potency-scaled impulse");
        h.succeed();
    }
    @GameTest(template="empty") public static void aonCastRejectsWrongContextAndModifiers(GameTestHelper h){
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AonGuard"));
        var line=List.of(List.of(new AonDrawing.Point(.1,.1),new AonDrawing.Point(.9,.9)));
        var glyph=new AonDrawing.Glyph(true,.5,.5,line);String dimension=h.getLevel().dimension().location().toString();
        var request=new AonDorNetwork.Check(UUID.randomUUID(),dimension,0,new AonDrawing(List.of(glyph)));
        h.assertTrue(AonDorService.execute(player,request).contains("no longer available"),"Unowned casting rejected");
        player.getAttribute(PowerAttributes.AONDOR).setBaseValue(1);PowerData.assignSlot(player,AonDorService.POWER,0);
        h.assertTrue(AonDorService.execute(player,new AonDorNetwork.Check(UUID.randomUUID(),"minecraft:wrong",0,request.drawing())).contains("no longer available"),"Wrong dimension rejected");
        var args=new AonDrawing(List.of(glyph,new AonDrawing.Glyph(false,.5,.1,.18,line,0,1)));
        h.assertTrue(AonDorService.execute(player,new AonDorNetwork.Check(UUID.randomUUID(),dimension,0,args)).contains("not recognized"),"Unknown modifier shapes cannot silently cast");
        h.assertTrue(AonDorService.execute(player,request).contains("not recognized"),"Unknown raw geometry cannot cast by claiming an ID");h.succeed();
    }
}
