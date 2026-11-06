package dev.cosmerearcanists.aondor;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.power.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(dev.cosmerearcanists.CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AonMathGameTests {
    private static FakePlayer player(GameTestHelper h){for(int x=0;x<=4;x++)for(int y=20;y<=24;y++)for(int z=0;z<=12;z++)h.setBlock(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR);var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AonMath"));p.setPos(h.absolutePos(new BlockPos(2,20,2)).getCenter());p.setYRot(0);p.setXRot(0);p.getAttribute(PowerAttributes.AONDOR).setBaseValue(1);PowerData.assignSlot(p,AonDorService.POWER,0);return p;}
    private static AonDrawing.Glyph glyph(FakePlayer p,String key,double x,double y,int parent,int number){var t=AonDorService.definitions(p).stream().filter(v->v.id().equals("cosmerearcanists:"+key)).findFirst().orElseThrow();var original=key.equals("number")?AonNumerals.strokes(number):t.strokes();var lines=original.stream().map(line->line.stream().map(q->new AonDrawing.Point(x+(q.x()-.5)*.08,y+(q.y()-.5)*.08)).toList()).toList();return new AonDrawing.Glyph(t.effect(),x,y,.08,lines,parent,number);}
    private static String cast(FakePlayer p,List<AonDrawing.Glyph> glyphs){return AonTestCasting.execute(p,new AonDorNetwork.Check(UUID.randomUUID(),p.level().dimension().location().toString(),0,new AonDrawing(glyphs)));}
    @GameTest(template="empty") public static void nestedDivisionCreatesFractionalRadiusAndRejectsZero(GameTestHelper h){
        var p=player(h);var pig=EntityType.PIG.create(h.getLevel());pig.setPos(p.position().add(3.25,0,0));pig.setTicksFrozen(200);h.getLevel().addFreshEntity(pig);
        var ink=new ArrayList<>(List.of(glyph(p,"heat",.5,.7,-1,1),glyph(p,"entities_in",.5,.5,0,1),glyph(p,"sphere",.5,.3,1,1),glyph(p,"position_of",.5,.18,2,1),glyph(p,"caster",.5,.07,3,1),glyph(p,"divide",.73,.3,2,1),glyph(p,"number",.73,.15,5,7),glyph(p,"number",.88,.3,5,2)));
        h.assertTrue(cast(p,ink).contains("Heat: warmed")&&pig.getTicksFrozen()==60,"7/2 produces radius3.5 and reaches a target3.25blocks away");
        pig.setTicksFrozen(200);ink.set(7,glyph(p,"number",.88,.3,5,0));h.assertTrue(cast(p,ink).contains("divide by zero")&&pig.getTicksFrozen()==200,"Division by zero fails before modifying targets");h.succeed();
    }
    @GameTest(template="empty") public static void vectorMultiplicationPreservesForceMagnitude(GameTestHelper h){
        var p=player(h);var pig=EntityType.PIG.create(h.getLevel());pig.setPos(p.getX(),p.getEyeY()-.4,p.getZ()+3);h.getLevel().addFreshEntity(pig);
        var ink=List.of(glyph(p,"force",.4,.6,-1,1),glyph(p,"gaze_entity",.4,.3,0,1),glyph(p,"multiply",.7,.6,0,1),glyph(p,"look_direction",.7,.3,2,1),glyph(p,"number",.88,.6,2,2));
        h.assertTrue(AonDorEffects.target(p)==pig,"Fixture gaze must resolve pig above test infrastructure; eye="+p.getEyePosition()+" gaze="+p.getLookAngle()+" pig="+pig.position());String result=cast(p,ink);h.assertTrue(result.contains("pushed 1")&&Math.abs(pig.getDeltaMovement().z-AonDorConfig.FORCE.get()*2)<1e-7,"Multiplying gaze vector by2 doubles added Force; result="+result+" velocity="+pig.getDeltaMovement());h.succeed();
    }
    @GameTest(template="empty") public static void positionPlusVectorOffsetsLight(GameTestHelper h){
        var p=player(h);var target=BlockPos.containing(p.position().add(0,2,0));h.getLevel().setBlock(target,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        var ink=List.of(glyph(p,"light",.4,.7,-1,1),glyph(p,"add",.4,.5,0,1),glyph(p,"position_of",.4,.3,1,1),glyph(p,"caster",.4,.15,2,1),glyph(p,"vector",.7,.5,1,1),glyph(p,"number",.7,.3,4,0),glyph(p,"number",.88,.5,4,2),glyph(p,"number",.7,.7,4,0));
        var program=AonProgram.compile(new AonDrawing(ink),AonDorService.definitions(p),AonDorConfig.tolerance(),AonDorConfig.SHAPE_ERROR.get(),AonDorConfig.SHAPE_GAP.get(),AonDorConfig.FALLBACK_POTENCY.get());var calculated=(net.minecraft.world.phys.Vec3)AonEvaluator.value(p,program.root().arguments().get(0));h.assertTrue(calculated.distanceTo(p.position().add(0,2,0))<1e-7,"Calculated position differs: "+calculated+" calculations="+program.calculations());String result=cast(p,ink);h.assertTrue(result.contains("permanent light")&&h.getLevel().getBlockState(target).is(AonLightBlock.LIGHT.get())&&h.getLevel().getBlockState(target).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT),"Adding world vector(0,2,0) offsets caster position and places real light; result="+result+" target="+target+" state="+h.getLevel().getBlockState(target)+" above="+h.getLevel().getBlockState(target.above())+" below="+h.getLevel().getBlockState(target.below())+" eye="+p.getEyePosition()+" position="+p.position()+" mayBuild="+p.getAbilities().mayBuild+" mayInteract="+h.getLevel().mayInteract(p,target)+" loaded="+h.getLevel().hasChunkAt(target)+" inBounds="+h.getLevel().isInWorldBounds(target)+" border="+h.getLevel().getWorldBorder().isWithinBounds(target)+" calculations="+program.calculations());h.succeed();
    }
    @GameTest(template="empty") public static void lightPersistsInAnOtherwiseEmptySection(GameTestHelper h){
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(1,2,1));var pos=new BlockPos(base.getX(),level.getMaxBuildHeight()-8,base.getZ());var section=level.getChunkAt(pos).getSection(level.getSectionIndex(pos.getY()));
        h.assertTrue(section.hasOnlyAir(),"Fixture section must initially contain only air");
        h.assertTrue(AonLightBlock.place(level,pos,12,4),"Temporary light must be stored even in an empty section");
        h.assertTrue(!section.hasOnlyAir()&&level.getBlockState(pos).is(AonLightBlock.LIGHT.get())&&level.getBlockState(pos).getLightEmission()==12,"Real invisible light is readable and emits light above terrain");
        h.assertTrue(level.getBlockState(pos).getShape(level,pos).isEmpty(),"Invisible light must not intercept block-gaze selection");
        h.runAfterDelay(6,()->{h.assertTrue(level.getBlockState(pos).isAir()&&section.hasOnlyAir(),"Expiry restores an empty section");h.succeed();});
    }
    @GameTest(template="empty") public static void dynamicZeroNormalizationFailsBeforeForce(GameTestHelper h){
        var p=player(h);var pig=EntityType.PIG.create(h.getLevel());pig.setPos(p.getX(),p.getEyeY()-.4,p.getZ()+3);h.getLevel().addFreshEntity(pig);
        var ink=List.of(glyph(p,"force",.4,.6,-1,1),glyph(p,"gaze_entity",.4,.3,0,1),glyph(p,"normalize",.7,.6,0,1),glyph(p,"subtract",.7,.4,2,1),glyph(p,"position_of",.7,.24,3,1),glyph(p,"caster",.7,.1,4,1),glyph(p,"position_of",.88,.4,3,1),glyph(p,"caster",.88,.24,6,1));
        h.assertTrue(AonDorEffects.target(p)==pig,"Fixture must see the selected target");String result=cast(p,ink);h.assertTrue(result.contains("zero vector")&&pig.getDeltaMovement().lengthSqr()==0,"World-dependent zero vector rejects before any target moves; result="+result+" velocity="+pig.getDeltaMovement());h.succeed();
    }
}
