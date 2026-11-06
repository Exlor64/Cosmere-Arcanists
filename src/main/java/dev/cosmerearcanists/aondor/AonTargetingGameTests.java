package dev.cosmerearcanists.aondor;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.cosmerearcanists.power.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;

@GameTestHolder(dev.cosmerearcanists.CosmereArcanists.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AonTargetingGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"AonTarget"));p.setPos(h.absolutePos(new BlockPos(2,2,2)).getCenter());p.getAttribute(PowerAttributes.AONDOR).setBaseValue(1);PowerData.assignSlot(p,AonDorService.POWER,0);return p;}
    private static AonDrawing.Glyph glyph(FakePlayer p,String name,boolean effect,double x,double y,double size,int parent,double number){var t=AonDorService.definitions(p).stream().filter(a->a.id().equals("cosmerearcanists:"+name)).findFirst().orElseThrow();var source=name.equals("number")?AonNumerals.strokes((int)number):t.strokes();var strokes=source.stream().map(line->line.stream().map(q->new AonDrawing.Point(x+(q.x()-.5)*size,y+(q.y()-.5)*size)).toList()).toList();return new AonDrawing.Glyph(effect,x,y,size,strokes,parent,number);}
    @GameTest(template="empty") public static void targetingCatalogAndNestedHeatSelectNearbyEntities(GameTestHelper h){
        var p=player(h);h.assertTrue(AonDorService.definitions(p).size()==18,"Three effects and fifteen argument templates loaded");var pig=EntityType.PIG.create(h.getLevel());pig.setPos(p.position().add(1,0,0));pig.setTicksFrozen(200);h.getLevel().addFreshEntity(pig);
        var drawing=new AonDrawing(List.of(glyph(p,"heat",true,.5,.7,.16,-1,1),glyph(p,"entities_in",false,.5,.4,.1,0,1),glyph(p,"sphere",false,.5,.2,.08,1,1),glyph(p,"position_of",false,.5,.1,.05,2,1),glyph(p,"caster",false,.5,.04,.04,3,1),glyph(p,"number",false,.7,.2,.08,2,3)));
        String result=AonDorService.execute(p,new AonDorNetwork.Check(UUID.randomUUID(),p.level().dimension().location().toString(),0,drawing));h.assertTrue(result.contains("Heat: warmed")&&pig.getTicksFrozen()==60,"Nested typed sphere resolves and warms an entity");h.succeed();
    }
    @GameTest(template="empty") public static void malformedNumberAndWrongTypesNeverExecute(GameTestHelper h){
        var p=player(h);var bad=new AonDrawing(List.of(glyph(p,"force",true,.5,.5,.2,-1,1),glyph(p,"number",false,.5,.2,.1,0,5)));
        String result=AonDorService.execute(p,new AonDorNetwork.Check(UUID.randomUUID(),p.level().dimension().location().toString(),0,bad));h.assertTrue(result.contains("expected")&&p.getDeltaMovement().lengthSqr()==0,"Wrong target type cannot move caster");
        var number=new AonDrawing.Glyph(false,.5,.2,.1,bad.glyphs().get(1).strokes(),0,5.75);var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());var request=new AonDorNetwork.Check(UUID.randomUUID(),p.level().dimension().location().toString(),0,new AonDrawing(List.of(bad.main(),number)));
        try{AonDorNetwork.Check.CODEC.encode(buffer,request);var decoded=AonDorNetwork.Check.CODEC.decode(buffer);h.assertTrue(decoded.drawing().glyphs().get(1).parent()==0&&decoded.drawing().glyphs().get(1).number()==5.75,"Explicit parent and numeric literal round-trip");}finally{buffer.release();}h.succeed();
    }
    @GameTest(template="empty") public static void numericValueComesFromCirclesNotPayload(GameTestHelper h){
        var p=player(h);var two=glyph(p,"number",false,.5,.2,.15,0,2);
        var forged=new AonDrawing.Glyph(false,two.x(),two.y(),two.size(),two.strokes(),0,99999);
        var match=AonRecognition.assess(forged,AonDorService.definitions(p),AonDorConfig.tolerance(),AonDorConfig.SHAPE_ERROR.get(),AonDorConfig.SHAPE_GAP.get());
        h.assertTrue(match.template()!=null&&match.number()==2,"Only the two surrounding circles determine the literal");
        var zero=glyph(p,"number",false,.5,.2,.15,0,0);var zeroMatch=AonRecognition.assess(zero,AonDorService.definitions(p),AonDorConfig.tolerance(),AonDorConfig.SHAPE_ERROR.get(),AonDorConfig.SHAPE_GAP.get());h.assertTrue(zeroMatch.number()==0,"Central circle is structural, not counted");h.succeed();
    }
    @GameTest(template="empty") public static void projectionCanCarryMoreThanOneGlyphStrokeBudget(GameTestHelper h){
        var line=List.of(new AonDrawing.Point(.1,.1),new AonDrawing.Point(.9,.9));var strokes=new ArrayList<List<AonDrawing.Point>>();for(int i=0;i<24;i++)strokes.add(line);var packet=new AonDorNetwork.Preview(h.getLevel().dimension().location().toString(),0,true,new AonDrawing.Ink(strokes,.5));var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{AonDorNetwork.Preview.CODEC.encode(buffer,packet);h.assertTrue(AonDorNetwork.Preview.CODEC.decode(buffer).glyph().strokes().size()==24,"World ink does not stop at the sixteen-stroke per-Aon limit");}finally{buffer.release();}h.succeed();
    }
}
